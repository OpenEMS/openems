package io.openems.edge.controller.ess.timeofusetariff;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.BALANCING;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.DISCHARGE_GRID;
import static io.openems.edge.controller.ess.timeofusetariff.Utils.ESS_DISCHARGE_TO_GRID_POWER;
import static io.openems.edge.controller.ess.timeofusetariff.Utils.ESS_MAX_SOC;
import static io.openems.edge.controller.ess.timeofusetariff.Utils.ESS_MIN_SOC;
import static io.openems.edge.controller.ess.timeofusetariff.Utils.calculateChargePowerInChargeGrid;
import static io.openems.edge.energy.api.EnergyUtils.findFirstPeakIndex;
import static io.openems.edge.energy.api.EnergyUtils.findFirstValleyIndex;
import static io.openems.edge.energy.api.EnergyUtils.findValleyIndexes;
import static io.openems.edge.energy.api.simulation.Coefficient.ESS;
import static io.openems.edge.energy.api.simulation.Coefficient.GRID_TO_CONS;
import static io.openems.edge.energy.api.simulation.Coefficient.GRID_TO_ESS;
import static io.openems.edge.energy.api.simulation.Coefficient.PROD_TO_ESS;
import static io.openems.edge.energy.api.simulation.Coefficient.PROD_TO_GRID;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.round;
import static java.util.Arrays.stream;
import static org.apache.commons.math3.optim.linear.Relationship.EQ;
import static org.apache.commons.math3.optim.nonlinear.scalar.GoalType.MAXIMIZE;
import static org.apache.commons.math3.optim.nonlinear.scalar.GoalType.MINIMIZE;

import java.util.Arrays;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import com.google.common.collect.ImmutableList;

import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.energy.api.handler.DifferentModes.InitialPopulation;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler;
import io.openems.edge.energy.api.handler.EshWithDifferentModes;
import io.openems.edge.energy.api.simulation.EnergyFlow;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext.Period;

public class EnergyScheduler {

	public static record OptimizationContext(int maxSocEnergyInChargeGrid, int essChargePowerInChargeGrid,
			int minSocEnergyInDischargeGrid, int doNotDischargeToGridAfterPeriod) {
	}

	/**
	 * Builds the {@link EnergyScheduleHandler}.
	 * 
	 * <p>
	 * This is public so that it can be used by the EnergyScheduler integration
	 * test.
	 * 
	 * @param parent         the parent {@link OpenemsComponent}
	 * @param configSupplier supplier for {@link Config}
	 * @return a {@link EnergyScheduleHandler}
	 */
	public static EshWithDifferentModes<StateMachine, OptimizationContext, Void> buildEnergyScheduleHandler(
			OpenemsComponent parent, Supplier<Config> configSupplier) {
		return EnergyScheduleHandler.WithDifferentModes.<StateMachine, OptimizationContext, Void>create(parent) //
				.setSerializer(Config.serializer(), configSupplier) //

				.setDefaultMode(BALANCING) //
				.setAvailableModes(() -> {
					var config = configSupplier.get();
					return config != null //
							? config.controlMode.modes //
							: new StateMachine[] { BALANCING };
				})

				.setInitialPopulationsProvider((goc, coc, availableModes) -> {
					// Prepare Initial Population with cheapest price per valley set to
					// DELAY_DISCHARGE or CHARGE_GRID
					var result = ImmutableList.<InitialPopulation<StateMachine>>builder();
					generateInitialPopulation(result, goc, StateMachine.DELAY_DISCHARGE);
					var hasChargeGrid = stream(availableModes).anyMatch(m -> m == StateMachine.CHARGE_GRID);
					if (hasChargeGrid) {
						generateInitialPopulation(result, goc, StateMachine.CHARGE_GRID);
					}
					var hasDischargeGrid = stream(availableModes).anyMatch(m -> m == DISCHARGE_GRID);
					if (hasDischargeGrid) {
						generateInitialPopulationForDischargeToGrid(result, goc);
					}
					return result.build();
				})

				.setOptimizationContext(goc -> {
					// Maximium-SoC in CHARGE_GRID is 90 %
					var maxSocEnergyInChargeGrid = round(goc.ess().totalEnergy() * (ESS_MAX_SOC / 100));
					var essChargeInChargeGrid = calculateChargePowerInChargeGrid(goc);
					var minSocEnergyInDischargeGrid = round(goc.ess().totalEnergy() * (ESS_MIN_SOC / 100));

					var doNotDischargeToGridAfterPeriod = calculateDoNotDischargeToGridAfterPeriod(goc);
					return new OptimizationContext(maxSocEnergyInChargeGrid, essChargeInChargeGrid,
							minSocEnergyInDischargeGrid, doNotDischargeToGridAfterPeriod);
				})

				.setSimulator((id, period, gsc, coc, csc, ef, mode, fitness) -> {
					if (mode == DISCHARGE_GRID
							&& (period.production() > 0 || period.index() > coc.doNotDischargeToGridAfterPeriod)) {
						mode = BALANCING;
					}

					var consumption = ef.finalizeConsumption();
					switch (mode) {
					case BALANCING -> applyBalancing(ef, consumption); // TODO Move to CtrlBalancing
					case DELAY_DISCHARGE -> applyDelayDischarge(ef, consumption);
					case CHARGE_GRID -> {
						var chargeEnergy = max(0, //
								min(//
										period.duration().convertPowerToEnergy(coc.essChargePowerInChargeGrid),
										coc.maxSocEnergyInChargeGrid - gsc.ess.getInitialEnergy()));
						applyChargeGrid(ef, consumption, chargeEnergy);
					}
					case DISCHARGE_GRID -> {
						var dischargeEnergy = max(0, //
								min(//
										period.duration().convertPowerToEnergy(ESS_DISCHARGE_TO_GRID_POWER),
										gsc.ess.getInitialEnergy() - coc.minSocEnergyInDischargeGrid()));
						applyDischargeGrid(ef, consumption, dischargeEnergy);
					}
					}
				})

				.setPostProcessor(Utils::postprocessSimulatorState)

				.build();
	}

