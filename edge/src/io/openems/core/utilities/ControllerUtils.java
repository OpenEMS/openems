/*******************************************************************************
 * OpenEMS - Open Source Energy Management System
 * Copyright (c) 2016 FENECON GmbH and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * Contributors:
 *   FENECON GmbH - initial API and implementation and initial documentation
 *******************************************************************************/
package io.openems.core.utilities;

import java.util.Collections;
import java.util.List;

public class ControllerUtils {

	public static double calculateCosPhi(long activePower, long reactivePower) {
		return activePower / Math.sqrt(Math.pow(activePower, 2) + Math.pow(reactivePower, 2));
	}

	public static long calculateReactivePower(long activePower, long apparentPower) {
		return (long) Math.sqrt(Math.pow(apparentPower, 2) - Math.pow(activePower, 2));
	}

	public static long calculateReactivePower(long activePower, double cosPhi, boolean capacitive) {
		long apparentPower = calculateApparentPower(activePower, cosPhi);
		long reactivePower = calculateReactivePower(activePower, apparentPower);
		// differe between capacitive and inductive CosPhi
		if (capacitive) {
			reactivePower *= -1;
		}
		return reactivePower;
	}

	public static long calculateActivePower(long reactivePower, long apparentPower) {
		return (long) Math.sqrt(Math.pow(apparentPower, 2) - Math.pow(reactivePower, 2));
	}

	public static long calculateApparentPower(long activePower, long reactivePower) {
		return (long) Math.sqrt(Math.pow(activePower, 2) + Math.pow(reactivePower, 2));
	}

	/**
	 * Calculates the absolute activePower according to the cosPhi
	 *
	 * @param apparentPower
	 * @param cosPhi
	 * @return returns the absolute activePower
	 */
	public static long calculateActivePowerFromApparentPower(long apparentPower, double cosPhi) {
		return (long) (apparentPower * cosPhi);
	}

	//
	// public static long calculateActivePowerFromReactivePower(long reactivePower, double cosPhi) {
	// return (long) (reactivePower / Math.tan(Math.acos(cosPhi)));
	// }
	//
	public static long calculateApparentPower(long activePower, double cosPhi) {
		return (long) (activePower / cosPhi);
	}

	//
	// public static long reduceActivePower(long activePower, long reactivePower, long maxChargeApparentPower,
	// long maxDischargeApparentPower) {
	// boolean activePowerPos = true;
	//
	// if (activePower < 0) {
	// activePowerPos = false;
	// }
	//
	// if (isCharge(activePower, reactivePower)) {
	// /*
	// * Charge
	// */
	// if (calculateApparentPower(activePower, reactivePower) * -1 < maxChargeApparentPower) {
	// double cosPhi = ControllerUtils.calculateCosPhi(activePower, reactivePower);
	// activePower = ControllerUtils.calculateActivePowerFromApparentPower(maxChargeApparentPower, cosPhi);
	// }
	// } else {
	//
	// /*
	// * Discharge
	// */
	// if (ControllerUtils.calculateApparentPower(activePower, reactivePower) > maxDischargeApparentPower) {
	// double cosPhi = ControllerUtils.calculateCosPhi(activePower, reactivePower);
	// activePower = ControllerUtils.calculateActivePowerFromApparentPower(maxDischargeApparentPower, cosPhi);
	// }
	// }
	// if (!activePowerPos && activePower >= 0) {
	// activePower *= -1;
	// }
	//
	// return activePower;
	// }
	//
	// public static long reduceReactivePower(long activePower, long reactivePower, long maxChargeApparentPower,
	// long maxDischargeApparentPower, long maxApparent, WriteChannel<Long> setReactivePower) {
	// boolean reactivePowerPos = true;
	// if (reactivePower < 0) {
	// reactivePowerPos = false;
	// }
	// if (isCharge(activePower, reactivePower)) {
	// /*
	// * Charge
	// */
	// if (calculateApparentPower(activePower, reactivePower) * -1 < maxChargeApparentPower) {
	// double cosPhi = ControllerUtils.calculateCosPhi(activePower, reactivePower);
	// reactivePower = ControllerUtils.calculateReactivePower(reduceActivePower(activePower, reactivePower,
	// maxChargeApparentPower, maxDischargeApparentPower), cosPhi);
	// }
	// } else {
	//
	// /*
	// * Discharge
	// */
	// if (ControllerUtils.calculateApparentPower(activePower, reactivePower) > maxDischargeApparentPower) {
	// double cosPhi = ControllerUtils.calculateCosPhi(activePower, reactivePower);
	// reactivePower = ControllerUtils.calculateReactivePower(reduceActivePower(activePower, reactivePower,
	// maxChargeApparentPower, maxDischargeApparentPower), cosPhi);
	// }
	// }
	// if (!reactivePowerPos && reactivePower >= 0) {
	// reactivePower *= -1;
	// }
	// return reactivePower;
	// }
	//
	public static boolean isCharge(long activePower, long reactivePower) {
		if (activePower >= 0) {
			return false;
		} else {
			return true;
		}
	}

	public static double getValueOfLine(List<Point> points, double x) {
		Point smaller = getSmallerPoint(points, x);
		Point greater = getGreaterPoint(points, x);
		if (smaller != null && greater == null) {
			return smaller.y;
		} else if (smaller == null && greater != null) {
			return greater.y;
		} else if (smaller != null && greater != null) {
			if (smaller.equals(greater)) {
				return smaller.y;
			} else {
				double m = (greater.y - smaller.y) / (greater.x - smaller.x);
				double t = smaller.y - m * smaller.x;
				return m * x + t;
			}
		} else {
			throw new ArithmeticException("x[" + x + "] is out of Range of the points");
		}
	}

	private static Point getGreaterPoint(List<Point> points, double x) {
		Collections.sort(points, (p1, p2) -> (int) (p1.x - p2.x));
		for (int i = 0; i < points.size(); i++) {
			Point p = points.get(i);
			if (p.x >= x) {
				return p;
			}
		}
		return null;
	}

	private static Point getSmallerPoint(List<Point> points, double x) {
		Collections.sort(points, (p1, p2) -> (int) (p2.x - p1.x));
		for (int i = 0; i < points.size(); i++) {
			Point p = points.get(i);
			if (p.x <= x) {
				return p;
			}
		}
		return null;
	}
}
