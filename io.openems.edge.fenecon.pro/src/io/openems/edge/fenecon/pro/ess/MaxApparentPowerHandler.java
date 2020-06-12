package io.openems.edge.fenecon.pro.ess;

import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MaxApparentPowerHandler {

	private final static int MAX_DELTA = 200; // [W]
	private final static int ADJUST_DURATION = 10; // [s]

	private final FeneconProEss parent;
	private final Logger log = LoggerFactory.getLogger(MaxApparentPowerHandler.class);

	private Instant lastAdjust = Instant.now();

	public MaxApparentPowerHandler(FeneconProEss parent) {
		this.parent = parent;
	}

	public void calculateMaxApparentPower() {
		Integer setPower = this.parent.getDebugSetActivePower().get();
		Integer power = this.parent.getActivePower().get();
		int oldMaxApparentPower = this.parent.getMaxApparentPower().orElse(FeneconProEss.MAX_APPARENT_POWER);

		if (setPower == null || power == null) {
			// Reset MaxApparentPower
			this.parent._setMaxApparentPower(FeneconProEss.MAX_APPARENT_POWER);
			return;
		}

		if (Duration.between(this.lastAdjust, Instant.now()).getSeconds() > ADJUST_DURATION) {
			if (Math.abs(setPower - power) > MAX_DELTA) {
				/*
				 * Exceeded MaxDelta -> reduce MaxApparentPower
				 */

				this.adjustMaxApparentPower("Exceeded MaxDelta -> Reducing MaxApparentPower", setPower, power,
						oldMaxApparentPower, oldMaxApparentPower - MAX_DELTA);
			}
		} else {
			if (Math.abs(setPower - power) < MAX_DELTA) {
				/*
				 * Within MaxDelta -> increase MaxApparentPower
				 */

				this.adjustMaxApparentPower("Within MaxDelta -> Increasing MaxApparentPower", setPower, power,
						oldMaxApparentPower, oldMaxApparentPower + MAX_DELTA);
			}
		}
	}

	private void adjustMaxApparentPower(String description, Integer setPower, Integer power, int oldMaxApparentPower,
			int newMaxApparentPower) {
		// never below MAX_DELTA
		newMaxApparentPower = Math.max(newMaxApparentPower, MAX_DELTA);

		// never above MAX_APPARENT_POWER
		newMaxApparentPower = Math.min(newMaxApparentPower, FeneconProEss.MAX_APPARENT_POWER);

		if (oldMaxApparentPower != newMaxApparentPower) {
			this.parent.logInfo(this.log, //
					description + ": " //
							+ "SetPower [" + setPower + "] " //
							+ "Power [" + power + "] " //
							+ "Old [" + oldMaxApparentPower + "] " //
							+ "New [" + newMaxApparentPower + "]");
			this.parent._setMaxApparentPower(newMaxApparentPower);
		}
		this.lastAdjust = Instant.now();
	}
}
