package io.openems.edge.controller.ess.timeofusetariff;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.BALANCING;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.CHARGE_GRID;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.DELAY_DISCHARGE;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.DISCHARGE_GRID;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.PEAK_SHAVING;
import static io.openems.edge.controller.ess.timeofusetariff.Utils.ESS_DISCHARGE_TO_GRID_POWER;
import static io.openems.edge.controller.ess.timeofusetariff.Utils.calculateChargePowerInChargeGrid;
import static io.openems.edge.controller.ess.timeofusetariff.Utils.calculateMaxSocForEnvironment;
import static io.openems.edge.energy.api.EnergyUtils.energyToSoc;
import static io.openems.edge.energy.api.EnergyUtils.findFirstPeakIndex;
import static io.openems.edge.energy.api.EnergyUtils.findFirstValleyIndex;
import static io.openems.edge.energy.api.EnergyUtils.findValleyIndexes;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.round;

import java.util.Arrays;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import com.google.common.collect.ImmutableSortedSet;

import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.energy.api.handler.DifferentModes.InitialPopulation;
import io.openems.edge.energy.api.handler.DifferentModes.Modes;
import io.openems.edge.energy.api.handler.DifferentModes.Modes.Mode;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler;
import io.openems.edge.energy.api.handler.EshWithDifferentModes;
import io.openems.edge.energy.api.simulation.EnergyFlow;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext.Period;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext.Period.Price;
import io.openems.edge.energy.api.simulation.GlobalScheduleContext;

public class EnergyScheduler {

	public static record OptimizationContext(//
			int maxSocInChargeGrid, int maxEnergyInChargeGrid, int essChargePowerInChargeGrid) {

		protected static OptimizationContext from(GlobalOptimizationContext goc) {
			// TODO: calculateMaxSocForEnvironment() is prepared for DISCHARGE_GRID
			final var maxSocInChargeGrid = calculateMaxSocForEnvironment(goc.environment());
			final var maxEnergyInChargeGrid = round(goc.ess().totalEnergy() * (maxSocInChargeGrid / 100F));
			final var essChargePowerInChargeGrid = calculateChargePowerInChargeGrid(goc, maxEnergyInChargeGrid);

			// TODO calculateDoNotDischargeToGridAfterPeriod(goc, prices) is prepared for
			// DISCHARGE_GRID
			return new OptimizationContext(maxSocInChargeGrid, maxEnergyInChargeGrid, essChargePowerInChargeGrid);
		}
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

				.setModes(() -> {
					var config = configSupplier.get();
					return Modes.of(Arrays.stream(StateMachine.values()) //
							.map(m -> new Mode<StateMachine>(//
									m, //
									config != null && config.controlMode.modes.contains(m), //
									preferenceRankFor(m)))//
							.collect(toImmutableList()));
				})

				.setInitialPopulationsProvider((goc, coc, modes) -> {
					// Prepare Initial Population with cheapest price per valley set to
					// DELAY_DISCHARGE or CHARGE_GRID
					final var result = ImmutableSortedSet.<InitialPopulation<StateMachine>>naturalOrder();
					final var prices = goc.streamPeriodsWithPrice() //
							.map(Period.WithPrice::price) //
							.mapToDouble(Price::actual) //
							.toArray();

					generateInitialPopulation(result, goc, prices, DELAY_DISCHARGE);
					var hasChargeGrid = modes.streamForOptimizer().anyMatch(m -> m == CHARGE_GRID);
					if (hasChargeGrid) {
						generateInitialPopulation(result, goc, prices, CHARGE_GRID);
					}
					var hasDischargeGrid = modes.streamForOptimizer().anyMatch(m -> m == DISCHARGE_GRID);
					if (hasDischargeGrid) {
						generateInitialPopulationForDischargeToGrid(result, goc, prices);
					}
					return result.build();
				})

				.setOptimizationContext(goc -> {
					return OptimizationContext.from(goc);
				})

				.setSimulator((id, period, gsc, coc, csc, ef, mode, fitness, isFinalRun) -> {
					if (mode == null) {
						mode = BALANCING;
					}

					if (period.gridBuySoftLimit() != null) {
						// Try setting GridMaxBuy to GridBuySoftLimit
						ef.setGridMaxBuy(period.gridBuySoftLimit());
					}

					if (isFinalRun) {
						// This is the final run -> post-process mode
						mode = postProcessMode(period, gsc, coc, ef, mode);
					}

					// Disallow mode CHARGE_GRID when ess is full
					final int soc = energyToSoc(gsc.ess.getInitialEnergy(), gsc.goc.ess().totalEnergy());
					if (mode == CHARGE_GRID && soc >= coc.maxSocInChargeGrid()) {
						fitness.addHardConstraintViolation();
					}

					simulateMode(period, gsc, coc, ef, mode);
					return mode;
				})

				.build();
	}

	private static Integer preferenceRankFor(StateMachine m) {
		return switch (m) {
		case CHARGE_GRID, DISCHARGE_GRID -> 1;
		case BALANCING, PEAK_SHAVING -> 2;
		case DELAY_DISCHARGE -> 3;
		};
	}

