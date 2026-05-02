package io.openems.edge.ess.generic.symmetric;

import static io.openems.edge.common.channel.ChannelUtils.setValue;

import io.openems.edge.battery.api.Battery;
import io.openems.edge.batteryinverter.api.SymmetricBatteryInverter;
import io.openems.edge.common.component.ClockProvider;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.generic.common.AbstractAllowedChargeDischargeHandler;

public class AllowedChargeDischargeHandler extends AbstractAllowedChargeDischargeHandler<EssGenericManagedSymmetric> {

	public AllowedChargeDischargeHandler(EssGenericManagedSymmetric parent) {
		super(parent);
	}

	@Override
	public void accept(ClockProvider clockProvider, Battery battery, SymmetricBatteryInverter inverter) {
		this.calculateAllowedChargeDischargePower(clockProvider, battery, inverter);

		// Battery limits
		var batteryAllowedChargePower = Math.round(this.lastBatteryAllowedChargePower);
		var batteryAllowedDischargePower = Math.round(this.lastBatteryAllowedDischargePower);

		// PV-Production (for HybridEss)
		var pvProduction = Math.max(//
				TypeUtils.orElse(//
						TypeUtils.subtract(this.parent.getActivePower().get(), this.parent.getDcDischargePower().get()), //
						0),
				0);

		// Apply AllowedChargePower and AllowedDischargePower
		setValue(this.parent, ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER,
				batteryAllowedChargePower * -1 /* invert charge power */);
		setValue(this.parent, ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER,
				batteryAllowedDischargePower + pvProduction);
	}

}
