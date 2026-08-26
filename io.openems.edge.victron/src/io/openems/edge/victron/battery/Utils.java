package io.openems.edge.victron.battery;

import static java.lang.Math.max;

import java.util.List;
import java.util.Objects;

import io.openems.edge.controller.ess.emergencycapacityreserve.ControllerEssEmergencyCapacityReserve;
import io.openems.edge.controller.ess.limittotaldischarge.ControllerEssLimitTotalDischarge;

/**
 * Utils for Victron Battery.
 */
public final class Utils {

	private Utils() {
	}

	/**
	 * Returns the configured minimum SoC, or zero.
	 *
	 * @param ctrlLimitTotalDischarges      the list of
	 *                                      {@link ControllerEssLimitTotalDischarge}
	 * @param ctrlEmergencyCapacityReserves the list of
	 *                                      {@link ControllerEssEmergencyCapacityReserve}
	 * @return the value in [%]
	 */
	public static int getEssMinSocPercentage(List<ControllerEssLimitTotalDischarge> ctrlLimitTotalDischarges,
			List<ControllerEssEmergencyCapacityReserve> ctrlEmergencyCapacityReserves) {

		int minDischargeSoc = (ctrlLimitTotalDischarges != null)//
				? ctrlLimitTotalDischarges.stream()//
						.map(ctrl -> ctrl.getMinSoc().get())//
						.filter(Objects::nonNull)//
						.mapToInt(v -> max(0, v))//
						.max().orElse(0)//
				: 0;

		int minReserveSoc = (ctrlEmergencyCapacityReserves != null)//
				? ctrlEmergencyCapacityReserves.stream()//
						.map(ctrl -> ctrl.getActualReserveSoc().get())//
						.filter(Objects::nonNull)//
						.mapToInt(v -> max(0, v))//
						.max().orElse(0)//
				: 0;

		return max(minDischargeSoc, minReserveSoc);
	}
}
