package io.openems.edge.controller.ess.timeofusetariff;

import static io.openems.edge.controller.ess.timeofusetariff.Utils.ESS_MAX_SOC;
import static io.openems.edge.controller.ess.timeofusetariff.Utils.calculateChargeEnergyInChargeGrid;
import static io.openems.edge.energy.api.EnergyUtils.findValleyIndexes;
import static io.openems.edge.energy.api.simulation.Coefficient.CONS;
import static io.openems.edge.energy.api.simulation.Coefficient.ESS;
import static io.openems.edge.energy.api.simulation.Coefficient.GRID_TO_CONS;
import static io.openems.edge.energy.api.simulation.Coefficient.GRID_TO_ESS;
import static io.openems.edge.energy.api.simulation.Coefficient.PROD_TO_ESS;
import static io.openems.edge.energy.api.simulation.Coefficient.PROD_TO_GRID;
import static java.lang.Math.min;
import static java.lang.Math.round;
import static java.util.Arrays.stream;
import static org.apache.commons.math3.optim.linear.Relationship.EQ;
import static org.apache.commons.math3.optim.nonlinear.scalar.GoalType.MAXIMIZE;
import static org.apache.commons.math3.optim.nonlinear.scalar.GoalType.MINIMIZE;

import java.util.Arrays;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableList;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.energy.api.handler.DifferentModes.InitialPopulation;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler;
import io.openems.edge.energy.api.handler.EshWithDifferentModes;
import io.openems.edge.energy.api.simulation.EnergyFlow;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext.Period;

public class EnergyScheduler {

	// TODO maxChargePowerFromGrid is not used!
	public static record Config(ControlMode controlMode) {
	}

	public static record OptimizationContext(int maxSocEnergyInChargeGrid, int essChargeInChargeGrid) {
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
				.setDefaultMode(StateMachine.BALANCING) //
				.setAvailableModes(() -> {
					var config = configSupplier.get();
					return config != null //
							? config.controlMode.modes //
							: new StateMachine[] { StateMachine.BALANCING };
				})

				.setInitialPopulationsFunction((goc, availableModes) -> {
					// Prepare Initial Population with cheapest price per valley set to
					// DELAY_DISCHARGE or CHARGE_GRID
					var result = ImmutableList.<InitialPopulation<StateMachine>>builder();
					generateInitialPopulation(result, goc, StateMachine.DELAY_DISCHARGE);
					var hasChargeGrid = stream(availableModes).anyMatch(m -> m == StateMachine.CHARGE_GRID);
					if (hasChargeGrid) {
						generateInitialPopulation(result, goc, StateMachine.CHARGE_GRID);
					}
					return result.build();
				})

				.setOptimizationContext(goc -> {
					// Maximium-SoC in CHARGE_GRID is 90 %
					var maxSocEnergyInChargeGrid = round(goc.ess().totalEnergy() * (ESS_MAX_SOC / 100));
					var essChargeInChargeGrid = calculateChargeEnergyInChargeGrid(goc);
					return new OptimizationContext(maxSocEnergyInChargeGrid, essChargeInChargeGrid);
				})

				.setSimulator((period, gsc, coc, csc, ef, mode) -> {
					switch (mode) {
					case BALANCING -> applyBalancing(ef); // TODO Move to CtrlBalancing
					case DELAY_DISCHARGE -> applyDelayDischarge(ef);
					case CHARGE_GRID -> {
						ef.setEssMaxCharge(coc.maxSocEnergyInChargeGrid - gsc.ess.getInitialEnergy());
						applyChargeGrid(ef, coc.essChargeInChargeGrid);
					}
					}
					return 0.;
				})

				.setPostProcessor(Utils::postprocessSimulatorState)

				.build();
	}

	/**
	 * Simulate {@link EnergyFlow} in {@link StateMachine#BALANCING}.
	 * 
	 * @param model the {@link EnergyFlow.Model}
	 */
	public static void applyBalancing(EnergyFlow.Model model) {
		var consumption = model.setExtremeCoefficientValue(CONS, MINIMIZE);
		var target = consumption - model.production;
		model.setFittingCoefficientValue(ESS, EQ, target);
	}

	/**
	 * Simulate {@link EnergyFlow} in DELAY_DISCHARGE.
	 * 
	 * @param model the {@link EnergyFlow.Model}
	 */
	public static void applyDelayDischarge(EnergyFlow.Model model) {
		var consumption = model.setExtremeCoefficientValue(CONS, MINIMIZE);
		var target = min(0 /* Charge -> apply Balancing */, consumption - model.production);
		model.setFittingCoefficientValue(ESS, EQ, target);
	}

	/**
	 * Simulate {@link EnergyFlow} in {@link StateMachine#CHARGE_GRID}.
	 * 
	 * @param model        the {@link EnergyFlow.Model}
	 * @param chargeEnergy the target charge-from-grid energy
	 */
	public static void applyChargeGrid(EnergyFlow.Model model, int chargeEnergy) {
		model.setExtremeCoefficientValue(CONS, MINIMIZE);
		model.setExtremeCoefficientValue(PROD_TO_ESS, MAXIMIZE);
		model.setExtremeCoefficientValue(GRID_TO_CONS, MAXIMIZE);
		model.setFittingCoefficientValue(GRID_TO_ESS, EQ, chargeEnergy);
	}

	/**
	 * Simulate {@link EnergyFlow} in a future DISCHARGE_GRID state.
	 * 
	 * @param model           the {@link EnergyFlow.Model}
	 * @param dischargeEnergy the target discharge-to-grid energy
	 */
	public static void applyDischargeGrid(EnergyFlow.Model model, int dischargeEnergy) {
		model.setExtremeCoefficientValue(CONS, MINIMIZE);
		model.setExtremeCoefficientValue(PROD_TO_GRID, MAXIMIZE);
		model.setFittingCoefficientValue(GRID_TO_ESS, EQ, -dischargeEnergy);
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
								: StateMachine.BALANCING) // default
						.toArray(StateMachine[]::new)) //
				.map(InitialPopulation::new) //
				.forEach(result::add);
	}
}