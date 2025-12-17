package io.openems.edge.energy.api;

import io.openems.common.types.ChannelAddress;

public class EnergyConstants {

	public static final int PERIODS_PER_HOUR = 4;

	/**
	 * Number of Periods to Schedule if neither Prediction nor Prices are available.
	 */
	public static final int SCHEDULE_PERIODS_ON_EMPTY = 94;

	public static final ChannelAddress SUM_PRODUCTION = new ChannelAddress("_sum", "ProductionActivePower");
	public static final ChannelAddress SUM_UNMANAGED_CONSUMPTION = new ChannelAddress("_sum",
			"UnmanagedConsumptionActivePower");

	private EnergyConstants() {
	}
}
