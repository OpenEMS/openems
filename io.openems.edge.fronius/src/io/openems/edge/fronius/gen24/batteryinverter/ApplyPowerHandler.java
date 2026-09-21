package io.openems.edge.fronius.gen24.batteryinverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.fronius.enums.SetControlMode;
import io.openems.edge.fronius.gen24.battery.FroniusGen24Battery;

public class ApplyPowerHandler {

	private final Logger log = LoggerFactory.getLogger(ApplyPowerHandler.class);

	private static final int RATE_100_PERCENT = 10000;
	private static final int RATE_HYSTERESIS = 25;
	private static final long MIN_WRITE_INTERVAL_MS = 2000;
	private static final long KEEP_ALIVE_INTERVAL_MS = 30000;

	// Fronius' own watchdog defaults to 8h (28800s, the max of the register's
	// documented 0-28800s range) if InOutWRte_RvrtTms is never written - far too
	// long to wait for autonomous operation to resume after we stop actively
	// writing in INTERNAL mode. We write a short value once instead, ensuring
	// OutWRte/InWRte are reliably at 0 well before the watchdog fires.
	private static final int SHORT_REVERT_TIMEOUT_SECONDS = 15;

	// Inverter power control (Model 123)
	// WMaxLimPct expects 0..100, where 100 = 100%
	private static final int W_MAX_LIM_100_PERCENT = 100;
	private static final int W_MAX_LIM_HYSTERESIS = 1;
	private static final long W_MAX_LIM_KEEP_ALIVE_MS = 30000;

	private final BatteryInverterFroniusGen24Impl parent;

	private SetControlMode lastControlMode = null;
	private Integer lastOutWRte = null;
	private Integer lastInWRte = null;
	private long lastWriteMillis = 0L;

	// Tracks whether the one-time "force zero + short watchdog" write has
	// already happened for the current INTERNAL-mode period, so it is only
	// written once per transition, not every cycle.
	private boolean internalModeTransitionHandled = false;

	// Inverter state
	private Integer lastWMaxLimPct = null;
	private int lastWMaxLimEna = -1; // -1 = never written
	private long lastWMaxLimWriteMillis = 0L;

	public ApplyPowerHandler(BatteryInverterFroniusGen24Impl parent) {
		this.parent = parent;
	}

	/**
	 * Applies the power setpoints to the battery inverter.
	 *
	 * @param battery          the Fronius Gen24 battery
	 * @param setActivePower   the active power setpoint in W
	 * @param setReactivePower the reactive power setpoint in var
	 * @param controlmode      the control mode
	 * @throws OpenemsNamedException on error
	 */
	public synchronized void apply(FroniusGen24Battery battery, int setActivePower, int setReactivePower,
			ControlMode controlmode) throws OpenemsNamedException {

		Result result = switch (controlmode) {
		case INTERNAL -> this.handleInternalMode();
		case REMOTE -> this.handleRemoteMode(setActivePower);
		};

		this.parent._setDebugControlMode(result.controlMode());

		// Inverter power limit – always check, independent of shouldWrite
		var limitOpt = this.parent.getActivePowerLimitChannel().getNextWriteValueAndReset();
		if (limitOpt.isPresent() || this.lastWMaxLimEna == 1) {
			this.applyInverterPowerLimit(limitOpt.isPresent() ? limitOpt.get() : null);
		}

		// In INTERNAL mode: write once (StorCtlMod=CHARGE_AND_DISCHARGE_LIMIT,
		// power=0, short revert timeout) so Fronius quickly falls back to
		// autonomous operation instead of staying at the last REMOTE limit for
		// up to its 8h watchdog default. Not written every cycle - only on the
		// transition itself.
		if (result.controlMode() == SetControlMode.DISABLED) {
			if (!this.internalModeTransitionHandled) {
				this.writeInternalModeTransition(battery);
				this.internalModeTransitionHandled = true;
			}
			// Reset state so everything is re-written on the next REMOTE start
			this.lastControlMode = null;
			this.lastOutWRte = null;
			this.lastInWRte = null;
			return;
		}
		this.internalModeTransitionHandled = false;

		// Write StorCtl_Mod (40348) every cycle in REMOTE mode –
		// so Fronius knows the mode immediately after restart or connection loss
		EnumWriteChannel setControlMode = battery.channel(FroniusGen24Battery.ChannelId.SET_STORAGE_CONTROL_MODE);
		setControlMode.setNextWriteValue(result.controlMode());

		if (!this.shouldWrite(result)) {
			return;
		}

		IntegerWriteChannel setOutWRte = battery.channel(FroniusGen24Battery.ChannelId.SET_OUT_W_RTE);

		IntegerWriteChannel setInWRte = battery.channel(FroniusGen24Battery.ChannelId.SET_IN_W_RTE);

		setOutWRte.setNextWriteValue(result.outWRte());
		setInWRte.setNextWriteValue(result.inWRte());

		this.rememberWrittenResult(result);
	}

	/**
	 * Writes StorCtlMod, zero power, and a short watchdog revert timeout once, on
	 * the transition into INTERNAL mode. Without this, the Fronius keeps honoring
	 * the last REMOTE-mode limit for up to its 8h watchdog default, since nothing
	 * else is written while INTERNAL is active.
	 *
	 * @param battery the Fronius Gen24 battery
	 * @throws OpenemsNamedException on error
	 */
	private void writeInternalModeTransition(FroniusGen24Battery battery) throws OpenemsNamedException {

		EnumWriteChannel setControlMode = battery.channel(FroniusGen24Battery.ChannelId.SET_STORAGE_CONTROL_MODE);
		setControlMode.setNextWriteValue(SetControlMode.CHARGE_AND_DISCHARGE_LIMIT);

		IntegerWriteChannel setOutWRte = battery.channel(FroniusGen24Battery.ChannelId.SET_OUT_W_RTE);
		IntegerWriteChannel setInWRte = battery.channel(FroniusGen24Battery.ChannelId.SET_IN_W_RTE);
		setOutWRte.setNextWriteValue(0);
		setInWRte.setNextWriteValue(0);

		IntegerWriteChannel setRevertTimeout = battery.channel(FroniusGen24Battery.ChannelId.SET_REVERT_TIMEOUT);
		setRevertTimeout.setNextWriteValue(SHORT_REVERT_TIMEOUT_SECONDS);
	}

