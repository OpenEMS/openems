package io.openems.edge.battery.soltaro.single.versionb;

import io.openems.edge.battery.soltaro.BatteryProtectionDefinitionSoltaro;
import io.openems.edge.common.channel.ChannelId;

public class BatteryProtectionDefinitionSoltaroSingleB extends BatteryProtectionDefinitionSoltaro {

	@Override
	public ChannelId getBmsAllowedChargeCurrent() {
		return SingleRackVersionB.ChannelId.SYSTEM_MAX_CHARGE_CURRENT;
	}

	@Override
	public ChannelId getBmsAllowedDischargeCurrent() {
		return SingleRackVersionB.ChannelId.SYSTEM_MAX_DISCHARGE_CURRENT;
	}

}
