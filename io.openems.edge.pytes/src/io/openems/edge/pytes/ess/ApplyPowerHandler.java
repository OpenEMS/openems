package io.openems.edge.pytes.ess;

import org.slf4j.Logger;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.pytes.battery.PytesBattery;
import io.openems.edge.pytes.dccharger.PytesDcCharger;
import io.openems.edge.pytes.enums.EnableDisable;
import io.openems.edge.pytes.enums.RemoteDispatchRealtimeControlSwitch;
import io.openems.edge.pytes.enums.RemoteDispatchSystemLimitSwitch;
import io.openems.edge.pytes.enums.WorkState;

public class ApplyPowerHandler {

	// === Dependencies ===
	private final ApplyPowerEss ess;
	private final PytesBattery battery;
	private final PytesDcCharger dcCharger;
	private final Logger log;

	// === Feed-forward ===
	// Measured 2026-09-16 on the live system: the inverter applies a constant
	// bias of ~190-220 W towards charging to the commanded battery power (500 W
	// discharge commanded -> 300 W delivered; 200 W charge commanded -> 418 W
	// delivered), and conversion losses between battery and AC side that grow
	// with the total inverter throughput (battery + PV): ~50 W @ 0.8 kW,
	// ~160 W @ 4.2 kW, ~210 W @ 6.5 kW. Both are compensated up-front so a new
	// set-point is right within the inverter's own dead time (~10 s). The bias
	// does not apply at 0 W. The model is kept slightly conservative; the trim
	// covers the rest. Shared with AllowedChargeDischargeHandler so the limits
	// reported to the solver are what actually arrives on the AC side.
	static final int BIAS_W = 190;
	static final int LOSS_BASE_W = 30;
	static final double LOSS_FACTOR = 0.03; // of |battery| + PV
	private static final int MIN_TARGET_W = 50; // below this the inverter is treated as idle

	// === Setpoint trim ===
	// A slow integral correction on the AC-side battery contribution
	// (ActivePower - PV, which is what Sum, UI and all OpenEMS controllers use)
	// removes what the feed-forward model does not cover. The residual differs
	// by direction, so charge and discharge keep their own trim and a sign
	// change needs no re-settling. All time constants are in milliseconds and
	// scaled with the configured cycle time.
	private static final double TRIM_GAIN_PER_S = 0.04; // -> ~25 s time constant
	private static final int TRIM_LIMIT = 300; // W, anti-windup
	private static final int TRIM_WARMUP_MS = 30_000; // BMS values are unreliable right after start
	private static final int TRIM_FREEZE_MS = 12_000; // no integration while the inverter follows a step
	private static final int TRIM_FREEZE_STEP_W = 100; // step size that triggers the freeze
	private double trimDischarge = 0;
	private double trimCharge = 0;
	private long elapsedMs = 0;
	private long freezeUntilMs = 0;
	private Integer lastInverterTarget = null;

	/**
	 * Expected conversion losses between battery and AC side.
	 *
	 * @param batteryPower battery power in W (sign irrelevant)
	 * @param pvPower      PV power in W
	 * @return losses in W
	 */
	static int expectedLosses(int batteryPower, int pvPower) {
		return LOSS_BASE_W + (int) Math.round(LOSS_FACTOR * (Math.abs(batteryPower) + Math.max(0, pvPower)));
	}

	private final PvSurplusProbe surplusProbe = new PvSurplusProbe();

	public ApplyPowerHandler(ApplyPowerEss ess, PytesBattery battery, PytesDcCharger dcCharger) {
		this.ess = ess;
		this.battery = battery;
		this.dcCharger = dcCharger;
		this.log = ess.getLogger();
	}