	private Result handleInternalMode() {
		return new Result(SetControlMode.DISABLED, 0, 0);
	}

	private Result handleRemoteMode(int setActivePower) {

		Integer wChaMax = this.readWChaMax();

		if (wChaMax == null || wChaMax <= 0) {
			return this.handleInternalMode();
		}

		int limitedActivePower = clamp(setActivePower, -wChaMax, wChaMax);

		int rate = (int) Math.round((double) limitedActivePower / (double) wChaMax * RATE_100_PERCENT);
		rate = clamp(rate, -RATE_100_PERCENT, RATE_100_PERCENT);

		int outWRte = rate;
		int inWRte = rate * (-1);

		return new Result(SetControlMode.CHARGE_AND_DISCHARGE_LIMIT, outWRte, inWRte);
	}

	private Integer readWChaMax() {
		try {
			var channel = this.parent.getStorageWChaMaxChannel();
			var nextValue = channel.getNextValue();
			if (nextValue.isDefined()) {
				return Math.max(0, Math.round(Math.abs(nextValue.get())));
			}
			var value = channel.value();
			if (value.isDefined()) {
				return Math.max(0, Math.round(Math.abs(value.get())));
			}
			return null;
		} catch (OpenemsException e) {
			return null;
		}
	}

	private boolean shouldWrite(Result result) {

		if (this.lastControlMode == null) {
			return true;
		}

		// A mode change must always be written immediately, regardless of the
		// throttle below - StorCtlMod is written every cycle (see apply()), so
		// if we delayed the rate registers here, the inverter could briefly act
		// on a stale OutWRte/InWRte value right when the new mode's gate opens.
		if (result.controlMode() != this.lastControlMode) {
			return true;
		}

		long now = System.currentTimeMillis();

		if (now - this.lastWriteMillis < MIN_WRITE_INTERVAL_MS) {
			return false;
		}

		if (this.lastOutWRte == null || Math.abs(result.outWRte() - this.lastOutWRte) >= RATE_HYSTERESIS) {
			return true;
		}

		if (this.lastInWRte == null || Math.abs(result.inWRte() - this.lastInWRte) >= RATE_HYSTERESIS) {
			return true;
		}

		return now - this.lastWriteMillis >= KEEP_ALIVE_INTERVAL_MS;
	}

	private void rememberWrittenResult(Result result) {
		this.lastControlMode = result.controlMode();
		this.lastOutWRte = result.outWRte();
		this.lastInWRte = result.inWRte();
		this.lastWriteMillis = System.currentTimeMillis();
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	private static record Result(SetControlMode controlMode, int outWRte, int inWRte) {
	}

	// =========================================================================
	// Inverter power limit (SunSpec Model 123)
	// =========================================================================

	private void applyInverterPowerLimit(Integer limitW) {

		if (limitW != null) {
			Integer wMaxLimPct = this.convertWattsToPct(limitW);
			if (wMaxLimPct != null) {
				this.writeWMaxLim(wMaxLimPct, 1);
			}
		} else {
			// No setpoint → disable power limit
			this.writeWMaxLim(W_MAX_LIM_100_PERCENT, 0);
		}
	}

	private Integer convertWattsToPct(int limitW) {
		try {
			var wMaxChannel = this.parent.getWMaxChannel();
			Float wMax = wMaxChannel.getNextValue().isDefined() ? (Float) wMaxChannel.getNextValue().get()
					: wMaxChannel.value().isDefined() ? (Float) wMaxChannel.value().get() : null;

			if (wMax == null || wMax <= 0) {
				return null;
			}

			int pct = (int) Math.round((double) clamp(limitW, 0, Math.round(wMax)) / wMax * W_MAX_LIM_100_PERCENT);

			return clamp(pct, 0, W_MAX_LIM_100_PERCENT);

		} catch (OpenemsException e) {
			return null;
		}
	}

	private void writeWMaxLim(int wMaxLimPct, int ena) {

		wMaxLimPct = clamp(wMaxLimPct, 0, W_MAX_LIM_100_PERCENT);
		ena = ena == 0 ? 0 : 1;

		long now = System.currentTimeMillis();

		boolean enaChanged = ena != this.lastWMaxLimEna;
		boolean pctChanged = this.lastWMaxLimPct == null
				|| Math.abs(wMaxLimPct - this.lastWMaxLimPct) >= W_MAX_LIM_HYSTERESIS;
		boolean keepAlive = now - this.lastWMaxLimWriteMillis >= W_MAX_LIM_KEEP_ALIVE_MS;

		if (!enaChanged && !pctChanged && !keepAlive) {
			return;
		}

		try {
			this.parent.writeWMaxLimPct(wMaxLimPct);
			this.parent.writeWMaxLimEna(ena);
			this.parent._setDebugWMaxLimPct(wMaxLimPct);
			this.parent._setDebugWMaxLimEna(ena);

			this.lastWMaxLimPct = wMaxLimPct;
			this.lastWMaxLimEna = ena;
			this.lastWMaxLimWriteMillis = now;

		} catch (OpenemsNamedException e) {
			this.log.debug("S123 not available – battery control continues", e);
		}
	}
}