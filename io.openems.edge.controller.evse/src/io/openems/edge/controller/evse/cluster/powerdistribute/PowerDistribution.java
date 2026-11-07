package io.openems.edge.controller.evse.cluster.powerdistribute;

import static com.google.common.base.MoreObjects.toStringHelper;
import static io.openems.edge.evse.api.common.ApplySetPoint.roundDownToPowerStep;

import java.util.ArrayList;
import java.util.List;

import io.openems.edge.controller.evse.cluster.DistributionStrategy;
import io.openems.edge.controller.evse.cluster.powerdistribute.ramp.PowerDistributionRamp;
import io.openems.edge.controller.evse.single.Mode;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.evse.api.common.ApplySetPoint;

public class PowerDistribution {
	protected final PowerDistributionEntries entries;
	protected final DistributionStrategy distributionStrategy;

	/**
	 * Calculates the total excess power, depending on the current PV production and
	 * house consumption.
	 */
	private final int totalExcessPower;

	protected PowerDistribution(PowerDistributionEntries entries, DistributionStrategy distributionStrategy,
			int totalExcessPower) {
		this.entries = entries;
		this.distributionStrategy = distributionStrategy;
		this.totalExcessPower = totalExcessPower;
	}

	private int calculateTotalSetPoint() {
		int total = 0;
		for (var entry : this.entries.getActiveEntries()) {
			total += entry.setPointInWatt;
		}
		return total;
	}

	/**
	 * Distribute excess power to Controllers in {@link Mode#SURPLUS} mode.
	 *
	 * <p>
	 * First distributes minimum required power to each Controller (e.g. 6 A on
	 * single-/three-phase); then distributes remaining excess power as per given
	 * {@link DistributionStrategy}.
	 */
	public void distributeSurplusPower() {
		this.initializeSetPoints();

		var totalFixedPower = this.calculateTotalSetPoint();
		var totalDistributablePower = Math.max(0, this.totalExcessPower - totalFixedPower);

		var remainingDistributablePower = this.distributeSurplusMinPower(totalDistributablePower);
		this.distributeSurplusRemainingPower(remainingDistributablePower);
		this.distributeToApplySetPointStep();
	}

	/**
	 * Applies ramping to all active entries by updating each set-point to the
	 * ramp-limited value.
	 *
	 * @param ramp the ramp state and rules used to calculate the ramped set-point
	 */
	public void applyRamp(PowerDistributionRamp ramp) {
		for (var entry : this.entries.getActiveEntries()) {
			entry.setPointInWatt = ramp.getRampedSetPoint(entry);
		}
	}

	/**
	 * Stores the current set-points of all active entries as the "last set-points"
	 * in the given ramp.
	 *
	 * <p>
	 * Existing stored values are cleared before the current values are written.
	 *
	 * @param ramp the ramp that keeps the last set-point values per entry key
	 */
	public void storeLastSetPointsInRamp(PowerDistributionRamp ramp) {
		ramp.clearLastSetPoints();
		for (var entry : this.entries.getActiveEntries()) {
			ramp.setLastSetPoint(entry.key, entry.setPointInWatt);
		}
	}

	/**
	 * Initialize the Set-Points for {@link Mode#FORCE}, {@link Mode#MINIMUM} and
	 * {@link Mode#ZERO}.
	 */
	protected void initializeSetPoints() {
		for (var e : this.entries.getActiveEntries()) {
			e.setPointInWatt = switch (e.mode) {
			case MINIMUM -> e.applySetPoint.min();
			case FORCE -> e.applySetPoint.max();
			case SURPLUS, ZERO -> 0;
			};
		}
	}

	/**
	 * Distribute minimum required power to each Controller (e.g. 6 A on
	 * single-/three-phase).
	 *
	 * @param distributablePower the total distributable power (i.e. the excess
	 *                           power)
	 * @return the remaining distributable power
	 */
	protected int distributeSurplusMinPower(int distributablePower) {
		var remaining = distributablePower;
		for (var e : this.entries.getActiveEntries()) {
			if (!e.hasSurplus()) {
				continue;
			}

			var power = e.applySetPoint.toPower(e.applySetPoint.min());
			if (power > remaining) {
				continue;
			}

			e.setPointInWatt = power;
			remaining -= power;
		}
		return remaining;
	}

