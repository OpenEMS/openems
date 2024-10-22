package io.openems.edge.energy.v1.optimizer;

import static io.openems.edge.common.type.TypeUtils.fitWithin;
import static java.lang.Math.max;
import static java.lang.Math.min;

import io.openems.edge.controller.ess.timeofusetariff.StateMachine;
import io.openems.edge.energy.v1.optimizer.ParamsV1.OptimizePeriod;

/**
 * Simulates a detailed Energy-Flow.
 */
@Deprecated
public record EnergyFlowV1(//
		int production, /* positive */
		int consumption, /* positive */
		int ess, /* charge negative, discharge positive */
		int grid, /* sell-to-grid negative, buy-from-grid positive */
		int productionToConsumption, /* positive */
		int productionToGrid, /* positive */
		int productionToEss, /* positive */
		int gridToConsumption, /* positive */
		int essToConsumption, /* positive */
		int gridToEss /* discharge-to-grid negative, charge-from-grid positive */
) {

	/**
	 * Simulate {@link EnergyFlowV1} in {@link StateMachine#BALANCING}.
	 * 
	 * @param p          the {@link ParamsV1}
	 * @param op         the {@link OptimizePeriod}
	 * @param essInitial ESS Initially Available Energy (SoC in [Wh])
	 * @return the {@link EnergyFlowV1}
	 */
	public static EnergyFlowV1 withBalancing(ParamsV1 p, OptimizePeriod op, int essInitial) {
		return create(p, op, essInitial, //
				p.essTotalEnergy(), // Allow Balancing till full battery
				op.consumption() - op.production());
	}

	/**
	 * Simulate {@link EnergyFlowV1} in {@link StateMachine#DELAY_DISCHARGE}.
	 * 
	 * @param p          the {@link ParamsV1}
	 * @param op         the {@link OptimizePeriod}
	 * @param essInitial ESS Initially Available Energy (SoC in [Wh])
	 * @return the {@link EnergyFlowV1}
	 */
	public static EnergyFlowV1 withDelayDischarge(ParamsV1 p, OptimizePeriod op, int essInitial) {
		return create(p, op, essInitial, //
				p.essTotalEnergy(), // Allow Delay-Discharge with full battery
				min(0, op.consumption() - op.production())); // Allow charge; no discharge
	}

	/**
	 * Simulate {@link EnergyFlowV1} in {@link StateMachine#CHARGE_GRID}.
	 * 
	 * @param p          the {@link ParamsV1}
	 * @param op         the {@link OptimizePeriod}
	 * @param essInitial ESS Initially Available Energy (SoC in [Wh])
	 * @return the {@link EnergyFlowV1}
	 */
	public static EnergyFlowV1 withChargeGrid(ParamsV1 p, OptimizePeriod op, int essInitial) {
		return create(p, op, essInitial, //
				p.essMaxSocEnergy(), // Allow Charge-Grid only till Max-SoC
				// Same as Delay-Discharge + Charge-From-Grid
				min(0, op.consumption() - op.production()) - op.essChargeInChargeGrid());
	}

	protected static EnergyFlowV1 create(ParamsV1 p, OptimizePeriod op, int essInitial, int essMaxSocEnergy,
			int essTarget) {
		var essMaxDischarge = max(0, essInitial - p.essMinSocEnergy());
		var essMaxCharge = max(0, essMaxSocEnergy - essInitial);

		var ess = essTarget;
		// Apply Max Buy-From-Grid Energy
		ess = max(op.consumption() - op.production() - op.maxBuyFromGrid(), ess);
		// Apply Minimum-SoC / Maximum-SoC
		ess = fitWithin(-essMaxCharge, essMaxDischarge, ess);
		// Apply ESS Max Charge/Discharge Energy
		ess = fitWithin(-op.essMaxChargeEnergy(), op.essMaxDischargeEnergy(), ess);

		var grid = op.consumption() - op.production() - ess;
		var productionToConsumption = min(op.production(), op.consumption());
		var productionToEss = max(0, min(-ess, op.production() - productionToConsumption));
		var productionToGrid = max(0, op.production() - productionToConsumption - productionToEss);
		var essToConsumption = max(0, min(op.consumption() - productionToConsumption, ess - productionToGrid));
		var gridToConsumption = max(0, op.consumption() - essToConsumption - productionToConsumption);
		var gridToEss = grid - gridToConsumption + productionToGrid;
		return new EnergyFlowV1(//
				op.production(), /* production */
				op.consumption(), /* consumption */
				ess, /* ess */
				grid, /* grid */
				productionToConsumption, /* productionToConsumption */
				productionToGrid, /* productionToGrid */
				productionToEss, /* productionToEss */
				gridToConsumption, /* gridToConsumption */
				essToConsumption, /* essToConsumption */
				gridToEss /* gridToEss */);
	}
}
