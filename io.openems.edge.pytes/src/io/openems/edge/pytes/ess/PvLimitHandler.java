package io.openems.edge.pytes.ess;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Dynamic grid feed-in limitation, ported from the SolarEdge hybrid
 * implementation: as soon as the export exceeds the limit from
 * Core.Meta, the inverter's AC output is capped (reg 43052, in % of the rated
 * power) at "current consumption + limit - tolerance", so the surplus PV has to
 * be curtailed. Below the limit the cap is released (100 %).
 *
 * <p>
 * Consumption is derived from the measured values: ess + grid = house incl.
 * backup port (PV is inside the ESS AC power). Grid and ESS power are averaged
 * over a few cycles to keep the cap calm. The register is only written when
 * the percentage changes and not more often than once per {@link #MIN_WRITE_MS}.
 *
 * <p>
 * The handler is pure computation; the ESS feeds it every cycle and writes the
 * result.
 */
class PvLimitHandler {

	/**
	 * Kept below the limit so that measurement jitter does not exceed it. Scaled
	 * down for small limits (see {@link #tolerance(int)}): with a 400 W limit a
	 * fixed 500 W would cap the output at 0 % and never release.
	 */
	static final int TOLERANCE_W = 500;
	/** Cycles used for averaging grid and ESS power. */
	static final int AVERAGE_CYCLES = 5;
	/** Minimum time between two register writes. */
	static final int MIN_WRITE_MS = 10_000;
	/**
	 * Time after a write before "cap not binding" is evaluated: the inverter needs
	 * ~5 s to follow a raised cap and the averages another 5 s.
	 */
	static final int SETTLE_MS = 10_000;
	static final int NO_LIMIT_PERCENT = 100;

	private final Deque<Integer> gridValues = new ArrayDeque<>();
	private final Deque<Integer> essValues = new ArrayDeque<>();
	private boolean limiting = false;
	private int lastWrittenPercent = -1;
	/** The cap the inverter currently applies (last written), null = 100 %. */
	private Integer writtenAcLimitW = null;
	private long lastWriteMs = Long.MIN_VALUE;
	private long elapsedMs = 0;

	/**
	 * Result of one cycle.
	 *
	 * @param limiting      whether the limitation is active
	 * @param acLimitW      the AC output cap in W, or null when not limiting
	 * @param percent       the percentage that should be in reg 43052
	 * @param writePercent  the percentage to write this cycle, or null if
	 *                      nothing has to be written
	 */
	record Result(boolean limiting, Integer acLimitW, int percent, Integer writePercent) {
	}

	/**
	 * Computes the AC output cap for this cycle.
	 *
	 * @param gridPower      grid power in W (negative = export), null if unknown
	 * @param essActivePower ESS AC power in W (positive = discharge/export)
	 * @param feedInLimitW   the feed-in limit in W, or null for no limitation
	 * @param ratedPowerW    the inverter's rated power (100 % of reg 43052)
	 * @param cycleTimeMs    the configured cycle time
	 * @return the {@link Result}
	 */
	Result compute(Integer gridPower, Integer essActivePower, Integer feedInLimitW, int ratedPowerW,
			int cycleTimeMs) {
		this.elapsedMs += cycleTimeMs;

		if (feedInLimitW == null || gridPower == null || essActivePower == null || ratedPowerW <= 0) {
			// no limitation possible/wanted: release the cap
			this.limiting = false;
			this.gridValues.clear();
			this.essValues.clear();
			return this.result(false, null, NO_LIMIT_PERCENT);
		}

		int gridAvg = this.average(this.gridValues, gridPower);
		int essAvg = this.average(this.essValues, essActivePower);

		// Enter limitation when the export exceeds the limit, leave it once the
		// cap is not needed anymore (the released cap would allow more than 100 %)
		boolean exportAboveLimit = -gridAvg > feedInLimitW;
		if (!this.limiting && exportAboveLimit) {
			this.limiting = true;
		}
		if (!this.limiting) {
			return this.result(false, null, NO_LIMIT_PERCENT);
		}

		// consumption (house + backup) = ess + grid; allowed output = consumption + limit,
		// never below the consumption itself (the house is always served from PV)
		int consumption = essAvg + gridAvg;
		int tolerance = tolerance(feedInLimitW);
		int acLimitW = Math.max(Math.max(0, consumption), consumption + feedInLimitW - tolerance);
		int percent = (int) Math.ceil(acLimitW * 100.0 / ratedPowerW);
		// Release the cap when it is not binding anymore: the inverter outputs
		// clearly less than the cap it currently applies (PV dropped), or the
		// cap would be above 100 %. While the cap is binding the available PV is
		// unknown (curtailed), so the cap has to stay; a rising consumption
		// simply raises the cap.
		boolean settled = this.elapsedMs - this.lastWriteMs >= SETTLE_MS;
		boolean capNotBinding = settled && this.writtenAcLimitW != null
				&& essAvg < this.writtenAcLimitW - tolerance;
		if (percent >= NO_LIMIT_PERCENT || capNotBinding) {
			this.limiting = false;
			return this.result(false, null, NO_LIMIT_PERCENT);
		}
		return this.result(true, acLimitW, Math.max(0, percent));
	}

	/**
	 * The distance kept below the feed-in limit: {@link #TOLERANCE_W}, but at
	 * most half of the limit.
	 *
	 * @param feedInLimitW the feed-in limit in W
	 * @return the tolerance in W
	 */
	static int tolerance(int feedInLimitW) {
		return Math.min(TOLERANCE_W, feedInLimitW / 2);
	}

	private Result result(boolean limiting, Integer acLimitW, int percent) {
		Integer write = null;
		boolean changed = percent != this.lastWrittenPercent;
		boolean rateOk = this.elapsedMs - this.lastWriteMs >= MIN_WRITE_MS;
		// releasing the cap is never delayed
		if (changed && (rateOk || percent == NO_LIMIT_PERCENT)) {
			write = percent;
			this.lastWrittenPercent = percent;
			this.lastWriteMs = this.elapsedMs;
			this.writtenAcLimitW = limiting ? acLimitW : null;
		}
		return new Result(limiting, acLimitW, percent, write);
	}

	private int average(Deque<Integer> values, int value) {
		values.addLast(value);
		while (values.size() > AVERAGE_CYCLES) {
			values.removeFirst();
		}
		return (int) Math.round(values.stream().mapToInt(Integer::intValue).average().orElse(value));
	}

	boolean isLimiting() {
		return this.limiting;
	}
}
