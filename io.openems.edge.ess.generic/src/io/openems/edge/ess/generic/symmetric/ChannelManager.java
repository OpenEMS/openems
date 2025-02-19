package io.openems.edge.ess.generic.symmetric;

import io.openems.edge.battery.api.Battery;
import io.openems.edge.batteryinverter.api.HybridManagedSymmetricBatteryInverter;
import io.openems.edge.batteryinverter.api.ManagedSymmetricBatteryInverter;
import io.openems.edge.batteryinverter.api.SymmetricBatteryInverter;
import io.openems.edge.common.component.ClockProvider;
import io.openems.edge.ess.api.HybridEss;
import io.openems.edge.ess.generic.common.AbstractChannelManager;

public class ChannelManager
		extends AbstractChannelManager<EssGenericManagedSymmetric, Battery, ManagedSymmetricBatteryInverter> {

	public ChannelManager(EssGenericManagedSymmetric parent) {
		super(parent, new AllowedChargeDischargeHandler(parent));
	}

	@Override
	public void activate(ClockProvider clockProvider, Battery battery,
			ManagedSymmetricBatteryInverter batteryInverter) {
		super.activate(clockProvider, battery, batteryInverter);

		switch (batteryInverter) {
		case HybridManagedSymmetricBatteryInverter hmsbi -> {
			this.<Long>addCopyListener(hmsbi, //
					HybridManagedSymmetricBatteryInverter.ChannelId.DC_CHARGE_ENERGY, //
					HybridEss.ChannelId.DC_CHARGE_ENERGY);
			this.<Long>addCopyListener(hmsbi, //
					HybridManagedSymmetricBatteryInverter.ChannelId.DC_DISCHARGE_ENERGY, //
					HybridEss.ChannelId.DC_DISCHARGE_ENERGY);
			this.<Long>addCopyListener(hmsbi, //
					HybridManagedSymmetricBatteryInverter.ChannelId.DC_DISCHARGE_POWER, //
					HybridEss.ChannelId.DC_DISCHARGE_POWER);
		}
		case ManagedSymmetricBatteryInverter msbi -> {
			this.<Long>addCopyListener(msbi, //
					SymmetricBatteryInverter.ChannelId.ACTIVE_CHARGE_ENERGY, //
					HybridEss.ChannelId.DC_CHARGE_ENERGY);
			this.<Long>addCopyListener(msbi, //
					SymmetricBatteryInverter.ChannelId.ACTIVE_DISCHARGE_ENERGY, //
					HybridEss.ChannelId.DC_DISCHARGE_ENERGY);
			this.<Long>addCopyListener(msbi, //
					SymmetricBatteryInverter.ChannelId.ACTIVE_POWER, //
					HybridEss.ChannelId.DC_DISCHARGE_POWER);
		}
		}
	}
}
