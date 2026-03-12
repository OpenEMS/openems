package io.openems.edge.pytes.ess;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;

import io.openems.edge.pytes.battery.PytesBattery;
import io.openems.edge.pytes.battery.PytesBatteryImpl;
import io.openems.edge.pytes.dccharger.PytesDcCharger;
import io.openems.edge.pytes.dccharger.PytesDcChargerImpl;
import io.openems.edge.pytes.ess.PytesJs3;

import java.util.Objects;
import java.util.stream.Stream;

import org.slf4j.Logger;

public class ApplyPowerHandler {

	// === Dependencies ===
	private final PytesJs3Impl ess;
	private final PytesBattery battery;
	private final PytesDcCharger dcCharger;
	private final Logger log;

	// === Smoothing & state ===
	private final AverageCalculator targetPowerAvg = new AverageCalculator(5);
	
	// --- Watchdog state ---
	private static final long KEEPALIVE_INTERVAL_MS = 60_000;
	private static final long MIN_WRITE_INTERVAL_MS = 10_000; // block writes for 10sec
	private long lastEssWriteMs = 0;
	

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
		/* ToDo
		if (ess.getWorkState() != WorkState.NORMAL) {
			log.error("ESS not in normal state. Skipping ApplyPower");
			return;
		}

		 */

		// --- Read inputs ---
		Integer maxApparentPower = this.ess.getMaxApparentPower().get();
		Integer batteryPower = this.battery.getDcDischargePower().get();
		// Integer powerAcGrid = ess.getGridOutPower().get();
		//Integer activePower = ess.getActivePower().orElse(0); ToDo: maybe necessary for offset calculation
		//Integer dcDischargePower = ess.getDcDischargePower().orElse(0);
		int pvPower = (this.dcCharger != null) ? this.dcCharger.getActualPower().get() :0 ;
		//Integer batteryVoltageRaw = this.battery.getStarterBatteryVoltage().get();
		Integer maxAllowedChargePower = this.ess.getAllowedChargePower().get();
		Integer maxAllowedDischargePower = this.ess.getAllowedDischargePower().get();
		
		long now = System.currentTimeMillis();
				
		// calculation of target power
		activePowerTarget = activePowerTarget - pvPower;

		this.targetPowerAvg.addValue(activePowerTarget);

		if (Stream.of(batteryPower, maxAllowedChargePower, maxAllowedDischargePower)
				.anyMatch(Objects::isNull)) {
			return;
		}
		if (maxApparentPower == null || maxApparentPower <= 0) {
			ess.logDebug(log, "Max Apparent power 0 or not available. Skipping ApplyPower");
			return;
		}

		
		int averageTargetPower = this.targetPowerAvg.getAverage();
		

		
		int setActivePowerValue = (int) Math.round(averageTargetPower / 10.0);  // Applied value has to be diveded by 10
		
		ess.setRemoteDispatchSwitch(1);
		ess.setRemoteDispatchTimeout(5);
		ess.setRemoteDispatchSystemLimitSwitch(0);
		ess.setRemoteDispatchSystemImportLimit(150);
		ess.setRemoteDispatchSystemExportLimit(150);
		ess.setRemoteDispatchRealtimeControlSwitch(2); // Set grid connection point
		ess.setRemoteDispatchRealtimeControlPower(setActivePowerValue *-1);		
	}

	
	
	
	// ========================= Helper =========================
	
	private boolean keepaliveDue(long nowMs) {
		return lastEssWriteMs == 0 || (nowMs - lastEssWriteMs) >= KEEPALIVE_INTERVAL_MS;
	}
	
	private boolean wroteRecently(long nowMs) {
	    return lastEssWriteMs != 0 && (nowMs - lastEssWriteMs) < MIN_WRITE_INTERVAL_MS;
	}	


	private int calculateDeciPercentFromPower(int maxApparentPower, int targetPower) {
		if (maxApparentPower <= 0) {
			throw new IllegalArgumentException("maxApparentPower must be > 0");
		}

		double percent = (double) targetPower * 100.0 / (double) maxApparentPower;

		// clamp to [-100, +100]
		percent = Math.max(-100.0, Math.min(100.0, percent));

		// 0.1% steps => [-1000..1000]
		return (int) Math.round(percent * 10.0);
	}

	/**
	 * Flags: "erstmal lassen", aber ohne Unterscheidung nach Charge/Discharge.
	 * Einheitliche Konfiguration.
	 */
	private void writeFlags() throws OpenemsNamedException {
		// 129: Generator Charging aus
		/*
		if (this.ess.getGeneratorCharingEnabled() != false) {
			this.ess.setGeneratorCharingEnabled(false);
*/

	}

}
