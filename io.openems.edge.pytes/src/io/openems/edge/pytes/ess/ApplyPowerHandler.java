package io.openems.edge.pytes.ess;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;

import io.openems.edge.pytes.battery.PytesBattery;
import io.openems.edge.pytes.dccharger.PytesDcCharger;
import io.openems.edge.pytes.enums.EnableDisable;
import io.openems.edge.pytes.enums.RemoteDispatchSystemLimitSwitch;

import org.slf4j.Logger;

public class ApplyPowerHandler {

	// === Dependencies ===
	private final PytesJs3Impl ess;
	private final PytesBattery battery;
	private final PytesDcCharger dcCharger;
	private final Logger log;

	// === Smoothing & state ===
	private final AverageCalculator targetBatteryPowerAvg = new AverageCalculator(5);

	public ApplyPowerHandler(PytesJs3Impl ess, PytesBattery battery, PytesDcCharger dcCharger) {
		this.ess = ess;
		this.battery = battery;
		this.dcCharger = dcCharger;
		this.log = ess.getLogger();
	}

	public void apply(int activePowerTarget, int reactivePower, int configuredMaxApparentPower)
			throws OpenemsNamedException {

		// --- Guards ---
		if (!ess.isManaged()) {
			log.debug("[ApplyPower] ESS not managed – skipping.");
			return;
		}
		/*
		 * ToDo if (ess.getWorkState() != WorkState.NORMAL) {
		 * log.error("ESS not in normal state. Skipping ApplyPower"); return; }
		 * 
		 */
		Integer maxAllowedChargePower = this.ess.getAllowedChargePower().get();
		Integer maxAllowedDischargePower = this.ess.getAllowedDischargePower().get(); // includes PV

		Integer maxApparentPower = this.ess.getMaxApparentPower().get();

		int pvPower = this.dcCharger != null ? this.dcCharger.getActualPower().orElse(0) : 0; // Maybe no pv connected
		
		Integer essActivePower = this.ess.getActivePower().get();
		Integer essDcDischargePower = this.ess.getDcDischargePower().get();


		Integer batteryPower = this.battery.getDcDischargePower().get();

		int batteryPowerTarget = 0;

		if (maxApparentPower == null) {
			log.error("[ApplyPower] maxApparentPower is null. Skipping ApplyPower");
			return;
		}

		if (maxAllowedChargePower == null) {
			log.error("[ApplyPower] maxAllowedChargePower is null. Skipping ApplyPower");
			return;
		}

		if (maxAllowedDischargePower == null) {
			log.error("[ApplyPower] maxAllowedDischargePower is null. Skipping ApplyPower");
			return;
		}

		if (batteryPower == null) {
			log.error("[ApplyPower] batteryPower is null. Skipping ApplyPower");
			return;
		}
		
		if (essActivePower == null) {
			log.error("[ApplyPower] essActivePower is null. Skipping ApplyPower");
			return;
		}	
		
		if (essDcDischargePower == null) {
			log.error("[ApplyPower] essDcDischargePower is null. Skipping ApplyPower");
			return;
		}		

		int pvPower2 = essActivePower - essDcDischargePower;
		
		// guards for AC
		maxApparentPower = Math.min(maxApparentPower, configuredMaxApparentPower);
		if (activePowerTarget > 0) { // discharging
			activePowerTarget = Math.min(activePowerTarget, maxApparentPower);
		} else {
			activePowerTarget = Math.max(activePowerTarget, -maxApparentPower);
		}		
		
		batteryPowerTarget = activePowerTarget - pvPower2;

		// guards for DC
		int maxAllowedBatteryDischargePower = Math.max(0, maxAllowedDischargePower - pvPower);		
		

		// DC-side clamp
		if (batteryPowerTarget > 0) { // discharge
			batteryPowerTarget = Math.min(batteryPowerTarget, maxAllowedBatteryDischargePower);
		} else { // charge
			batteryPowerTarget = Math.max(batteryPowerTarget, maxAllowedChargePower); // already negative
		}

		this.targetBatteryPowerAvg.addValue(batteryPowerTarget);

		int averageBatteryTargetPower = this.targetBatteryPowerAvg.getAverage();

		batteryPowerTarget = (int) Math.round(averageBatteryTargetPower / 10.0); // Applied value has to be diveded by
																					// 10

		this.writeFlags();
		/*
		 * Definition is determined by44105 control switch 1->10W Default : 0W 
		 * • When 44105=1, this register’s value is not effective 
		 * • When 44105=2, Negative value is battery discharge power, positive value is battery charge power.
		 * Range: Negative maxcharge/discharge power* parallel unit number ~Positive max
		 * charge/discharge power* parallelunit number 
		 * • When 44105=3, Negative value is Import power, positive value is Export power. Range:Negative inverter max
		 * output power* parallelunit number ~ Positiveinverter max outputpower*
		 * parallel unit number 
		 * • When 44105=4, Negative value is Import power, positive
		 * value is Export power. Range:Negative inverter max output power* parallelunit
		 * number ~ Positiveinverter max outputpower* parallel unit number
		 * 
		 * 
		 */

		batteryPowerTarget = batteryPowerTarget * -1;


		//batteryPowerTarget = 0;
		
		this.ess.debugLog("[ApplyPower] Battery hardware SetPoint: " + batteryPowerTarget + " [*10W]. PV Power 1/2 " + pvPower + "/" + pvPower2);
		
		ess.setRemoteDispatchRealtimeControlPower(batteryPowerTarget);
	}

	// ========================= Helper =========================

	/**
	 * Flags: "erstmal lassen", aber ohne Unterscheidung nach Charge/Discharge.
	 * Einheitliche Konfiguration.
	 */
	private void writeFlags() throws OpenemsNamedException {
		ess.setRemoteDispatchSwitch(EnableDisable.ENABLE);
		ess.setRemoteDispatchTimeout(5); // in Minutes
		ess.setRemoteDispatchSystemLimitSwitch(RemoteDispatchSystemLimitSwitch.DISABLE); // 44102 0
													// -> No
													// import
													// /
													// export
													// limitation

		// 44103 - not effective if 44102 == 0
		// ess.setRemoteDispatchSystemImportLimit(150);
		// 44104 - not effective if 44102 == 0
		// ess.setRemoteDispatchSystemExportLimit(150);

		/*
		 * 1: Battery Standby Control(No Charge/No Discharge at all) 2: Battery
		 * Charge/Discharge Control 3: Grid Connection Point Import/Export Control 4. AC
		 * Grid Port Import/Export Control Default :1 , others invalid
		 */
		ess.setRemoteDispatchRealtimeControlSwitch(2); // Battery Charge/Discharge Control

	}

}
