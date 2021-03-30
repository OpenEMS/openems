package io.openems.edge.ess.generic.symmetric;

import io.openems.edge.battery.api.Battery;
import io.openems.edge.batteryinverter.api.ManagedSymmetricBatteryInverter;
import io.openems.edge.ess.generic.common.AbstractGenericEssChannelManager;
import io.openems.edge.ess.generic.common.GenericManagedEss;

public class ChannelManager extends AbstractGenericEssChannelManager<Battery, ManagedSymmetricBatteryInverter> {

	public ChannelManager(GenericManagedEss parent) {
		super(parent);
	}

}