	/**
	 * Applies the given power setpoint to the ESS via remote dispatch.
	 *
	 * @param activePowerTarget         the target active power in W
	 * @param reactivePower             the target reactive power in var
	 * @param configuredMaxApparentPower the configured maximum apparent power in VA
	 * @param essSetpoint               the remote dispatch control mode to apply
	 * @throws OpenemsNamedException on error
	 */
	public void apply(int activePowerTarget, int reactivePower, int configuredMaxApparentPower, RemoteDispatchRealtimeControlSwitch essSetpoint)
			throws OpenemsNamedException {

		// --- Guards ---
		if (!this.ess.isManaged()) {
			this.log.debug("[ApplyPower] ReadOnly Mode enabled. Skip ApplyPower");
			return;
		}
		
		// WARNING (derating, fan, limit mismatch, ...) is informational: the
		// inverter keeps running, so the EMS keeps controlling it. Only ERROR
		// (a Level.FAULT channel), STANDBY and the start-up states stop writing.
		var workState = this.ess.getWorkState();
		if (workState != WorkState.NORMAL && workState != WorkState.WARNING) {
			this.log.debug("ESS not in normal mode. Skipping ApplyPower");
			return;
		}
		Integer maxAllowedChargePower = this.ess.getAllowedChargePower().get();
		Integer maxAllowedDischargePower = this.ess.getAllowedDischargePower().get(); // includes PV

		Integer maxApparentPower = this.ess.getMaxApparentPower().get();

		if (maxApparentPower == null) {
			this.log.debug("[ApplyPower] maxApparentPower is null. Skipping ApplyPower");
			return;
		}

		if (maxAllowedChargePower == null) {
			this.log.debug("[ApplyPower] maxAllowedChargePower is null. Skipping ApplyPower");
			return;
		}

		if (maxAllowedDischargePower == null) {
			this.log.debug("[ApplyPower] maxAllowedDischargePower is null. Skipping ApplyPower");
			return;
		}

		Integer batteryPower = this.battery.getDcDischargePower().get();

		if (batteryPower == null) {
			this.log.debug("[ApplyPower] batteryPower is null. Skipping ApplyPower");
			return;
		}

		Integer essActivePower = this.ess.getActivePower().get();

		if (essActivePower == null) {
			this.log.debug("[ApplyPower] essActivePower is null. Skipping ApplyPower");
			return;
		}

		Integer essDcDischargePower = this.ess.getDcDischargePower().get();

		if (essDcDischargePower == null) {
			this.log.debug("[ApplyPower] essDcDischargePower is null. Skipping ApplyPower");
			return;
		}


		// guards for AC
		maxApparentPower = Math.min(maxApparentPower, configuredMaxApparentPower);
		if (activePowerTarget > 0) { // discharging
			activePowerTarget = Math.min(activePowerTarget, maxApparentPower);
		} else {
			activePowerTarget = Math.max(activePowerTarget, -maxApparentPower);
		}

		int pvPower = this.dcCharger != null ? this.dcCharger.getActualPower().orElse(0) : 0; // Maybe no pv connected
		final boolean batteryControl = essSetpoint == RemoteDispatchRealtimeControlSwitch.BATTERY_CONTROL;

		// Time base, scaled with the configured cycle time
		int cycleTimeMs = this.ess.getCycleTime();
		this.elapsedMs += cycleTimeMs;

		// The controlled quantity (positive = discharge / export):
		// - Battery control (44105 = 2): the AC-side battery contribution
		//   ActivePower - PV. The inverter gets a battery set-point, so its bias
		//   and the conversion losses are compensated up-front. Clamp: raw BMS
		//   limits.
		// - AC output control (44105 = 4): the inverter's AC output ActivePower. The
		//   inverter splits PV/battery itself and covers its own losses, so no
		//   feed-forward. Clamp: the AC-side range reported to the solver.
		final int target;
		final int measured;
		final int upperLimit;
		final int lowerLimit;
		final int sign;
		final int surplusFloor;
		if (batteryControl) {
			target = activePowerTarget - pvPower;
			measured = essActivePower - pvPower;
			upperLimit = Math.max(0, this.ess.getBatteryDischargeLimit());
			lowerLimit = Math.min(0, this.ess.getBatteryChargeLimit());
			sign = -1; // reg 44106: negative = battery discharge
			surplusFloor = 0;
		} else {
			target = activePowerTarget;
			measured = essActivePower;
			// The inverter enforces the AC set-point regardless of the battery, so
			// the DC limits have to be translated here: AC = battery + PV. The lower
			// bound can be positive (PV above the charge limit must be exported);
			// OpenEMS itself only knows that bound via getSurplusPower().
			upperLimit = Math.max(0, maxAllowedDischargePower);
			int clampLower = Math.max(Math.min(0, maxAllowedChargePower), pvPower + this.ess.getBatteryChargeLimit());
			// The measured PV is not the available one while the inverter curtails
			// it to our set-point, so the lower bound above alone locks the PV at
			// consumption level once the battery is full; see PvSurplusProbe.
			surplusFloor = this.surplusProbe.compute(pvPower, essActivePower, this.ess.getGridPower(), batteryPower,
					this.ess.getBatteryChargeLimit(), this.ess.getGridFeedInLimit(), maxApparentPower,
					this.ess.isPvLimitActive(), cycleTimeMs);
			lowerLimit = surplusFloor > 0 ? Math.max(clampLower, surplusFloor) : clampLower;
			sign = 1; // reg 44106: positive = export
		}
		this.ess.channel(PytesJs3.ChannelId.SURPLUS_FLOOR).setNextValue(surplusFloor);
		boolean idle = Math.abs(target) < MIN_TARGET_W;

		// Loads on the backup port: in AC output control the inverter regulates
		// its GRID-SIDE port only and supplies the backup port on top, while
		// ActivePower (and the OpenEMS set-point) is the total AC output. The
		// backup load therefore has to be taken out of the register value (seen
		// live on 2026-09-17: a 1.2 kW EV on the backup port led to 850 W grid
		// export). In battery control the total is what matters, nothing to do.
		int backupLoad = batteryControl ? 0 : Math.max(0, this.battery.getBackupLoadPower().orElse(0));

		// Feed-forward: bias and losses always act in discharge direction.
		final int feedForward = batteryControl && !idle ? BIAS_W + expectedLosses(target, pvPower) : 0;

		// Step detection on what the inverter sees (a backup load step is a step
		// for the inverter as well)
		int inverterTarget = target - backupLoad;
		if (this.lastInverterTarget != null
				&& Math.abs(inverterTarget - this.lastInverterTarget) > TRIM_FREEZE_STEP_W) {
			this.freezeUntilMs = this.elapsedMs + TRIM_FREEZE_MS;
		}
		this.lastInverterTarget = inverterTarget;

		// Integral trim on the controlled quantity. Only integrate when the
		// set-point is not sitting on a limit (anti-windup: with the current trim
		// applied), not idle, not right after a step (the inverter needs its dead
		// time first) and the measurements are plausible (BMS values are garbage
		// right after start).
		int plausibleLimit = Math.max(maxAllowedDischargePower, -maxAllowedChargePower) + 500;
		boolean plausible = Math.abs(batteryPower) <= plausibleLimit && Math.abs(measured) <= plausibleLimit;
		boolean discharging = target > 0;
		double currentTrim = discharging ? this.trimDischarge : this.trimCharge;
		int untrimmedSetPoint = target + feedForward + (int) Math.round(currentTrim);
		boolean limited = untrimmedSetPoint > upperLimit || untrimmedSetPoint < lowerLimit;
		boolean settled = this.elapsedMs > TRIM_WARMUP_MS && this.elapsedMs >= this.freezeUntilMs;
		if (!idle && !limited && settled && plausible) {
			double delta = TRIM_GAIN_PER_S * (cycleTimeMs / 1000.0) * (target - measured);
			if (discharging) {
				this.trimDischarge = Math.max(-TRIM_LIMIT, Math.min(TRIM_LIMIT, this.trimDischarge + delta));
			} else {
				this.trimCharge = Math.max(-TRIM_LIMIT, Math.min(TRIM_LIMIT, this.trimCharge + delta));
			}
		}
		double trim = idle ? 0 : discharging ? this.trimDischarge : this.trimCharge;

		// Set-point = target + feed-forward + trim, clamped to the limits
		int setPoint = Math.max(lowerLimit, Math.min(upperLimit, target + feedForward + (int) Math.round(trim)));

		this.writeExternalControlFlags();
		// Reg 44106 (1 = 10 W): with 44105 = 2 (battery control) a negative value is
		// battery discharge, positive is charge; with 44105 = 4 the inverter
		// regulates its grid-side AC port (negative = import, positive = export),
		// see backupLoad above.
		int registerValue = (int) Math.round((setPoint - backupLoad) / 10.0) * sign;
		this.ess.setRemoteDispatchRealtimeControlSwitch(essSetpoint);
		this.ess.setRemoteDispatchRealtimeControlPower(registerValue);
		
		this.ess.debugLog(""
				+ "\n[ApplyPower] Mode: " + essSetpoint + ", TargetPower: " + activePowerTarget
				+ "\n[ApplyPower] EssPower: " + essActivePower
				+ "\n[ApplyPower] Limits: " + lowerLimit + "/" + upperLimit
				+ "\n[ApplyPower] ESS DC DischargePower: " + essDcDischargePower
				+ "\n[ApplyPower] Battery hardware SetPoint: " + registerValue
				+ "\n[ApplyPower] FeedForward: " + feedForward + " W, Trim: " + Math.round(trim) + " W (target " + target
				+ " W, measured " + measured + " W, BMS " + batteryPower + " W, trimD " + Math.round(this.trimDischarge)
				+ " trimC " + Math.round(this.trimCharge) + ")"
				+ "\n[ApplyPower]   PV Power " + pvPower + ", Backup load " + backupLoad + ", Feed-in limit "
				+ this.ess.getGridFeedInLimit() + ", Surplus floor " + surplusFloor
				+ (this.surplusProbe.isProbing() ? " (probing)" : ""));


	}

