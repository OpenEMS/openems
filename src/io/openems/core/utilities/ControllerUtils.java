package io.openems.core.utilities;

public class ControllerUtils {

	public static double calculateCosPhi(long activePower, long reactivePower) {
		return activePower / calculateApparentPower(activePower, reactivePower);
	}

	public static long calculateReactivePower(long activePower, double cosPhi) {
		return (long) (activePower * Math.sqrt(1 / Math.pow(cosPhi, 2) - 1));
	}

	public static long calculateApparentPower(long activePower, long reactivePower) {
		return (long) Math.sqrt(Math.pow(activePower, 2) + Math.pow(reactivePower, 2));
	}

	public static long calculateActivePower(long apparentPower, double cosPhi) {
		return (long) (apparentPower * cosPhi);
	}

	public static long calculateApparentPower(long activePower, double cosPhi) {
		return (long) (activePower / cosPhi);
	}

	public static boolean isCharge(long activePower, long reactivePower) {
		if (activePower >= 0 && reactivePower >= 0) {
			return false;
		} else if (activePower < 0 && reactivePower >= 0) {
			return true;
		} else if (activePower < 0 && reactivePower < 0) {
			return false;
		} else {
			return true;
		}
	}
}
