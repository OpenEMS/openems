package io.openems.edge.fenecon.mini.ess;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MaxApparentPowerHandler {

	private static final int MAX_DELTA = 200; // [W]
	private static final int ADJUST_CYCLES = 10;

	private final FeneconMiniEssImpl parent;
	private final Logger log = LoggerFactory.getLogger(MaxApparentPowerHandler.class);

	private int exceededCounter = 0;
	private int withinCounter = 0;

	public MaxApparentPowerHandler(FeneconMiniEssImpl parent) {
		this.parent = parent;
	}

	protected void calculateMaxApparentPower() {
		var setPower = this.parent.getDebugSetActivePower().get();
		var power = this.parent.getActivePower().get();
		int oldMaxApparentPower = this.parent.getMaxApparentPower().orElse(FeneconMiniEss.MAX_APPARENT_POWER);

		if (setPower == null || power == null) {
			// Reset MaxApparentPower
			this.parent._setMaxApparentPower(FeneconMiniEss.MAX_APPARENT_POWER);
			return;
		}

		/*
		 * Evaluate if power and setPower are within delta.
		 */
		if (/* Discharge */ setPower > 0 && setPower - MAX_DELTA > power //
				|| /* Charge */ setPower < 0 && setPower + MAX_DELTA < power) {
			// Exceeded MaxDelta
			this.exceededCounter++;
			this.withinCounter = 0;
		} else {
			// Within MaxDelta
			this.exceededCounter = 0;
			this.withinCounter++;
		}

		/*
		 * Adjust MaxApparentPower accordingly and reset counter.
		 */
		if (this.exceededCounter > ADJUST_CYCLES) {
			this.adjustMaxApparentPower("Exceeded MaxDelta -> Reducing MaxApparentPower", setPower, power,
					oldMaxApparentPower, oldMaxApparentPower - MAX_DELTA);

			this.exceededCounter = 0; // reset

		} else if (this.withinCounter > ADJUST_CYCLES) {
			this.adjustMaxApparentPower("Within MaxDelta -> Increasing MaxApparentPower", setPower, power,
					oldMaxApparentPower, oldMaxApparentPower + MAX_DELTA);

			this.withinCounter = 0; // reset
		}
	}

	private void adjustMaxApparentPower(String description, Integer setPower, Integer power, int oldMaxApparentPower,
			int newMaxApparentPower) {
		// never below MAX_DELTA * 1.5
		newMaxApparentPower = Math.max(newMaxApparentPower, Math.round(MAX_DELTA * 1.5f));

		// never above MAX_APPARENT_POWER
		newMaxApparentPower = Math.min(newMaxApparentPower, FeneconMiniEss.MAX_APPARENT_POWER);

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
