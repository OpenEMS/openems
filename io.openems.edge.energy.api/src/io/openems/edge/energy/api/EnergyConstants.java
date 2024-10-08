package io.openems.edge.energy.api;

import io.openems.common.types.ChannelAddress;

public class EnergyConstants {

	public static final int PERIODS_PER_HOUR = 4;

	public static final ChannelAddress SUM_PRODUCTION = new ChannelAddress("_sum", "ProductionActivePower");
	public static final ChannelAddress SUM_CONSUMPTION = new ChannelAddress("_sum", "ConsumptionActivePower");
	public static final ChannelAddress SUM_UNMANAGED_CONSUMPTION = new ChannelAddress("_sum",
			"UnmanagedConsumptionActivePower");

	private EnergyConstants() {
	}
}
