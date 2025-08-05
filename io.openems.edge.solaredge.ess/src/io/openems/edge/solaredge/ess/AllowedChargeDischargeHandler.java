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

		// PV-Production
		Integer pvProduction = 0;
		for (SolarEdgeCharger charger : parent.chargers) {
			pvProduction = TypeUtils.sum(pvProduction, charger.getActualPowerChannel().getNextValue().orElse(0));
		}
		
		// Calculates Maximum Allowed AC-Charge Power as positive numbers
		if(parent.getSoc().orElse(100) >= 100) batteryAllowedChargePower = 0;
		else batteryAllowedChargePower = TypeUtils.subtract(batteryAllowedChargePower,
				TypeUtils.min(batteryAllowedChargePower /* avoid negative number for `subtract` */, pvProduction));			
		
		// Calculates Maximum Allowed AC-Discharge Power as positive numbers
		if(parent.getSoc().orElse(0) > 10) batteryAllowedDischargePower = batteryAllowedDischargePower + pvProduction;
		else batteryAllowedDischargePower = pvProduction;
		
		// Apply AllowedChargePower and AllowedDischargePower
		this.parent._setAllowedChargePower(batteryAllowedChargePower * -1 /* invert charge power */);
		this.parent._setAllowedDischargePower(batteryAllowedDischargePower);
	}
	
}
