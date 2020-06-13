package io.openems.edge.fenecon.pro.ess;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MaxApparentPowerHandler {

	private final static int MAX_DELTA = 400; // [W]
	private final static int ADJUST_CYCLES = 10;
	private int exceededCounter = 0;
	private int withinCounter = 0;

	private final FeneconProEss parent;
	private final Logger log = LoggerFactory.getLogger(MaxApparentPowerHandler.class);

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

		/*
		 * Evaluate if power and setPower are within delta.
		 */
		if (/* Discharge */ (setPower > 0 && setPower - MAX_DELTA > power) //
				|| /* Charge */ (setPower < 0 && setPower + MAX_DELTA < power)) {
			// Exceeded MaxDelta
			exceededCounter++;
			withinCounter = 0;
		} else {
			// Within MaxDelta
			exceededCounter = 0;
			withinCounter++;
		}

		/*
		 * Adjust MaxApparentPower accordingly and reset counter.
		 */
		if (exceededCounter > ADJUST_CYCLES) {
			this.adjustMaxApparentPower("Exceeded MaxDelta -> Reducing MaxApparentPower", setPower, power,
					oldMaxApparentPower, oldMaxApparentPower - MAX_DELTA);

			this.exceededCounter = 0; // reset

		} else if (withinCounter > ADJUST_CYCLES) {
			this.adjustMaxApparentPower("Within MaxDelta -> Increasing MaxApparentPower", setPower, power,
					oldMaxApparentPower, oldMaxApparentPower + MAX_DELTA);

			this.withinCounter = 0; // reset
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
	}
}
