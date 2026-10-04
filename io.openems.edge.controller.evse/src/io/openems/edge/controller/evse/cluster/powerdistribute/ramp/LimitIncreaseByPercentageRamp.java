package io.openems.edge.controller.evse.cluster.powerdistribute.ramp;

import java.time.Clock;
import java.time.Duration;

import io.openems.edge.controller.evse.cluster.powerdistribute.PowerDistributionEntry;

public class LimitIncreaseByPercentageRamp extends PowerDistributionRamp {
	/**
	 * Max allowed change for increasing power/current. A value of 0.03 requires
	 * about 1 minute from 6 A to 32 A.
	 */
	private final float maxPercentageChangePerSecond;

	public LimitIncreaseByPercentageRamp(float maxPercentageChangePerSecond, Clock clock) {
		super(clock);
		this.maxPercentageChangePerSecond = maxPercentageChangePerSecond;
	}

	@Override
	protected int getConsumptionLimitInWatt(PowerDistributionEntry entry) {
		var lastSetPoint = this.getLastSetPointInWatt(entry.key);
		if (lastSetPoint == null) {
			return entry.applySetPoint.min();
		}

		if (entry.getSetPointInWatt() < lastSetPoint.setPointInWatt()) {
			return entry.getSetPointInWatt();
		}

		var elapsedMillis = Duration.between(lastSetPoint.time(), this.clock.instant()).toMillis();
		var allowedIncrease = (int) Math
				.ceil(lastSetPoint.setPointInWatt() * this.maxPercentageChangePerSecond * (elapsedMillis / 1000f));
		return Math.max(entry.applySetPoint.min(), lastSetPoint.setPointInWatt() + allowedIncrease);
	}
}
