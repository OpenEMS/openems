package io.openems.edge.deye.ess;

import org.slf4j.Logger;

import io.openems.edge.battery.api.Battery;
import io.openems.edge.batteryinverter.api.SymmetricBatteryInverter;
import io.openems.edge.common.component.ClockProvider;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.deye.battery.DeyeSunBattery;
import io.openems.edge.deye.dccharger.DeyeDcCharger;
import io.openems.edge.ess.generic.common.AbstractAllowedChargeDischargeHandler;

public class AllowedChargeDischargeHandler extends AbstractAllowedChargeDischargeHandler<DeyeSunHybridImpl> {

	private DeyeSunBattery battery;
	private final Logger log;

	public AllowedChargeDischargeHandler(DeyeSunHybridImpl parent, DeyeSunBattery battery, DeyeDcCharger dcCharger) {
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
		
		// values from BMS regarding hardware limits
		//Integer bmsMaxChargeCurrent = this.battery.getChargeMaxCurrent().get(); // A
		//Integer bmsMaxDischargeCurrent = this.battery.getDischargeMaxCurrent().get(); // A
		
		Integer bmsMaxChargeCurrent = this.battery.getBmsChargeCurrentLimit().get();
		Integer bmsMaxDischargeCurrent = this.battery.getBmsDischargeCurrentLimit().get();
		Integer bmsVoltage = this.battery.getBatteryVoltage().orElse(0); // mV
		
		// configured values - cannot be used as we use these channels for battery controlling
		Integer configuredBatteryMaxChargeCurrent = this.battery.getConfiguredMaxChargeCurrent(); // A
		Integer configuredBatteryMaxDischargeCurrent = this.battery.getConfiguredMaxDischargeCurrent(); // A
		

		if (bmsMaxChargeCurrent == null || bmsMaxDischargeCurrent == null || bmsVoltage == null ) {
			this.parent.logDebug(log, "[AllowChargeDischarge Handler] BMS values not available. Setting 0 W.");
	        parent._setAllowedChargePower(0);
	        parent._setAllowedDischargePower(0);
			return;
		}		
		
		double voltage = bmsVoltage / 1000.0;		
		
	    int maxChargeCurrent = Math.min((bmsMaxChargeCurrent),configuredBatteryMaxChargeCurrent); // A
	    int maxDischargeCurrent =  Math.min((bmsMaxDischargeCurrent),configuredBatteryMaxDischargeCurrent); // A
		
	    // 
	    double allowedChargePower = maxChargeCurrent * voltage * -1; // negative for charging
	    double allowedDischargePower = maxDischargeCurrent * voltage; // positive for discharging


		this.parent.logDebug(log,"[AllowChargeDischarge Handler] max. ChargeCurrent  " + maxChargeCurrent 
		+ "A maxDischargeCurrent: " + maxDischargeCurrent 
		+ "A Voltage:"  + voltage  
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
