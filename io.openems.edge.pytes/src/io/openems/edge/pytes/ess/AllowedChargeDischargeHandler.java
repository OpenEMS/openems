package io.openems.edge.pytes.ess;

import org.slf4j.Logger;

import io.openems.edge.battery.api.Battery;
import io.openems.edge.batteryinverter.api.SymmetricBatteryInverter;
import io.openems.edge.common.component.ClockProvider;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.ess.generic.common.AbstractAllowedChargeDischargeHandler;
import io.openems.edge.pytes.battery.PytesBattery;
import io.openems.edge.pytes.dccharger.PytesDcCharger;
import io.openems.edge.pytes.ess.PytesJs3Impl;

public class AllowedChargeDischargeHandler extends AbstractAllowedChargeDischargeHandler<PytesJs3Impl> {

	private PytesBattery battery;
	private final Logger log;

	public AllowedChargeDischargeHandler(PytesJs3Impl parent, PytesBattery battery, PytesDcCharger dcCharger) {
		super(parent);
		this.battery = battery;
		this.log = this.parent.getLogger();
	}

	@Override
	public void accept(ClockProvider clockProvider, Battery battery, SymmetricBatteryInverter inverter) {

		if (battery == null) {

			parent._setAllowedChargePower(0);
			parent._setAllowedDischargePower(0);
			return;
		}
		this.accept(clockProvider);
	}

	/**
	 * Calculates AllowedChargePower and AllowedDischargePower and sets the
	 * Channels.
	 *
	 * @param clockProvider a {@link ClockProvider}
	 */
	public void accept(ClockProvider clockProvider) {

		if (battery == null) {
		    parent._setAllowedChargePower(0);
		    parent._setAllowedDischargePower(0);
		    return;
		    
		}
		
		Double batteryMaxChargeCurrent = this.battery.getBmsChargeCurrentLimit().get() / 1000.0; // mA -> A
		Double batteryMaxDischargeCurrent = this.battery.getBmsDischargeCurrentLimit().get() / 1000.0;

		Integer configuredMaxChargeCurrent = this.battery.getConfiguredMaxChargeCurrent(); // A
		Integer configuredMaxDischargeCurrent = this.battery.getConfiguredMaxDischargeCurrent();

		Integer batteryVoltage = this.battery.getVoltage().get(); // V


/*
		    this.parent.logDebug(log, "[AllowChargeDischarge Handler] Values not available. Setting 0 W.");
		    parent._setAllowedChargePower(0);
		    parent._setAllowedDischargePower(0);
		    return;
		}
*/

		int maxChargeCurrent = (int)    Math.min(configuredMaxChargeCurrent,batteryMaxChargeCurrent);
		int maxDischargeCurrent = (int)    Math.min(configuredMaxDischargeCurrent,batteryMaxDischargeCurrent);

		// Convert A * V -> W (floor so we never exceed)
		int allowedChargePower = (int) Math.min(0, Math.ceil(maxChargeCurrent * batteryVoltage * -1));
		int allowedDischargePower = (int) Math.max(0, Math.floor(maxDischargeCurrent * batteryVoltage));

		this.parent.logDebug(log,"[AllowChargeDischarge Handler] max. ChargeCurrent  " + maxChargeCurrent 
		+ "A maxDischargeCurrent: " + maxDischargeCurrent 
		+ "A Voltage:"  + batteryVoltage  
		+ "V Allowed Charge Power "+ allowedChargePower
		+ "W/Allowed Discharge Power "+ allowedDischargePower
	
		 );

		// PV-Production
		var pvProduction = Math.max(//
				TypeUtils.orElse(//
						TypeUtils.subtract(this.parent.getActivePower().get(), this.parent.getDcDischargePower().get()), //
						0),
				0);
		// Apply AllowedChargePower and AllowedDischargePower
		this.parent._setAllowedChargePower((int) allowedChargePower); // 0 or negative
		this.parent._setAllowedDischargePower((int) allowedDischargePower + pvProduction); // positive
	}
	
}
