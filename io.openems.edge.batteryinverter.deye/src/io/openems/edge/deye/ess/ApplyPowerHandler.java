package io.openems.edge.deye.ess;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.common.channel.IntegerWriteChannel;
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
	private final AverageCalculator batteryTargetPowerAvg = new AverageCalculator(5);

	// === Last written values ===
	private int sellPSetLast = 0;
	private int iSetLast = 0;

	// Timers
	private long last109Ms = 0, last108Ms = 0, last128Ms = 0, last154Ms = 0, lastSwitchModesMs = 0;
	private long lastKeepAliveMs = 0;

	// after cleaning
	private static final long KEEPALIVE_MS = 12000;

	private static final int PV_FF_MAX_STEP_W = 600; // PV-Feedforward-Glättung
	private static final int DC_MODE_HYST_W = 100; // Mode-Hysterese

	// ACK-Ramp
	private static final int STEP_W = 200; // step size W
	private static final int ACK_TOL_W = 180; // tolerance W
	private static final int ACK_STABLE_N = 1; // Treffer in Folge
	private static final long ACK_TIMEOUT_MS = 3000; // Sicherheits-Timeout

	// Register-Pacing
	private static final long REG_COOLDOWN_MS = 1000; // 108/109/128
	private static final long REG154_COOLDOWN_MS = 300; // 154 braucht länger
	private long flagsSettingUntilMs = 0;

	// Pv correction
	private int lastUsedPvAc = 0;

	// deadBand variables
	private static final long DEAD_FLAGS_REFRESH_MS = 4000; // alle x Sekunden Flags im Deadband reasserten
	private boolean inDeadBand = false;
	private long lastDeadFlagsMs = 0;
	private final int DEADBAND_HYST = 100;

	// === ACK-Ramp (minimal-invasiv) ===
	private int desiredDcW = 0; // „Ziel“ aus deiner Regelung
	private int stepTargetDcW = 0; // nächster Freigabe-Step
	private int cmdDcW = 0; // was wir tatsächlich kommandieren
	private int okSeq = 0;
	private long stepStartMs = 0;

	private static final int MIN_A = 5;

	// === Mode ===
	private enum FlowMode {
		CHARGE, DISCHARGE
	} // ignore IDLE at this point

	private FlowMode lastMode = null;
	//private boolean flagsFirstStage = true;

	// === Model constants ===
	private static final double ETA_C = 0.92; // AC->DC while charging
	private static final double ETA_D = 0.94; // DC->AC while discharging
	private static final int P_IDLE = 120; // self consumption

	// Headroom: weil 108/109 Max-Limits sind und Deye „drunter“ bleibt
	private static final double HDR_DIS = 1.12; // +12% bei Entladung
	private static final double HDR_CHG = 1.08; // +8% bei Ladung

	// Vbat-Filter für stabile Ampere
	private static final double VBAT_ALPHA = 0.25; // EMA
	private double vbatEma = 52.0;

	private static final double FB_GAIN = 0.20; // was 0.30

	public ApplyPowerHandler(DeyeSunHybridImpl ess, DeyeSunBattery battery, DeyeDcCharger dcCharger) {
		this.ess = ess;
		this.battery = battery;
		this.dcCharger = dcCharger;
		this.log = this.ess.getLogger();
	}

	public void apply(int activePowerTarget, int reactivePower, int configuredMaxApparentPower, int deadBand)
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
		// cannot be negative
		deadBand = Math.max(0, deadBand);

		// --- Read inputs ---
		Integer maxApparentPower = ess.getMaxApparentPower().get();
		Integer batteryPower = battery.getDcPower().get();
		Integer powerAcGrid = ess.getGridOutPower().get();
		Integer activePower = ess.getActivePower().orElse(0);
		Integer dcDischargePower = ess.getDcDischargePower().orElse(0);
		int pvPower = (dcCharger != null) ? dcCharger.getActualPower().orElse(0) : 0;
		Integer batteryVoltageRaw = battery.getBatteryVoltage().get();
		Integer maxAllowedChargePower = ess.getAllowedChargePower().get();
		Integer maxAllowedDischargePower = ess.getAllowedDischargePower().get();
		Integer soc = ess.getSoc().orElse(50); // 50: conservative guessing

		if (Stream.of(batteryVoltageRaw, batteryPower, powerAcGrid, maxAllowedChargePower, maxAllowedDischargePower)
				.anyMatch(Objects::isNull))
			return;
		if (maxApparentPower == null) {
			ess.logDebug(log, "Max Apparent power not available. Skipping ApplyPower");
			return;
		}

		// --- Feed-forward / feedback ---
		// int usedPvPower = activePower - dcDischargePower; // AC power NOT from
		// battery -> PV
		int usedPvPower = safeUsedPvAc(activePower, dcDischargePower);

		int ffAc = activePowerTarget - usedPvPower; // desired AC target from battery

		int targetPowerDc;
		if (ffAc >= 0)
			targetPowerDc = (int) Math.ceil(ffAc / ETA_D); // discharging desired
		else
			targetPowerDc = (int) Math.floor(ffAc / ETA_C) - P_IDLE; // charging desired

		int essErrorAc = activePowerTarget - activePower;
		int fbDc = (essErrorAc >= 0) ? (int) Math.ceil(essErrorAc / ETA_D) : (int) Math.floor(essErrorAc / ETA_C);
		targetPowerDc += (int) Math.round(FB_GAIN * fbDc); // corrected DC target

		// apply Limits
		if (targetPowerDc >= 0)
			targetPowerDc = Math.min(targetPowerDc, maxAllowedDischargePower);
		else
			targetPowerDc = Math.max(targetPowerDc, maxAllowedChargePower);

		// floating average
		batteryTargetPowerAvg.addValue(targetPowerDc);
		targetPowerDc = batteryTargetPowerAvg.getAverage();

		// ramp to avoid quick changes
		int actualBatteryPower = batteryPower; // ist oben bereits non-null gecheckt
		int cmdDc = advanceAckRamp(targetPowerDc, actualBatteryPower);

		// --- Current setpoint from Vbat ---
		double batteryVoltage = (batteryVoltageRaw != null ? batteryVoltageRaw : 52000) / 1000.0; // mV -> V

		// Headroom je nach Richtung (weil 108/109 Max-Limits sind)
		double hdr = (cmdDc >= 0) ? HDR_DIS : HDR_CHG;

		// |P| -> A, mit Headroom; nie 0 A
		double calculatedCurrentSetpointAbsolute = (batteryVoltage > 0) ? (Math.abs(cmdDc) * hdr) / batteryVoltage : 0.0;

		int currentSetpointAbsolute = (int) Math.max(MIN_A, Math.min(100, Math.ceil(calculatedCurrentSetpointAbsolute))); // cap ggf. auf HW-Max
		currentSetpointAbsolute = Math.max(5, Math.min(100, Math.abs(currentSetpointAbsolute))); // never 0

		int sellAc = (int) Math.round(Math.max(0, cmdDc) * ETA_D);
		int sellPSetQuant = (cmdDc >= 0) ? quantize50floor(Math.max(0, Math.min(8000, sellAc)))
				: quantize50(Math.max(0, Math.min(8000, sellAc)));
		
		boolean batteryFull = (soc != null && soc >= 100);		

		int sellCapMax = quantize50floor(Math.min(8000, (maxApparentPower != null ? maxApparentPower : 8000)));
		boolean deadbandEnter = Math.abs(ffAc) < deadBand;
		boolean deadbandStay  = inDeadBand && Math.abs(ffAc) <= (deadBand + DEADBAND_HYST);	// leave deadband if hysteresis is over

		// overrule
		deadbandEnter = false;
		deadbandStay = false;
		
		if (deadbandEnter || deadbandStay) {
		    // cyclic refreshing of deadband flags
		    if ((nowMs() - lastDeadFlagsMs) >= DEAD_FLAGS_REFRESH_MS || !inDeadBand) {
		        writeDeadbandFlags();           // setzt 141/142/145/166/172 passend
		        lastDeadFlagsMs = nowMs();
		    }

		    // SoC = 100%: 154 kurz auf Cap anheben, um PV-Drossel zu vermeiden
		    if (batteryFull) {
		        boolean due154 = due(last154Ms, REG154_COOLDOWN_MS) || keepAliveDue();
		        if (sellPSetLast != sellCapMax && due154) {
		            this.ess.setSellModeTimePoint1Power(sellCapMax);
		            sellPSetLast = sellCapMax;
		            last154Ms = nowMs();
		        }
		    }

		    // Keep-alive: BMS-Limits nie 0 A werden lassen
		    if (battery.getBmsMaxChargeCurrent().get() == null || battery.getBmsMaxChargeCurrent().get() < MIN_A)
		        battery.setBmsMaxChargeCurrent(MIN_A);
		    if (battery.getBmsMaxDischargeCurrent().get() == null || battery.getBmsMaxDischargeCurrent().get() < MIN_A)
		        battery.setBmsMaxDischargeCurrent(MIN_A);

		    // Zustand merken und transparent machen
		    inDeadBand = true;
		    ess.logDebug(log, "DeadBand aktiv: interne Eigenverbrauchsregelung (< " +  this.ess.getDeadBand() + " W)");

		    // Optional nur für Transparenz im UI (wirken HW-seitig nicht):
		    this.ess.setTargetActivePower(0);
		    this.ess.setTargetCurrent(0);

		    // WICHTIG: Im Deadband KEINE 108/109/128/154-Regel-Schreibs (außer o.g.) -> früh raus
		    return;
		} else if (inDeadBand) {
		    // Wechsel raus aus Deadband -> Normalbetrieb
		    inDeadBand = false;
		    // ggf. sofortiger Re-Assert deiner SELLING_ACTIVE/LOAD_FIRST-Flags von außen erlaubt
		    flagsSettingUntilMs = 0;
		}

		int sign = (cmdDc > DC_MODE_HYST_W) ? 1 : (cmdDc < -DC_MODE_HYST_W) ? -1 : 0;
		FlowMode desiredMode = (sign > 0) ? FlowMode.DISCHARGE : (sign < 0) ? FlowMode.CHARGE : lastMode;

		// FlowMode desiredMode = (sign > 0) ? FlowMode.DISCHARGE : (sign < 0 ?
		// FlowMode.CHARGE : lastMode);
		// detect mode change (including initial non-null)
		boolean modeChanged = (desiredMode != null && desiredMode != lastMode);
		
		if (lastMode == null && desiredMode != null) {
			lastMode = desiredMode;
		}
		// force fresh current write after mode change (avoid delay from keepalive)
		if (modeChanged) {
			lastMode=desiredMode;
			iSetLast = -1;
			long n = nowMs();
			last108Ms = n - REG_COOLDOWN_MS;
			last109Ms = n - REG_COOLDOWN_MS;
			last128Ms = n - REG_COOLDOWN_MS;
			last154Ms = n - REG154_COOLDOWN_MS;

			lastSwitchModesMs = n - REG_COOLDOWN_MS;
			
			if (desiredMode == FlowMode.DISCHARGE) {
				battery.setBmsMaxChargeCurrent(MIN_A);
				this.ess.setGridChargeCurrent(MIN_A);
				last108Ms = nowMs();
				last128Ms = nowMs();
			} else {
				battery.setBmsMaxDischargeCurrent(MIN_A);
				last109Ms = nowMs();
			}
		}

		// long before108 = last108Ms, before109 = last109Ms, before128 = last128Ms,
		// before154 = last154Ms;

		// --- timers ---
		boolean due108 = due(last108Ms, (REG_COOLDOWN_MS * 3));
		boolean due109 = due(last109Ms, (REG_COOLDOWN_MS * 4));
		boolean due128 = due(last128Ms, REG_COOLDOWN_MS * 5);
		boolean due154 = due(last154Ms, REG_COOLDOWN_MS * 4);
		
		boolean switchModes = due(lastSwitchModesMs, REG_COOLDOWN_MS * 5);
		
		boolean force = keepAliveDue();
		
		if (switchModes) {
			writeFlagsFor(lastMode, false);	
		}
		if (force) {
			writeFlagsFor(lastMode, true);	
		}
			
		// Regs 108,109,128
		writeCurrentsFor(lastMode, currentSetpointAbsolute, due108, due109, due128, force);

		writeSellPowerFor(lastMode, sellPSetQuant, due154 || force, batteryFull, sellCapMax);

		// write to channels
		this.ess.setTargetActivePower(targetPowerDc);
		this.ess.setTargetCurrent(currentSetpointAbsolute);

		ess.logDebug(log, "\n-> AC target: " + activePowerTarget + "\n   PV: " + pvPower + " | AC.ff: " + ffAc
				+ "\n   ActivePower: " + activePower + " | Grid (Deye AC In): " + powerAcGrid + "\n   DC Discharge: "
				+ dcDischargePower + "[W]  DC target: " + targetPowerDc + "[W] | Iset(A): " + currentSetpointAbsolute + "[A]"
				+ "\n   154: " + sellPSetLast + "[W]" + "\n   Mode: " + lastMode + "\n | EnergyManagementModel: "
				+ this.ess.getEnergyManagementModel() + "\n | LimitControlFunction: "
				+ this.ess.getLimitControlFunction() + "\n | SolarSellMode: " + this.ess.getSolarSellMode()
				+ "\n | GridChargeEnabled: " + this.ess.getGridCharingEnabled() + "\nACKRamp desired=" + desiredDcW
				+ " stepTarget=" + stepTargetDcW + " cmdDc=" + cmdDc + " pbat=" + actualBatteryPower + " okSeq=" + okSeq);

	}

	// ========================= Helper =========================

	private void writeFlagsFor(FlowMode mode, boolean secondStage) throws OpenemsNamedException {
		lastSwitchModesMs = nowMs();
		if (mode == FlowMode.DISCHARGE) {
			
			// secondStage Registers only need to be set if keepalive/force is engaged
			
			// 129
			if (secondStage == true && this.ess.getGeneratorCharingEnabled() != false) {
				this.ess.setGeneratorCharingEnabled(false); // changed = true; }
			}
			
			// 130
			if (this.ess.getGridCharingEnabled() != false) {
				this.ess.setGridCharingEnabled(false); //
			}

			// 141
			if (secondStage == true && this.ess.getEnergyManagementModel() != EnergyManagementModel.LOAD_FIRST) {
				this.ess.setEnergyManagementModel(EnergyManagementModel.LOAD_FIRST);
			}

			// 142
			if (secondStage == true && this.ess.getLimitControlFunction() != LimitControlFunction.SELLING_ACTIVE) {
				this.ess.setLimitControlFunction(LimitControlFunction.SELLING_ACTIVE);
			}

			// 145
			if (this.ess.getSolarSellMode() != EnableDisable.DISABLED) {
				this.ess.setSolarSellMode(EnableDisable.DISABLED);
			}

			// 166
			if (this.ess.getSellModeTimePoint1Capacity().get() != 5) {
				this.ess.setSellModeTimePoint1Capacity(5);
			}

			// 172
			if (this.ess.getChargeModeTimePoint1().get() != 0) {
				this.ess.setChargeModeTimePoint1(0);
			}

		} else { // CHARGE

			// 129
			if (secondStage == true && this.ess.getGeneratorCharingEnabled() != false) {
				this.ess.setGeneratorCharingEnabled(false);
			}

			// 130
			if (this.ess.getGridCharingEnabled() != true) {
				this.ess.setGridCharingEnabled(true);
			}

			// 141
			if (secondStage == true && this.ess.getEnergyManagementModel() != EnergyManagementModel.LOAD_FIRST) {
				this.ess.setEnergyManagementModel(EnergyManagementModel.LOAD_FIRST);
			}

			// 142
			if (secondStage == true && this.ess.getLimitControlFunction() != LimitControlFunction.SELLING_ACTIVE) {
				this.ess.setLimitControlFunction(LimitControlFunction.SELLING_ACTIVE);
			}

			// 145
			if (this.ess.getSolarSellMode() != EnableDisable.ENABLED) {
				this.ess.setSolarSellMode(EnableDisable.ENABLED);
			}

			// 166
			if (this.ess.getSellModeTimePoint1Capacity().get() != 100) {
				this.ess.setSellModeTimePoint1Capacity(100);
			}

			// 172
			if (this.ess.getChargeModeTimePoint1().get() != 1) {
				this.ess.setChargeModeTimePoint1(1);
			}
		}
	}

	private void writeCurrentsFor(FlowMode mode, int iSetAbs, boolean due108, boolean due109, boolean due128,
			boolean force) throws OpenemsNamedException {

		if (mode == FlowMode.DISCHARGE) {
			boolean changed = (iSetLast != iSetAbs);

			// 109
			// Write both values in every cycle
			if (force || (changed && due109)) {
				// battery.setBmsMaxChargeCurrent(MIN_A); // 108
				battery.setBmsMaxDischargeCurrent(iSetAbs); // 109
				last109Ms = nowMs();
				iSetLast = iSetAbs;
				ess.logDebug(log, "\n\n\n|-> reg109 " + iSetAbs + "\n");
			}

		} else { // CHARGE
			boolean changed = (iSetLast != iSetAbs);

			// 108 then 128 in the same cycle if due
			if (force || (changed && due108)) {
				battery.setBmsMaxChargeCurrent(iSetAbs); // 108
				// battery.setBmsMaxDischargeCurrent(5); // 109
				last108Ms = nowMs();
				iSetLast = iSetAbs;
				ess.logDebug(log, "\n\n\n|-> reg108 " + iSetAbs + "\n");
			}
		}
	}

	private void writeSellPowerFor(FlowMode mode, int sellPSetQuant, boolean due154, boolean batteryFull,
			int sellCapMax) throws OpenemsNamedException {
		if (mode == FlowMode.DISCHARGE) {
			if (sellPSetQuant != sellPSetLast && due154) {
				this.ess.setSellModeTimePoint1Power(8000); // 154
				sellPSetLast = sellPSetQuant;
				last154Ms = nowMs();
			}
		} else { // CHARGE
			int desired = batteryFull ? sellCapMax : 0;
			if (sellPSetLast != desired && due154) {
				this.ess.setSellModeTimePoint1Power(desired); // 154 -> 0 only higher value if soc = 100
				sellPSetLast = desired;
				last154Ms = nowMs();
			}
		}
	}

	// ===== misc =====
	private boolean keepAliveDue() {
		long now = nowMs();
		if (now - lastKeepAliveMs >= KEEPALIVE_MS) {
			lastKeepAliveMs = now;
			return true;
		}
		return false;
	}

	private static boolean due(long lastMs, long gap) {
		return (nowMs() - lastMs) >= gap;
	}

	private static long nowMs() {
		return System.currentTimeMillis();
	}

	private static int quantize50(int w) {
		return ((w + 25) / 50) * 50;
	}

	private static int quantize50floor(int w) {
		return (w / 50) * 50;
	}

	// ===== (unchanged) =====
	private int applyBatteryLimits(int requestedPower) {
		if (battery == null)
			return requestedPower;
		Integer allowedChargePower = ess.getAllowedChargePower().get();
		Integer allowedDischargePower = ess.getAllowedDischargePower().get();
		if (allowedChargePower == null || allowedDischargePower == null)
			return requestedPower;
		if (requestedPower < 0)
			return (int) Math.max(requestedPower, -allowedChargePower);
		else
			return (int) Math.min(requestedPower, allowedDischargePower);
	}

	protected void calculateMaxAcPower(int maxApparentPower) {
		if (this.battery == null || this.ess == null || this.dcCharger == null)
			return;
		Integer allowedChargePower = ess.getAllowedChargePower().orElse(0);
		Integer allowedDischargePower = ess.getAllowedDischargePower().orElse(0);
		int pvProduction = TypeUtils.max(0, dcCharger.getActualPower().orElse(0));
		long maxAcImport = allowedChargePower - Math.min(allowedChargePower, pvProduction);
		long maxAcExport = allowedDischargePower + pvProduction;
		maxAcImport = Math.min(maxAcImport, maxApparentPower);
		maxAcExport = Math.min(maxAcExport, maxApparentPower);
		this.ess._setMaxAcImport((int) -maxAcImport);
		this.ess._setMaxAcExport((int) maxAcExport);
	}

	private void setActivePower(int activePowerWatt) throws OpenemsNamedException {
		var maxApparentPower = this.ess.getMaxApparentPower().get();
		if (maxApparentPower == null) {
			this.ess.logDebug(log, "Max Apparent Power is not available.");
			return;
		}
		int activePowerPercent = (int) ((double) activePowerWatt / maxApparentPower * 100);
		activePowerPercent = Math.max(0, Math.min(100, activePowerPercent));
		IntegerWriteChannel ch = this.ess.channel(DeyeSunHybrid.ChannelId.ACTIVE_POWER_REGULATION);
		ch.setNextWriteValue(activePowerPercent);
		this.ess.logDebug(log, "Set Active Power: " + activePowerWatt + " W (" + activePowerPercent + "%)");
	}

	private void writeDeadbandFlags() throws OpenemsNamedException {
		// use internal self consumption functions
		if (this.ess.getEnergyManagementModel() != EnergyManagementModel.LOAD_FIRST)
			this.ess.setEnergyManagementModel(EnergyManagementModel.LOAD_FIRST);
		
		if (this.ess.getLimitControlFunction() != LimitControlFunction.BUILT_IN)
			this.ess.setLimitControlFunction(LimitControlFunction.BUILT_IN);

		// Enable PV Selling
		if (this.ess.getSolarSellMode() != EnableDisable.ENABLED)
			this.ess.setSolarSellMode(EnableDisable.ENABLED);

		// charge to 100%
		// 166
		if (this.ess.getSellModeTimePoint1Capacity().get() != 100)
			this.ess.setSellModeTimePoint1Capacity(100);
		// 172
		if (this.ess.getChargeModeTimePoint1().get() != 0)
			this.ess.setChargeModeTimePoint1(0);
	}

	private int safeUsedPvAc(int activePower, int dcDischargePower) {
		int battDischargeAc = Math.max(0, dcDischargePower);
		int pvAc = Math.max(0, activePower - battDischargeAc);

		if (lastUsedPvAc == 0) { // set initial value
			lastUsedPvAc = pvAc;
			return pvAc;
		}
		int delta = pvAc - lastUsedPvAc;
		if (Math.abs(delta) > PV_FF_MAX_STEP_W) {
			pvAc = lastUsedPvAc + (delta > 0 ? PV_FF_MAX_STEP_W : -PV_FF_MAX_STEP_W);
		}
		lastUsedPvAc = pvAc;
		return pvAc;
	}

	private static boolean nearDirected(int target, int pbat, int tol) {
		return Math.abs(pbat - target) <= tol;
	}

	private int advanceAckRamp(int desired, int pbatNow) {
		// Hysterese um 0 W für Richtungslogik
		int signDesired = (desired > DC_MODE_HYST_W) ? 1 : (desired < -DC_MODE_HYST_W) ? -1 : 0;
		int signPrev = (this.desiredDcW > DC_MODE_HYST_W) ? 1 : (this.desiredDcW < -DC_MODE_HYST_W) ? -1 : 0;

		if (signDesired != signPrev) {
			int jump = Math.min(Math.abs(desired), 400); // first step size across zero (W)
			this.stepTargetDcW = (signDesired >= 0 ? +1 : -1) * jump;
			this.cmdDcW = this.stepTargetDcW;
			this.okSeq = 0;
			this.stepStartMs = nowMs();
		}
		this.desiredDcW = desired;

		// Step erreicht? (gerichtet + stabil)
		if (nearDirected(stepTargetDcW, pbatNow, ACK_TOL_W)) {
			if (++okSeq >= ACK_STABLE_N) {
				if (stepTargetDcW != desiredDcW) {
					int dir = Integer.compare(desiredDcW, stepTargetDcW);
					stepTargetDcW += dir * STEP_W;
					if ((dir > 0 && stepTargetDcW > desiredDcW) || (dir < 0 && stepTargetDcW < desiredDcW)) {
						stepTargetDcW = desiredDcW;
					}
					okSeq = 0;
					stepStartMs = nowMs();
				}
			}
		} else {
			okSeq = 0;
			// Sicherheits-Timeout → trotzdem weiter in Richtung Ziel
			if (nowMs() - stepStartMs >= ACK_TIMEOUT_MS) {
				int dir = Integer.compare(desiredDcW, stepTargetDcW);
				stepTargetDcW += dir * STEP_W;
				if ((dir > 0 && stepTargetDcW > desiredDcW) || (dir < 0 && stepTargetDcW < desiredDcW)) {
					stepTargetDcW = desiredDcW;
				}
				stepStartMs = nowMs();
			}
		}

		cmdDcW = stepTargetDcW;
		return cmdDcW;
	}


}