	// ========================= Helper =========================

	/**
	 * Writes the remote dispatch settings that select external (EMS) control.
	 *
	 * @throws OpenemsNamedException on write error
	 */
	private void writeExternalControlFlags() throws OpenemsNamedException {
		// The remote dispatch block 44100-44108 is re-written every cycle: the
		// inverter does not persist it and the failsafe expects periodic writes.
		// All nine registers get a value so the bridge sends ONE FC16 frame;
		// registers without a value (44103/44104/44108) would split it into two.
		// 44105/44106 are set by apply() in the same cycle.
		this.ess.setRemoteDispatchSwitch(EnableDisable.ENABLE); // 44100
		this.ess.setRemoteDispatchFailsafeSetting(this.ess.getFailsafeMinutes()); // 44101
		// 44102-44104: grid feed-in hard limit as hardware backstop (see
		// PytesJs3Impl.getGridFeedInLimit()). The inverter limits the export at
		// its grid meter and curtails PV when the battery cannot take the surplus.
		Integer feedInLimit = this.ess.getGridFeedInLimit();
		if (feedInLimit != null) {
			this.ess.setRemoteDispatchSystemLimitSwitch(RemoteDispatchSystemLimitSwitch.EXPORT_LIMIT_ENABLE);
			this.ess.setRemoteDispatchSystemExportLimit(feedInLimit); // W, register in 100 W steps
		} else {
			this.ess.setRemoteDispatchSystemLimitSwitch(RemoteDispatchSystemLimitSwitch.DISABLE);
			this.ess.setRemoteDispatchSystemExportLimit(0);
		}
		this.ess.setRemoteDispatchSystemImportLimit(0); // 44103, import limit not used
		// 44108: PV on, DO off, grid charge allowed, no off-grid standby. Same value
		// the inverter reports by default. ToDo: make grid charge configurable.
		this.ess.setRemoteDispatchRealtimeControlFunctionSwitch(false, false, true, false);
	}

}
