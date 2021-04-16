package io.openems.edge.ess.generic.symmetric;

import io.openems.edge.battery.api.Battery;
import io.openems.edge.batteryinverter.api.ManagedSymmetricBatteryInverter;
import io.openems.edge.ess.generic.common.AbstractChannelManager;

public class ChannelManager
		extends AbstractChannelManager<GenericManagedSymmetricEss, Battery, ManagedSymmetricBatteryInverter> {

	public ChannelManager(GenericManagedSymmetricEss parent) {
		super(parent, new AllowedChargeDischargeHandler(parent));
	}

}
