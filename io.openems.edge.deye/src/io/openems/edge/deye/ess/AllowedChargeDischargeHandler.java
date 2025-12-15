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
		
		Integer configureableMaxChargeCurrent = this.battery.getConfigurableChargeCurrentLimit().get();        // [A] 108
		Integer configurableMaxDischargeCurrent = this.battery.getConfigurableDischargeCurrentLimit().get();  // [A] 109

		Integer batteryMaxChargeCurrent = this.battery.getChargeMaxCurrent().get();        // [A] 212 (dynamic)
		Integer batteryMaxDischargeCurrent = this.battery.getDischargeMaxCurrent().get();  // [A] 213 (dynamic)

		Integer offgridMaxChargeCurrent = this.battery.getOffgridMaxChargeCurrent().get();        // [A] 218 (dynamic)
		Integer offgridMaxDischargeCurrent = this.battery.getOffgridMaxDischargeCurrent().get();  // [A] 219 (dynamic)

		Integer configuredBatteryMaxChargeCurrent = this.battery.getConfiguredMaxChargeCurrent();        // [A] OpenEMS user
		Integer configuredBatteryMaxDischargeCurrent = this.battery.getConfiguredMaxDischargeCurrent();  // [A] OpenEMS user

		Integer bmsVoltage = this.battery.getBatteryVoltage().orElse(null); // [mV]

		if (configureableMaxChargeCurrent == null
		        || configurableMaxDischargeCurrent == null
		        || batteryMaxChargeCurrent == null
		        || batteryMaxDischargeCurrent == null
		        || offgridMaxChargeCurrent == null
		        || offgridMaxDischargeCurrent == null
		        || configuredBatteryMaxChargeCurrent == null
		        || configuredBatteryMaxDischargeCurrent == null
		        || bmsVoltage == null
		        || bmsVoltage <= 0) {

		    this.parent.logDebug(log, "[AllowChargeDischarge Handler] Values not available. Setting 0 W.");
		    parent._setAllowedChargePower(0);
		    parent._setAllowedDischargePower(0);
		    return;
		}

		double voltage = bmsVoltage / 1000.0; // [V]

		// Min across: configurable (108/109), OpenEMS user, dynamic (212/213), dynamic offgrid (218/219)
		int maxChargeCurrent = Math.min(
		        configureableMaxChargeCurrent,
		        Math.min(configuredBatteryMaxChargeCurrent,
		                Math.min(batteryMaxChargeCurrent, offgridMaxChargeCurrent))
		);

		int maxDischargeCurrent = Math.min(
		        configurableMaxDischargeCurrent,
		        Math.min(configuredBatteryMaxDischargeCurrent,
		                Math.min(batteryMaxDischargeCurrent, offgridMaxDischargeCurrent))
		);

		// Convert A * V -> W (floor so we never exceed)
		int allowedChargePower = (int) Math.min(0, Math.ceil(maxChargeCurrent * voltage * -1));
		int allowedDischargePower = (int) Math.max(0, Math.floor(maxDischargeCurrent * voltage));


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