	/**
	 * Distribute distributablePower (i.e. remaining excess power) as per given
	 * {@link DistributionStrategy}.
	 *
	 * @param distributablePower the total distributable power (i.e. remaining
	 *                           excess power)
	 */
	protected void distributeSurplusRemainingPower(int distributablePower) {
		var entriesWithSurplusGreaterZero = this.entries.computeEntriesWithSurplusGreaterZero();
		if (entriesWithSurplusGreaterZero.isEmpty()) {
			return;
		}

		switch (this.distributionStrategy) {
		case EQUAL_POWER -> this.distributePowerEqual(entriesWithSurplusGreaterZero, distributablePower);
		case BY_PRIORITY -> this.distributePowerByPriority(entriesWithSurplusGreaterZero, distributablePower);
		}
	}

	/**
	 * Distribute power equally among Controllers.
	 *
	 * @param initialEntries            the PowerDistribution Entries
	 * @param initialDistributablePower the distributable power
	 */
	protected void distributePowerEqual(final List<PowerDistributionEntry> initialEntries,
			final int initialDistributablePower) {
		var possibleEntries = new ArrayList<PowerDistributionEntry>(initialEntries.size());
		for (var entry : initialEntries) {
			if (entry.setPointInWatt < entry.applySetPoint.max()) {
				possibleEntries.add(entry);
			}
		}

		if (possibleEntries.isEmpty()) {
			return; // avoid divide by zero
		}

		final var equalPower = initialDistributablePower / possibleEntries.size();
		var remaining = initialDistributablePower;
		for (var e : possibleEntries) {
			var before = e.setPointInWatt;
			var after = e.applySetPoint.fitWithin(before + equalPower);
			remaining -= after - before;

			e.setPointInWatt = after;
		}

		if (initialDistributablePower != remaining) {
			// Recursive call to distribute remaining power
			this.distributePowerEqual(possibleEntries, remaining);
		}
	}

	/**
	 * Distribute power by priority among Controllers.
	 *
	 * @param entriesWithSurplusGreaterZero All entries with surplus greater than
	 *                                      zero.
	 * @param distributablePower            the distributable power
	 */
	protected void distributePowerByPriority(List<PowerDistributionEntry> entriesWithSurplusGreaterZero,
			int distributablePower) {
		var remaining = distributablePower;
		for (var e : entriesWithSurplusGreaterZero) {
			var before = e.setPointInWatt;
			var after = e.applySetPoint.fitWithin(before + remaining);

			remaining -= after - before;
			e.setPointInWatt = after;
		}
	}

	/**
	 * This last step distributes the power according to the 'steps' defined in the
	 * {@link ApplySetPoint.Ability}.
	 *
	 * <p>
	 * Example: if a {@link EvseChargePoint} only supports
	 * {@link ApplySetPoint.Ability.Ampere}, its set-point is adjusted (reduced) to
	 * match the step. The gained power is again distributed among the Controllers.
	 */
	private void distributeToApplySetPointStep() {
		var entriesWithSurplus = this.entries.computeEntriesWithSurplusGreaterZero();
		var distributablePower = 0;
		for (var e : entriesWithSurplus.reversed()) {
			var set = roundDownToPowerStep(e.applySetPoint, e.setPointInWatt);
			distributablePower += e.setPointInWatt - set;
			e.setPointInWatt = set;
		}
		for (var e : entriesWithSurplus) {
			if (distributablePower < 1) {
				break;
			}
			var set = roundDownToPowerStep(e.applySetPoint, e.setPointInWatt + distributablePower);
			distributablePower -= set - e.setPointInWatt;
			e.setPointInWatt = set;
		}
	}

	@Override
	public final String toString() {
		return toStringHelper(this.getClass()) //
				.add("entries", "\n" + this.entries.toString()).toString();
	}
}
