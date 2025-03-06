package io.openems.edge.core.sum;

import static java.lang.Math.max;
import static java.lang.Math.min;

import io.openems.edge.common.sum.Sum;

/**
 * Calculates power distribution.
 */
public record PowerDistribution(//
		Integer productionToConsumption, /* positive */
		Integer productionToGrid, /* positive */
		Integer productionToEss, /* positive */
		Integer gridToConsumption, /* positive */
		Integer essToConsumption, /* positive */
		Integer gridToEss /* discharge-to-grid negative, charge-from-grid positive */
) {

	public static final PowerDistribution EMPTY = new PowerDistribution(null, null, null, null, null, null);

	/**
	 * Creates a {@link PowerDistribution}.
	 * 
	 * @param grid       the gridActivePower; possibly null
	 * @param production the productionActivePower; possibly null
	 * @param ess        the essDcDischargePower; possibly null
	 * @return a {@link PowerDistribution}; possibly {@link PowerDistribution#EMPTY}
	 */
	public static PowerDistribution of(Integer grid, Integer production, Integer ess) {
		final PowerDistribution result;
		if (grid != null && production != null && ess != null) {
			result = of(grid.intValue(), production.intValue(), ess.intValue());

		} else if (grid != null) {
			if (production == null && ess == null) {
				result = of(grid.intValue());
			} else {
				result = of(grid.intValue(), production != null ? production : 0, ess != null ? ess : 0);
			}

		} else {
			result = EMPTY;
		}
		return result;
	}

	private static PowerDistribution of(int grid, int production, int ess) {
		if (production < 0) { // invalid data
			return EMPTY;
		}
		var consumption = ess + grid + production;
		var essToConsumption = ess > 0 //
				? /* discharge */ min(ess, consumption) //
				: /* charge */ 0;
		var productionToEss = ess > 0 //
				? /* discharge */ 0 //
				: /* charge */ min(-ess, production);
		var productionToConsumption = min(production - productionToEss, consumption - essToConsumption);
		var productionToGrid = max(0, production - productionToConsumption - productionToEss);
		var gridToConsumption = max(0, consumption - essToConsumption - productionToConsumption);
		var gridToEss = grid - gridToConsumption + productionToGrid;
		return new PowerDistribution(productionToConsumption, productionToGrid, productionToEss, gridToConsumption,
				essToConsumption, gridToEss);
	}

	private static PowerDistribution of(int grid) {
		if (grid < 0) { // invalid data
			return EMPTY;
		}
		// Grid Buy to Consumption
		return new PowerDistribution(null, null, null, grid, null, null);
	}

	protected void updateChannels(SumImpl sum) {
		// Power
		sum.channel(Sum.ChannelId.PRODUCTION_TO_CONSUMPTION_POWER).setNextValue(this.productionToConsumption);
		sum.channel(Sum.ChannelId.PRODUCTION_TO_GRID_POWER).setNextValue(this.productionToGrid);
		sum.channel(Sum.ChannelId.PRODUCTION_TO_ESS_POWER).setNextValue(this.productionToEss);
		sum.channel(Sum.ChannelId.GRID_TO_CONSUMPTION_POWER).setNextValue(this.gridToConsumption);
		sum.channel(Sum.ChannelId.ESS_TO_CONSUMPTION_POWER).setNextValue(this.essToConsumption);
		sum.channel(Sum.ChannelId.GRID_TO_ESS_POWER).setNextValue(this.gridToEss);

		// Energy
		sum.calculateProductionToConsumptionEnergy.update(this.productionToConsumption);
		sum.calculateProductionToGridEnergy.update(this.productionToGrid);
		sum.calculateProductionToEssEnergy.update(this.productionToEss);
		sum.calculateGridToConsumptionEnergy.update(this.gridToConsumption);
		sum.calculateEssToConsumptionEnergy.update(this.essToConsumption);
		if (this.gridToEss == null) {
			sum.calculateGridToEssEnergy.update(null);
			sum.calculateEssToGridEnergy.update(null);
		} else if (this.gridToEss > 0) {
			sum.calculateGridToEssEnergy.update(this.gridToEss);
			sum.calculateEssToGridEnergy.update(0);
		} else {
			sum.calculateGridToEssEnergy.update(0);
			sum.calculateEssToGridEnergy.update(-this.gridToEss);
		}
	}
}
