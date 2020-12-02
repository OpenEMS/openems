package io.openems.edge.ess.generic.hybrid;

import io.openems.edge.battery.api.Battery;
import io.openems.edge.batteryinverter.api.HybridManagedSymmetricBatteryInverter;
import io.openems.edge.ess.generic.common.AbstractGenericEssChannelManager;
import io.openems.edge.ess.generic.common.GenericManagedEss;

public class ChannelManager extends AbstractGenericEssChannelManager<Battery, HybridManagedSymmetricBatteryInverter> {

	public ChannelManager(GenericManagedEss parent) {
		super(parent);
	}

}