	private static void simulateMode(Period period, GlobalScheduleContext gsc, OptimizationContext coc,
			EnergyFlow.Model ef, StateMachine mode) {
		switch (mode) {
		case BALANCING -> applyBalancing(ef);
		case DELAY_DISCHARGE -> applyDelayDischarge(ef);
		case CHARGE_GRID, PEAK_SHAVING -> {
			var chargeEnergy = min(//
					period.duration().convertPowerToEnergy(coc.essChargePowerInChargeGrid),
					coc.maxEnergyInChargeGrid - gsc.ess.getInitialEnergy());
			applyChargeGrid(ef, chargeEnergy);
		}
		case DISCHARGE_GRID -> {
			var dischargeEnergy = period.duration().convertPowerToEnergy(ESS_DISCHARGE_TO_GRID_POWER);
			applyDischargeGrid(ef, dischargeEnergy);
		}
		}
	}

	/**
	 * Simulate {@link EnergyFlow} in {@link StateMachine#BALANCING}.
	 * 
	 * @param model the {@link EnergyFlow.Model}
	 */
	public static void applyBalancing(EnergyFlow.Model model) {
		int target = -model.getSurplus();
		model.setEss(target);
	}

	/**
	 * Simulate {@link EnergyFlow} in DELAY_DISCHARGE.
	 * 
	 * @param model the {@link EnergyFlow.Model}
	 */
	public static void applyDelayDischarge(EnergyFlow.Model model) {
		int target = min(0, -model.getSurplus());
		model.setEss(target);
	}

	/**
	 * Simulate {@link EnergyFlow} in {@link StateMachine#CHARGE_GRID}.
	 * 
	 * @param model        the {@link EnergyFlow.Model}
	 * @param chargeEnergy the target charge-from-grid energy
	 */
	public static void applyChargeGrid(EnergyFlow.Model model, int chargeEnergy) {
		int target = min(0, -model.getSurplus()) - chargeEnergy;
		model.setEss(target);
	}

	/**
	 * Simulate {@link EnergyFlow} in a future DISCHARGE_GRID state.
	 * 
	 * @param model           the {@link EnergyFlow.Model}
	 * @param dischargeEnergy the target discharge-to-grid energy
	 */
	public static void applyDischargeGrid(EnergyFlow.Model model, int dischargeEnergy) {
		int target = max(0, -model.getSurplus()) + dischargeEnergy;
		model.setEss(target);
	}

	private static void generateInitialPopulation(ImmutableSortedSet.Builder<InitialPopulation<StateMachine>> result,
			GlobalOptimizationContext goc, double[] prices, StateMachine mode) {
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
			ImmutableSortedSet.Builder<InitialPopulation<StateMachine>> result, GlobalOptimizationContext goc,
			double[] prices) {
		IntStream.of(findFirstPeakIndex(1, prices)) //
				.mapToObj(i -> goc.periods().stream() //
						.map(p -> p.index() == i //
								? DISCHARGE_GRID
								: BALANCING) // default
						.toArray(StateMachine[]::new)) //
				.map(InitialPopulation::new) //
				.forEach(result::add);
	}

	protected static int calculateDoNotDischargeToGridAfterPeriod(GlobalOptimizationContext goc, double[] prices) {
		return findFirstPeakIndex(findFirstValleyIndex(1, prices), prices);
	}

	private static StateMachine postProcessMode(Period period, GlobalScheduleContext gsc, OptimizationContext coc,
			EnergyFlow.Model ef, StateMachine mode) {
		final var initialMode = mode;
		var balancing = simulateModeWithCopy(period, gsc, coc, ef, BALANCING);
		var delayDischarge = simulateModeWithCopy(period, gsc, coc, ef, DELAY_DISCHARGE);

		if (mode == CHARGE_GRID) {
			var chargeGrid = simulateModeWithCopy(period, gsc, coc, ef, CHARGE_GRID);
			if (chargeGrid.getEss() == balancing.getEss()) {
				mode = BALANCING;
			} else if (chargeGrid.getEss() > balancing.getEss()) {
				mode = PEAK_SHAVING;
			} else if (chargeGrid.getEss() == delayDischarge.getEss()) {
				mode = DELAY_DISCHARGE;
			}
		}

		if (mode == DELAY_DISCHARGE) {
			if (delayDischarge.getEss() == balancing.getEss()) {
				mode = BALANCING;
			} else if (delayDischarge.getEss() > 0) {
				mode = PEAK_SHAVING;
			}
		}

		if (initialMode != mode) {
			System.out.println("OPTIMIZER PostProcess " + period.time() + " from " + initialMode + " to " + mode);
		}

		return mode;
	}

	private static EnergyFlow simulateModeWithCopy(Period period, GlobalScheduleContext gsc, OptimizationContext coc,
			EnergyFlow.Model ef, StateMachine mode) {
		var efCopy = EnergyFlow.Model.copyOf(ef);
		simulateMode(period, gsc, coc, efCopy, mode);
		return efCopy.solve();
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