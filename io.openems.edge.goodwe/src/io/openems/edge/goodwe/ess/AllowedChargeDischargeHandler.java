package io.openems.edge.goodwe.ess;

import io.openems.edge.battery.api.Battery;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.component.ClockProvider;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.ess.generic.common.AbstractAllowedChargeDischargeHandler;
import io.openems.edge.goodwe.common.GoodWe;

public class AllowedChargeDischargeHandler extends AbstractAllowedChargeDischargeHandler<GoodWeEssImpl> {

	public AllowedChargeDischargeHandler(GoodWeEssImpl parent) {
		super(parent);
	}

	@Override
	public void accept(ClockProvider clockProvider, Battery battery) {
		this.accept(clockProvider);
	}

	public void accept(ClockProvider clockProvider) {
		IntegerReadChannel bmsChargeImaxChannel = parent.channel(GoodWe.ChannelId.BMS_CHARGE_IMAX);
		Integer bmsChargeImax = bmsChargeImaxChannel.value().get();
		IntegerReadChannel bmsDischargeImaxChannel = parent.channel(GoodWe.ChannelId.BMS_DISCHARGE_IMAX);
		Integer bmsDischargeImax = bmsDischargeImaxChannel.value().get();
		IntegerReadChannel wbmsBatVoltageChannel = parent.channel(GoodWe.ChannelId.WBMS_BAT_VOLTAGE);
		Integer wbmsBatVoltage = wbmsBatVoltageChannel.value().get();
		this.calculateAllowedChargeDischargePower(clockProvider, true, bmsChargeImax, bmsDischargeImax, wbmsBatVoltage);

		// Battery limits
		int batteryAllowedChargePower = Math.round(this.lastBatteryAllowedChargePower);
		int batteryAllowedDischargePower = Math.round(this.lastBatteryAllowedDischargePower);

		// PV-Production
		int pvProduction = Math.max(//
				TypeUtils.orElse(//
						TypeUtils.subtract(this.parent.getActivePower().get(), this.parent.getDcDischargePower().get()), //
						0),
				0);

		System.out.println("Set AllowedCharge/Discharge " + (batteryAllowedChargePower * -1) + " | "
				+ (batteryAllowedDischargePower + pvProduction));

		// Apply AllowedChargePower and AllowedDischargePower
		this.parent._setAllowedChargePower(batteryAllowedChargePower * -1 /* invert charge power */);
		this.parent._setAllowedDischargePower(batteryAllowedDischargePower + pvProduction);
	}

}
