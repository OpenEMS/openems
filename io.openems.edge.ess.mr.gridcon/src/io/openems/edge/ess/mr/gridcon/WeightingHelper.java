// CHECKSTYLE:OFF
package io.openems.edge.ess.mr.gridcon;

import io.openems.edge.battery.api.Battery;

public class WeightingHelper {

	/**
	 * Calculates the weighting for Gridcon depending on connected batteries and
	 * given active power.
	 * 
	 * @param activePower
	 * @param b1
	 * @param b2
	 * @param b3
	 * @return
	 */
	public static Float[] getWeighting(float activePower, Battery b1, Battery b2, Battery b3) {

		Float[] ret = { 0f, 0f, 0f };

		// Discharge
		if (activePower > 0) {
			ret[0] = getWeightingForDischarge(b1);
			ret[1] = getWeightingForDischarge(b2);
			ret[2] = getWeightingForDischarge(b3);
			// Charge
		} else if (activePower < 0) {
			ret[0] = getWeightingForCharge(b1);
			ret[1] = getWeightingForCharge(b2);
			ret[2] = getWeightingForCharge(b3);
			// active power is zero
		} else {
			ret = getWeightingForNoPower(b1, b2, b3);
		}

		return ret;
	}

	static Float[] getWeightingForNoPower(Battery b1, Battery b2, Battery b3) {

		float weightA = 0;
		if (isBatteryReady(b1)) {
			weightA = 1;
		}
		float weightB = 0;
		if (isBatteryReady(b2)) {
			weightB = 1;
		}

		float weightC = 0;
		if (isBatteryReady(b3)) {
			weightC = 1;
		}

		return new Float[] { weightA, weightB, weightC };
	}

	static float getWeightingForCharge(Battery b) {
		float weight = 0;
		if (b != null && isBatteryReady(b)) {
			float current = Math.min(EssGridcon.MAX_CURRENT_PER_STRING, b.getChargeMaxCurrent().get());
			float voltage = b.getVoltage().get();
			weight = current * voltage;
		}
		return weight;
	}

	static float getWeightingForDischarge(Battery b) {
		float weight = 0;
		if (b != null && isBatteryReady(b)) {
			float current = Math.min(EssGridcon.MAX_CURRENT_PER_STRING, b.getDischargeMaxCurrent().get());
			float voltage = b.getVoltage().get();
			weight = current * voltage;
		}
		return weight;
	}

	static boolean isBatteryReady(Battery battery) {
		if (battery == null) {
			return false;
		}
		return !Helper.isUndefined(battery) && battery.isStarted();
	}

	public static int getStringControlMode(Battery battery1, Battery battery2, Battery battery3) {
		int weightingMode = 0;

		boolean useBatteryStringA = (battery1 != null && battery1.isStarted());
		if (useBatteryStringA) {
			weightingMode = weightingMode + 1; // battA = 1 (2^0)
		}
		boolean useBatteryStringB = (battery2 != null && battery2.isStarted());
		if (useBatteryStringB) {
			weightingMode = weightingMode + 8; // battB = 8 (2^3)
		}
		boolean useBatteryStringC = (battery3 != null && battery3.isStarted());
		if (useBatteryStringC) {
			weightingMode = weightingMode + 64; // battC = 64 (2^6)
		}

		return weightingMode;
	}
}
// CHECKSTYLE:ON
