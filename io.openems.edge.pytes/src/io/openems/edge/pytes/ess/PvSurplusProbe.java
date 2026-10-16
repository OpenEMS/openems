package io.openems.edge.pytes.ess;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Surplus search for AC output control (reg 44105 = 4).
 *
 * <p>
 * In this mode the inverter regulates its AC output to the set-point and
 * curtails PV (MPPT off the maximum power point) as soon as the battery cannot
 * absorb the rest. The measured PV then only reflects the curtailment, not the
 * available production: with a full battery and a set-point at house
 * consumption, 6 kW of available PV were used at 100 W (live 2026-09-18), and
 * whether the operating point drifted up or down depended on +-100 W of
 * battery noise. Nothing in the control chain asks for more.
 *
 * <p>
 * The probe therefore raises an AC floor above the measured PV in steps: while
 * the PV follows a step, the next (doubled, up to {@link #STEP_MAX_W}) one is
 * taken after {@link #PROBE_INTERVAL_MS};
 * when the battery has to fill the gap instead (ActivePower - PV rises by more
 * than {@link #MARGIN_W} against the value at the step), the available PV is
 * reached, the floor falls back to the last good level and the search pauses
 * for a growing hold time. Outside a step the floor never exceeds the measured
 * PV, so a PV drop (cloud) pulls it down immediately.
 *
 * <p>
 * The floor is bounded by consumption + feed-in limit - {@link #RESERVE_W}:
 * deliberately below the inverter's own export cap, because in cap mode the
 * inverter ignores the set-point (finding of 2026-09-17). The probe is off
 * while the battery still absorbs surplus (the inverter charges the battery
 * before it curtails PV), at night, and while the dynamic feed-in limitation
 * (reg 43052) curtails PV on purpose.
 *
 * <p>
 * Pure computation, see {@link PvLimitHandler}.
 */
class PvSurplusProbe {

	/** First probe step above the measured PV; doubled on every success. */
	static final int STEP_W = 300;
	/** Largest probe step: a recovery after a cloud must not take minutes. */
	static final int STEP_MAX_W = 1200;
	/** Battery contribution that counts as "PV did not follow". */
	static final int MARGIN_W = 150;
	/** Distance kept below the feed-in limit so the inverter's cap stays out. */
	static final int RESERVE_W = 300;
	/** Below this PV the search is off (night). */
	static final int MIN_PV_W = 50;
	/** Time between two probe steps; also the time a step gets to succeed. */
	static final int PROBE_INTERVAL_MS = 10_000;
	/** A step is judged only after the inverter had time to follow (~5 s). */
	static final int EVAL_MS = 6_000;
	/** Pause after a failed step, doubled on every consecutive failure. */
	static final int HOLD_MIN_MS = 60_000;
	static final int HOLD_MAX_MS = 300_000;
	/** Cycles used for averaging PV and consumption. */
	static final int AVERAGE_CYCLES = 5;

	private final Deque<Integer> pvValues = new ArrayDeque<>();
	private final Deque<Integer> essValues = new ArrayDeque<>();
	private final Deque<Integer> gapValues = new ArrayDeque<>();
	private final Deque<Integer> consumptionValues = new ArrayDeque<>();

	private int floor = 0;
	private long elapsedMs = 0;
	private long lastStepMs = Long.MIN_VALUE / 2;
	private long holdUntilMs = Long.MIN_VALUE / 2;
	private int holdMs = HOLD_MIN_MS;
	private boolean stepPending = false;
	private int floorBeforeStep = 0;
	private int gapAtStep = 0;
	private int pvAtStep = 0;
	private int stepW = STEP_W;

	/**
	 * Computes the AC floor for this cycle.
	 *
	 * @param pvPower        measured PV in W
	 * @param essActivePower ESS AC power in W (positive = export)
	 * @param gridPower      grid power in W (negative = export), null if unknown
	 * @param batteryPower   battery power in W (positive = discharge)
	 * @param chargeLimit    raw battery charge limit in W (negative or 0)
	 * @param feedInLimit    feed-in limit in W, null for no limitation
	 * @param ratedPower     upper bound when there is no feed-in limit
	 * @param pvLimitActive  the dynamic feed-in limitation (reg 43052) is active
	 * @param cycleTimeMs    the configured cycle time
	 * @return the AC floor in W, 0 for none
	 */
	int compute(int pvPower, int essActivePower, Integer gridPower, int batteryPower, int chargeLimit,
			Integer feedInLimit, int ratedPower, boolean pvLimitActive, int cycleTimeMs) {
		this.elapsedMs += cycleTimeMs;
		final int pvAvg = average(this.pvValues, pvPower);
		final int essAvg = average(this.essValues, essActivePower);
		final int consumption = average(this.consumptionValues, gridPower != null ? essActivePower + gridPower : 0);

		if (pvLimitActive || pvPower < MIN_PV_W) {
			return this.reset();
		}

		final int maxFloor;
		if (feedInLimit != null) {
			// without a grid meter the consumption is unknown: assume 0
			maxFloor = Math.max(0, (gridPower != null ? consumption : 0) + feedInLimit - RESERVE_W);
		} else {
			maxFloor = ratedPower;
		}

		// Battery share of the AC output. Derived from the instantaneous
		// measurements (no BMS lag): rises when the inverter has to take battery
		// power to reach the set-point, i.e. when the PV cannot follow. Judged
		// against its average at the step, and a step is only taken while the
		// gap is steady - right after a set-point change the AC output lags the
		// PV by hundreds of watts (live 2026-09-18), which would look like a
		// failed step otherwise.
		int gap = essActivePower - pvPower;
		int gapAvg = average(this.gapValues, gap);
		boolean steady = this.gapValues.size() >= AVERAGE_CYCLES && Math.abs(gap - gapAvg) <= MARGIN_W;

		if (this.stepPending) {
			long sinceStep = this.elapsedMs - this.lastStepMs;
			if (sinceStep >= EVAL_MS && gap - this.gapAtStep > MARGIN_W) {
				// PV did not follow: back to the last good level
				this.stepPending = false;
				this.floor = this.floorBeforeStep;
				if (pvPower < this.pvAtStep - MARGIN_W) {
					// the PV dropped during the step (cloud): no ceiling was found,
					// the floor follows the PV down below and the search goes on
					this.holdMs = HOLD_MIN_MS;
				} else {
					// ceiling found: pause, longer on every consecutive hit, and
					// approach it with small steps next time
					this.holdUntilMs = this.elapsedMs + this.holdMs;
					this.holdMs = Math.min(HOLD_MAX_MS, this.holdMs * 2);
					this.stepW = STEP_W;
				}
			} else if (sinceStep >= PROBE_INTERVAL_MS) {
				// PV followed: larger steps while it does
				this.stepPending = false;
				this.holdMs = HOLD_MIN_MS;
				this.stepW = Math.min(STEP_MAX_W, this.stepW * 2);
			}
		}

		if (!this.stepPending) {
			// Outside a step the floor never exceeds the measurement: a found
			// level is kept, a PV drop pulls it down.
			this.floor = Math.min(this.floor, pvAvg);
			// The inverter charges the battery before it curtails PV, so while the
			// battery charges below its limit (or delivers) the measured PV is the
			// available one. An idle battery counts as saturated as well: the
			// inverter may refuse to charge for its own reasons (its max-charge
			// SoC, live at 99 % with 12 A BMS allowance) and curtail instead.
			boolean batterySaturated = batteryPower <= chargeLimit + MARGIN_W || Math.abs(batteryPower) <= MARGIN_W;
			boolean due = this.elapsedMs >= this.holdUntilMs //
					&& this.elapsedMs - this.lastStepMs >= PROBE_INTERVAL_MS;
			int next = Math.min(maxFloor, pvAvg + this.stepW);
			if (batterySaturated && steady && due && next > this.floor) {
				// The level to fall back to is what the inverter delivered before
				// the step: the PV with a full battery, the controllers' set-point
				// while the battery is still charging (the floor may be 0 here).
				this.floorBeforeStep = Math.min(pvAvg, essAvg);
				this.gapAtStep = gapAvg;
				this.pvAtStep = pvAvg;
				this.floor = next;
				this.lastStepMs = this.elapsedMs;
				this.stepPending = true;
			}
		}
		return Math.max(0, Math.min(this.floor, maxFloor));
	}

	private int reset() {
		this.floor = 0;
		this.stepPending = false;
		this.holdMs = HOLD_MIN_MS;
		this.stepW = STEP_W;
		return 0;
	}

	/**
	 * Whether a probe step is currently being evaluated.
	 *
	 * @return true while a step is pending
	 */
	boolean isProbing() {
		return this.stepPending;
	}

	private static int average(Deque<Integer> values, int value) {
		values.addLast(value);
		while (values.size() > AVERAGE_CYCLES) {
			values.removeFirst();
		}
		return (int) Math.round(values.stream().mapToInt(Integer::intValue).average().orElse(value));
	}
}
