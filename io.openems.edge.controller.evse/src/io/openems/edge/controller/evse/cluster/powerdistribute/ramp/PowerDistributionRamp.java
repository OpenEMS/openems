package io.openems.edge.controller.evse.cluster.powerdistribute.ramp;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import io.openems.edge.controller.evse.cluster.powerdistribute.PowerDistributionEntry;
import io.openems.edge.controller.evse.cluster.powerdistribute.PowerDistributionKey;

public abstract class PowerDistributionRamp {

	protected final Map<PowerDistributionKey, StoredLastSetPoint> lastSetPoints = new HashMap<>();
	protected Clock clock;

	protected PowerDistributionRamp(Clock clock) {
		this.clock = clock;
	}

	/**
	 * Calculates the consumption limit in Watt for the given entry.
	 *
	 * @param entry the power distribution entry
	 * @return the allowed consumption limit in Watt
	 */
	protected abstract int getConsumptionLimitInWatt(PowerDistributionEntry entry);

	/**
	 * Gets the last stored set-point for the given key.
	 *
	 * @param key the power distribution key
	 * @return the stored last set-point or {@code null} if no set-point exists
	 */
	public StoredLastSetPoint getLastSetPointInWatt(PowerDistributionKey key) {
		return this.lastSetPoints.get(key);
	}

	/**
	 * Removes all stored last set-points.
	 */
	public void clearLastSetPoints() {
		this.lastSetPoints.clear();
	}

	/**
	 * Stores a set-point for the given key with the current timestamp.
	 *
	 * @param key            the power distribution key
	 * @param setPointInWatt the set-point in Watt
	 */
	public void setLastSetPoint(PowerDistributionKey key, int setPointInWatt) {
		this.lastSetPoints.put(key, new StoredLastSetPoint(this.clock.instant(), setPointInWatt));
	}

	/**
	 * Returns the ramped set point of the given entry.
	 *
	 * <p>
	 * If the set-point is smaller than or equal to the minimum apply set-point, it
	 * is returned unchanged.
	 *
	 * @param entry the power distribution entry
	 * @return the limited set-point in Watt
	 */
	public int getRampedSetPoint(PowerDistributionEntry entry) {
		if (entry.getSetPointInWatt() <= entry.applySetPoint.min()) {
			return entry.getSetPointInWatt();
		}

		var limit = this.getConsumptionLimitInWatt(entry);
		return Math.min(entry.getSetPointInWatt(), Math.min(limit, entry.applySetPoint.max()));
	}

	public record StoredLastSetPoint(Instant time, int setPointInWatt) {
	}
}