	/**
	 * Simulate {@link EnergyFlow} in {@link StateMachine#BALANCING}.
	 * 
	 * @param model       the {@link EnergyFlow.Model}
	 * @param consumption the final consumption
	 */
	public static void applyBalancing(EnergyFlow.Model model, double consumption) {
		var target = consumption - model.production;
		model.setFittingCoefficientValue(ESS, EQ, target);
	}

	/**
	 * Simulate {@link EnergyFlow} in DELAY_DISCHARGE.
	 * 
	 * @param model       the {@link EnergyFlow.Model}
	 * @param consumption the final consumption
	 */
	public static void applyDelayDischarge(EnergyFlow.Model model, double consumption) {
		var target = min(0 /* Charge -> apply Balancing */, consumption - model.production);
		model.setFittingCoefficientValue(ESS, EQ, target);
	}

	/**
	 * Simulate {@link EnergyFlow} in {@link StateMachine#CHARGE_GRID}.
	 * 
	 * @param model        the {@link EnergyFlow.Model}
	 * @param consumption  the final consumption
	 * @param chargeEnergy the target charge-from-grid energy
	 */
	public static void applyChargeGrid(EnergyFlow.Model model, double consumption, double chargeEnergy) {
		var essMaxCharge = model.getExtremeCoefficientValue(ESS, MINIMIZE); // negative
		model.setFittingCoefficientValue(PROD_TO_ESS, EQ, -essMaxCharge);
		model.setExtremeCoefficientValue(GRID_TO_CONS, MAXIMIZE);
		chargeEnergy = min(-essMaxCharge, chargeEnergy);
		model.setFittingCoefficientValue(GRID_TO_ESS, EQ, chargeEnergy);
	}

	/**
	 * Simulate {@link EnergyFlow} in a future DISCHARGE_GRID state.
	 * 
	 * @param model           the {@link EnergyFlow.Model}
	 * @param consumption     the final consumption
	 * @param dischargeEnergy the target discharge-to-grid energy
	 */
	public static void applyDischargeGrid(EnergyFlow.Model model, double consumption, int dischargeEnergy) {
		var essMaxDischarge = model.getExtremeCoefficientValue(ESS, MAXIMIZE); // positive
		model.setFittingCoefficientValue(GRID_TO_ESS, EQ, -min(essMaxDischarge, dischargeEnergy));
		model.setExtremeCoefficientValue(PROD_TO_GRID, MAXIMIZE);
	}

	private static void generateInitialPopulation(ImmutableList.Builder<InitialPopulation<StateMachine>> result,
			GlobalOptimizationContext goc, StateMachine mode) {
		final var prices = goc.periods().stream() //
				.mapToDouble(Period::price) //
				.toArray();
		Arrays.stream(findValleyIndexes(prices)) //
				.mapToObj(i -> goc.periods().stream() //
						.map(p -> p.index() == i //
								? mode // set mode
								: BALANCING) // default
						.toArray(StateMachine[]::new)) //
				.map(InitialPopulation::new) //
				.forEach(result::add);
	}

	private static void generateInitialPopulationForDischargeToGrid(
			ImmutableList.Builder<InitialPopulation<StateMachine>> result, GlobalOptimizationContext goc) {
		final var prices = goc.periods().stream() //
				.mapToDouble(Period::price) //
				.toArray();
		IntStream.of(findFirstPeakIndex(1, prices)) //
				.mapToObj(i -> goc.periods().stream() //
						.map(p -> p.index() == i //
								? DISCHARGE_GRID
								: BALANCING) // default
						.toArray(StateMachine[]::new)) //
				.map(InitialPopulation::new) //
				.forEach(result::add);
	}

	private static int calculateDoNotDischargeToGridAfterPeriod(GlobalOptimizationContext goc) {
		final var prices = goc.periods().stream() //
				.mapToDouble(Period::price) //
				.toArray();
		return findFirstPeakIndex(findFirstValleyIndex(1, prices), prices);
	}

	// TODO maxChargePowerFromGrid is not used!
	public static record Config(ControlMode controlMode) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link Config}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Config> serializer() {
			return jsonObjectSerializer(Config.class, json -> {
				return new Config(//
						json.getEnum("controlMode", ControlMode.class) //
				);
			}, obj -> {
				return buildJsonObject() //
						.addProperty("controlMode", obj.controlMode) //
						.build();
			});
		}
	}
}