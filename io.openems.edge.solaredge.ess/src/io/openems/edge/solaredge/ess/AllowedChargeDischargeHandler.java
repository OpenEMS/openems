package io.openems.edge.solaredge.ess;

import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.component.ClockProvider;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.ess.generic.common.AbstractAllowedChargeDischargeHandler;

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
		var bmsChargePseudoImax = bmsMaxChargePowerChannel.value().orElse(0)/bmsPseudoVoltage;
		var bmsDischargePseudoImax = bmsMaxDischargePowerChannel.value().orElse(0)/bmsPseudoVoltage;
		this.calculateAllowedChargeDischargePower(clockProvider, true, bmsChargePseudoImax, bmsDischargePseudoImax, bmsPseudoVoltage);

		// Battery limits
		var batteryAllowedChargePower = Math.round(this.lastBatteryAllowedChargePower);
		var batteryAllowedDischargePower = Math.round(this.lastBatteryAllowedDischargePower);

		// PV-Production
		var pvProduction = Math.max(0, TypeUtils.orElse(this.parent.getPvProduction(),0));
		
		// Minimum SOC = 10%
		if(parent.getSoc().orElse(0) > 10) batteryAllowedDischargePower = batteryAllowedDischargePower + pvProduction;
		else batteryAllowedDischargePower = pvProduction;
		
		// Maximum SOC = 100%
		if(parent.getSoc().orElse(0) == 100) batteryAllowedChargePower = 0;
			
		// Apply AllowedChargePower and AllowedDischargePower
		this.parent._setAllowedChargePower(batteryAllowedChargePower * -1 /* invert charge power */);
		this.parent._setAllowedDischargePower(batteryAllowedDischargePower);
	}
	
}
