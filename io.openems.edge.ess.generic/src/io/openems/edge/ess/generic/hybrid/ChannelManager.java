package io.openems.edge.ess.generic.hybrid;

import io.openems.edge.battery.api.Battery;
import io.openems.edge.batteryinverter.api.HybridManagedSymmetricBatteryInverter;
import io.openems.edge.batteryinverter.api.ManagedSymmetricBatteryInverter;
import io.openems.edge.common.component.ClockProvider;
import io.openems.edge.ess.api.HybridEss;
import io.openems.edge.ess.generic.common.AbstractGenericEssChannelManager;
import io.openems.edge.ess.generic.common.GenericManagedEss;

public class ChannelManager extends AbstractGenericEssChannelManager<Battery, HybridManagedSymmetricBatteryInverter> {

	public ChannelManager(GenericManagedEss parent) {
		super(parent);
	}

	@Override
	public void activate(ClockProvider clockProvider, Battery battery,
			ManagedSymmetricBatteryInverter batteryInverter) {
		super.activate(clockProvider, battery, batteryInverter);

		this.<Long>addCopyListener(batteryInverter, //
				HybridManagedSymmetricBatteryInverter.ChannelId.DC_CHARGE_ENERGY, //
				HybridEss.ChannelId.DC_CHARGE_ENERGY);
		this.<Long>addCopyListener(batteryInverter, //
				HybridManagedSymmetricBatteryInverter.ChannelId.DC_DISCHARGE_ENERGY, //
				HybridEss.ChannelId.DC_DISCHARGE_ENERGY);
		this.<Long>addCopyListener(batteryInverter, //
				HybridManagedSymmetricBatteryInverter.ChannelId.DC_DISCHARGE_POWER, //
				HybridEss.ChannelId.DC_DISCHARGE_POWER);
	}

}
