package io.openems.edge.pytes.ess;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;

import io.openems.edge.pytes.battery.PytesBattery;
import io.openems.edge.pytes.dccharger.PytesDcCharger;
import io.openems.edge.pytes.ess.PytesJs3;

import java.util.Objects;
import java.util.stream.Stream;

import org.slf4j.Logger;

public class ApplyPowerHandler {

	// === Dependencies ===
	private final PytesJs3 ess;
	private final PytesBattery battery;
	private final PytesDcCharger dcCharger;
	private final Logger log;

	// === Smoothing & state ===
	private final AverageCalculator targetPowerAvg = new AverageCalculator(5);
	private int powerDeciPercentLast = 0;
	
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
/*
		// --- Guards ---
		if (!ess.isManaged()) {
			log.debug("[ApplyPower] ESS not managed – skipping.");
			return;
		}
		if (ess.getWorkState() != WorkState.NORMAL) {
			log.error("ESS not in normal state. Skipping ApplyPower");
			return;
		}

		// --- Read inputs ---
		Integer maxApparentPower = ess.getMaxApparentPower().get();
		Integer batteryPower = battery.getDcPower().get();
		// Integer powerAcGrid = ess.getGridOutPower().get();
		Integer activePower = ess.getActivePower().orElse(0);
		Integer dcDischargePower = ess.getDcDischargePower().orElse(0);
		int pvPower = (dcCharger != null) ? dcCharger.getActualPower().orElse(0) : 0;
		Integer batteryVoltageRaw = battery.getBatteryVoltage().get();
		Integer maxAllowedChargePower = ess.getAllowedChargePower().get();
		Integer maxAllowedDischargePower = ess.getAllowedDischargePower().get();
		Integer activeDeciPercent = ess.getBatteryPowerDeciPercent().orElse(0);
		
		long now = System.currentTimeMillis();
				
		// calculation of target power
		activePowerTarget = activePowerTarget - pvPower;

		this.targetPowerAvg.addValue(activePowerTarget);

		if (Stream.of(batteryVoltageRaw, batteryPower, maxAllowedChargePower, maxAllowedDischargePower)
				.anyMatch(Objects::isNull)) {
			return;
		}
		if (maxApparentPower == null || maxApparentPower <= 0) {
			ess.logDebug(log, "Max Apparent power 0 or not available. Skipping ApplyPower");
			return;
		}

		int powerDeciPercent = calculateDeciPercentFromPower(maxApparentPower, activePowerTarget);
		int averageTargetPower = this.targetPowerAvg.getAverage();
		
		// Criteria for 'write now'
		boolean targetMoved = Math.abs(averageTargetPower - activePowerTarget) > 100;
		boolean setpointChanged = powerDeciPercent != this.powerDeciPercentLast;

		// Watchdog: write forced if watchdog time exceeded
		boolean keepalive = keepaliveDue(now);
		boolean recentWrite = wroteRecently(now);
		
		boolean shouldWrite = !recentWrite && (keepalive || (targetMoved && setpointChanged));
/*		
		ess.logDebug(log,"keepalive: " + keepalive + " recentWrite: " + recentWrite + " targetMoved: " + targetMoved + " setpointChanged: " + setpointChanged);

		if (shouldWrite) {			

			this.writeFlags();
			
			this.writeSetpointToEss(powerDeciPercent);

			int powerPercent = (int) Math.round((double) powerDeciPercent / 10.0);

			if (keepalive && !setpointChanged && !targetMoved) {
				ess.logDebug(log, "Keepalive write to ESS (no setpoint change) -> " + powerPercent + "% (" + powerDeciPercent + " d%)");
			} else {
				ess.logDebug(log,
					"\n\n\n Writing values to ESS. Active DeciPercent: " + activeDeciPercent
					+ "\n-> AC target: " + activePowerTarget + " avg Target:" + averageTargetPower + "(" + powerPercent + "[d%]) "
					+ "\n   PV: " + pvPower
					+ "\n   DcDisCharge: " + dcDischargePower
					+ "\n   ActivePower: " + activePower
					+ "\n | EnergyManagementModel: " + this.ess.getEnergyManagementModel()
					+ "\n | LimitControlFunction: " + this.ess.getLimitControlFunction()
					+ "\n | SolarSellMode: " + this.ess.getSolarSellMode()
					+ "\n | GridChargeEnabled: " + this.ess.getGridCharingEnabled()
				);
			}
		}

*/
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
