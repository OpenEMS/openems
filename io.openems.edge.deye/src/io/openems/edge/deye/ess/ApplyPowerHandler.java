package io.openems.edge.deye.ess;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.deye.battery.DeyeSunBattery;
import io.openems.edge.deye.dccharger.DeyeDcCharger;

import io.openems.edge.deye.enums.WorkState;
import io.openems.edge.deye.enums.EnableDisable;
import io.openems.edge.deye.enums.EnergyManagementModel;
import io.openems.edge.deye.enums.LimitControlFunction;

import java.util.Objects;
import java.util.stream.Stream;

import org.slf4j.Logger;

public class ApplyPowerHandler {

	// === Dependencies ===
	private final DeyeSunHybridImpl ess;
	private final DeyeSunBattery battery;
	private final DeyeDcCharger dcCharger;
	private final Logger log;

	// === Smoothing & state ===
	private final AverageCalculator targetPowerAvg = new AverageCalculator(5);

	// === Last written values ===
	private int iSetLast = 0;

	// Pv correction
	private int lastUsedPvAc = 0;
	
	private int cycleCounter = 0;

	private static final int MAX_A = 20;

	// === Mode ===
	private enum FlowMode {
		CHARGE, DISCHARGE
	}

	private FlowMode lastMode = null;

	public ApplyPowerHandler(DeyeSunHybridImpl ess, DeyeSunBattery battery, DeyeDcCharger dcCharger) {
		this.ess = ess;
		this.battery = battery;
		this.dcCharger = dcCharger;
		this.log = this.ess.getLogger();
	}

	public void apply(int activePowerTarget, int reactivePower, int configuredMaxApparentPower)
			throws OpenemsNamedException {

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
		//Integer powerAcGrid = ess.getGridOutPower().get();
		Integer activePower = ess.getActivePower().orElse(0);
		Integer dcDischargePower = ess.getDcDischargePower().orElse(0);
		int pvPower = (dcCharger != null) ? dcCharger.getActualPower().orElse(0) : 0;
		Integer batteryVoltageRaw = battery.getBatteryVoltage().get();
		Integer maxAllowedChargePower = ess.getAllowedChargePower().get();
		Integer maxAllowedDischargePower = ess.getAllowedDischargePower().get();
		
		// calculation of target power
		activePowerTarget = activePowerTarget - pvPower;
		
		this.targetPowerAvg.addValue(activePowerTarget);

		if (Stream.of(batteryVoltageRaw, batteryPower, maxAllowedChargePower, maxAllowedDischargePower)
				.anyMatch(Objects::isNull)) {
			return;
		}
		if (maxApparentPower == null) {
			ess.logDebug(log, "Max Apparent power not available. Skipping ApplyPower");
			return;
		}
		
		this.writeFlags();
		this.cycleCounter ++;
		
		int powerDeciPercent = calculateDeciPercentFromPower(maxApparentPower, activePowerTarget);
		int averageTargetPower = this.targetPowerAvg.getAverage();
		
		
		if (( Math.abs( averageTargetPower - activePowerTarget) > 100) || this.cycleCounter > 3) {

			this.cycleCounter = 0;
			
		this.ess.setSetRemoteMode(1); // reg 1100 0->disable, 1->enable
		this.ess.setSetRemoteWatchdogTime(600); // reg 1101 Watchdog
		
		this.ess.setFuckOff1(0); // 1102
		this.ess.setFuckOff2(0); // 1103
		
		this.ess.setSetControlMode(1); // reg 1104 // set 1 for battery control (DC); set 0 for AC-control
		this.ess.setSetBatteryControlMode(2); // reg 1105 // set 2 for for percentage control (reg 1109); set 3 for SOC control (reg 1110)
		this.ess.setSet3PControlMode(0);  // reg 1106 set 0 for 3p control via reg. 1111; set 1 for control each phase individually
		
		this.ess.setBatteryConstantVoltage(0); // 1107
		this.ess.setBatteryConstantCurrent(0); // 1108
		
		this.ess.setSetBatteryPowerPercent(powerDeciPercent); // 1109 
		this.ess.setSetBatteryPowerSoc(0); // 1110
		
		
		this.ess.setSetAcSetpoint3pPercent(0); // reg 1111. Negative values -> Charge
		


		} 		
		// convert for debugging
		int powerPercent = (int) Math.round((double)powerDeciPercent / 10.0);
		
 		ess.logDebug(log, "\n-> AC target: " + activePowerTarget + " avg Target:" + averageTargetPower
 				+ "(" + powerPercent +" /10%) " 
				+ "\n   PV: " + pvPower
				+ "\n   DcDisCharge: " + dcDischargePower 				
				+ "\n   ActivePower: " + activePower + " | Grid (Deye AC In): ToDo" 
				+ "\n   Mode: " + lastMode
				+ "\n | EnergyManagementModel: " + this.ess.getEnergyManagementModel()
				+ "\n | LimitControlFunction: " + this.ess.getLimitControlFunction()
				+ "\n | SolarSellMode: " + this.ess.getSolarSellMode()
				+ "\n | GridChargeEnabled: " + this.ess.getGridCharingEnabled());		
	}

	// ========================= Helper =========================

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
		if (this.ess.getGeneratorCharingEnabled() != false) {
			this.ess.setGeneratorCharingEnabled(false);
		}

		// 130: Grid Charging an
		if (this.ess.getGridCharingEnabled() != true) {
			this.ess.setGridCharingEnabled(true);
		}

		// 141: Load first
		if (this.ess.getEnergyManagementModel() != EnergyManagementModel.LOAD_FIRST) {
			this.ess.setEnergyManagementModel(EnergyManagementModel.LOAD_FIRST);
		}

		// 142: Selling Active
		if (this.ess.getLimitControlFunction() != LimitControlFunction.SELLING_ACTIVE) {
			this.ess.setLimitControlFunction(LimitControlFunction.SELLING_ACTIVE);
		}

		// 145: PV-Selling aktiv
		if (this.ess.getSolarSellMode() != EnableDisable.ENABLED) {
			this.ess.setSolarSellMode(EnableDisable.ENABLED);
		}

		// 166: 100 % SOC
		if (this.ess.getSellModeTimePoint1Capacity().get() != 100) {
			this.ess.setSellModeTimePoint1Capacity(100);
		}

		// 172: Charge Mode Zeitpunkt 1 = 1
		if (this.ess.getChargeModeTimePoint1().get() != 1) {
			this.ess.setChargeModeTimePoint1(1);
		}
		
/*		
		if (battery.getConfigurableChargeCurrentLimit().get() != MAX_A ) {
			battery.setBmsMaxChargeCurrent(MAX_A);  // 108	
		}
		
		if (battery.getBmsDischargeCurrentLimit().get() != MAX_A ) {
			battery.setBmsMaxDischargeCurrent(MAX_A); // 109
		}		
		
		if (this.ess.getGridChargeCurrent().get() != MAX_A) {
			this.ess.setGridChargeCurrent(MAX_A);   // 128	
		}
*/

	}



}
