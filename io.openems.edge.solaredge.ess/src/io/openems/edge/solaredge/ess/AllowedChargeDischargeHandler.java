package io.openems.edge.solaredge.ess;

import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.component.ClockProvider;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.ess.generic.common.AbstractAllowedChargeDischargeHandler;
import io.openems.edge.solaredge.ess.charger.SolarEdgeCharger;

import io.openems.edge.battery.api.Battery;
import io.openems.edge.batteryinverter.api.SymmetricBatteryInverter;

public class AllowedChargeDischargeHandler extends AbstractAllowedChargeDischargeHandler<SolarEdgeEssImpl> {

	public AllowedChargeDischargeHandler(SolarEdgeEssImpl parent) {
		super(parent);
	}

	@Override
	public void accept(ClockProvider clockProvider, Battery battery, SymmetricBatteryInverter inverter) {
		this.accept(clockProvider);
	}

	/**
	 * Calculates AllowedChargePower and AllowedDischargePower and sets the
	 * Channels.
	 *
	 * @param clockProvider a {@link ClockProvider}
	 */
	public void accept(ClockProvider clockProvider) {
		IntegerReadChannel bmsMaxChargePowerChannel = parent.channel(SolarEdgeEss.ChannelId.BATTERY1_MAX_CHARGE_CONTINUES_POWER);
		IntegerReadChannel bmsMaxDischargePowerChannel = parent.channel(SolarEdgeEss.ChannelId.BATTERY1_MAX_DISCHARGE_CONTINUES_POWER);
		var bmsPseudoVoltage = 1;
		var bmsChargePseudoImax = bmsMaxChargePowerChannel.getNextValue().orElse(0)/bmsPseudoVoltage;
		var bmsDischargePseudoImax = bmsMaxDischargePowerChannel.getNextValue().orElse(0)/bmsPseudoVoltage;
		this.calculateAllowedChargeDischargePower(clockProvider, true, bmsChargePseudoImax, bmsDischargePseudoImax, bmsPseudoVoltage);

		// Battery limits
		var batteryAllowedChargePower = Math.round(this.lastBatteryAllowedChargePower);
		var batteryAllowedDischargePower = Math.round(this.lastBatteryAllowedDischargePower);

		// Inverter limits
		var maxApparentPower = parent.getMaxApparentPower().orElse(0);

		// PV-Production
		Integer pvProduction = 0;
		for (SolarEdgeCharger charger : parent.chargers) {
			pvProduction = TypeUtils.sum(pvProduction, charger.getActualPowerChannel().getNextValue().orElse(0));
		}
		
		// Block battery charging on battery full
		if(parent.getSoc().orElse(100) >= 100)
			batteryAllowedChargePower = 0;
		
		// Block battery discharging on battery empty
		if(parent.getSoc().orElse(0) <= 10)
			batteryAllowedDischargePower = 0;

		// Calculates Maximum Allowed AC-Charge Power as positive numbers (or negative when force discharge is active)
		//   Force discharge: pvProduction>batteryAllowedChargePower requires a minimum discharge
		var acAllowedChargePower = batteryAllowedChargePower - pvProduction;
		
		// Calculates Maximum Allowed AC-Discharge Power as positive numbers
		var acAllowedDischargePower = TypeUtils.min(batteryAllowedDischargePower + pvProduction,parent.getMaxApparentPower().orElse(0));

		// Force Discharge active?
		if (acAllowedChargePower < 0) {

			// Limit forced DischargePower to maxApparentPower
			if(Math.abs(acAllowedChargePower)>maxApparentPower) acAllowedChargePower = maxApparentPower*(-1);

			// Make sure AllowedDischargePower is greater-or-equals absolute AllowedChargePower
			acAllowedDischargePower = Math.max(Math.abs(acAllowedChargePower), acAllowedDischargePower);
		} else {

			// Limit acChargerPower to maxApparentPower
			if(acAllowedChargePower>maxApparentPower) acAllowedChargePower = maxApparentPower;
		}

		// Apply AllowedChargePower and AllowedDischargePower
		this.parent._setAllowedChargePower(acAllowedChargePower * -1 /* invert charge power */);
		this.parent._setAllowedDischargePower(acAllowedDischargePower);
	}
}
