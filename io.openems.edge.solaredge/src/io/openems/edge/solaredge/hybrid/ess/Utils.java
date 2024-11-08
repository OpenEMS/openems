package io.openems.edge.solaredge.hybrid.ess;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.stream.IntStream.concat;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.controller.ess.emergencycapacityreserve.ControllerEssEmergencyCapacityReserve;
import io.openems.edge.controller.ess.limittotaldischarge.ControllerEssLimitTotalDischarge;

import io.openems.edge.controller.ess.chargedischargelimiter.ControllerEssChargeDischargeLimiter;

/**
 * Utils for {@link TimeOfUseTariffController}.
 * 
 * <p>
 * All energy values are in [Wh] and positive, unless stated differently.
 */
public final class Utils {

	private Utils() {
	}

	/** Keep some buffer to avoid scheduling errors because of bad predictions. */
	public static final float ESS_MAX_SOC = 90F;

	/** Limit Charge Power for §14a EnWG. */
	public static final int ESS_LIMIT_14A_ENWG = -4200;

	/**
	 * C-Rate (capacity divided by time) during {@link StateMachine#CHARGE_GRID}.
	 * With a C-Rate of 0.5 the battery gets fully charged within 2 hours.
	 */
	public static final float ESS_CHARGE_C_RATE = 0.5F;

	public static final ChannelAddress SUM_PRODUCTION = new ChannelAddress("_sum", "ProductionActivePower");
	public static final ChannelAddress SUM_CONSUMPTION = new ChannelAddress("_sum", "ConsumptionActivePower");
	public static final ChannelAddress SUM_GRID = new ChannelAddress("_sum", "GridActivePower");
	public static final ChannelAddress SUM_UNMANAGED_CONSUMPTION = new ChannelAddress("_sum",
			"UnmanagedConsumptionActivePower");
	public static final ChannelAddress SUM_ESS_DISCHARGE_POWER = new ChannelAddress("_sum", "EssDischargePower");
	public static final ChannelAddress SUM_ESS_SOC = new ChannelAddress("_sum", "EssSoc");

	protected static final long EXECUTION_LIMIT_SECONDS_BUFFER = 30;
	protected static final long EXECUTION_LIMIT_SECONDS_MINIMUM = 60;

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
		return concat(//
				ctrlLimitTotalDischarges.stream() //
						.map(ctrl -> ctrl.getMinSoc().get()) //
						.filter(Objects::nonNull) //
						.mapToInt(v -> max(0, v)), // only positives
				ctrlEmergencyCapacityReserves.stream() //
						.map(ctrl -> ctrl.getActualReserveSoc().get()) //
						.filter(Objects::nonNull) //
						.mapToInt(v -> max(0, v))) // only positives
				.max().orElse(0);
	}

	public static void filterControllersByEssId(List<ControllerEssChargeDischargeLimiter> controllers, String myEssId) {
	    Iterator<ControllerEssChargeDischargeLimiter> iterator = controllers.iterator();
	    while (iterator.hasNext()) {
	        ControllerEssChargeDischargeLimiter controller = iterator.next();
	        if (!myEssId.equals(controller.getEssId())) { // Check if controller's ESS ID matches
	            iterator.remove(); // Remove controllers that do not refer to the specified ESS
	        }
	    }
	}	
	
	
	/**
	 * Returns a range of useable SoC, e.g. min Soc:20, max SoC 95 -> range 75.
	 * 
	 * @param ctrlChargeDischargeLimiters   the list of
	 *                                      {@link ControllerEssChargeDischargeLimiter}
	 * @param ctrlLimitTotalDischarges      the list of
	 *                                      {@link ControllerEssLimitTotalDischarge}
	 * @param ctrlEmergencyCapacityReserves the list of
	 *                                      {@link ControllerEssEmergencyCapacityReserve}
	 * @return the value in [%]
	 */
	public static int[] getEssUsableSocRange(List<ControllerEssChargeDischargeLimiter> ctrlChargeDischargeLimiters,
			List<ControllerEssLimitTotalDischarge> ctrlLimitTotalDischarges,
			List<ControllerEssEmergencyCapacityReserve> ctrlEmergencyCapacityReserves) {
		// Null checks for lists before processing streams
		int minDischargeSoc = (ctrlLimitTotalDischarges != null)
				? ctrlLimitTotalDischarges.stream().map(ctrl -> ctrl.getMinSoc().orElse(null)) // Defensive null
																								// handling
						.filter(Objects::nonNull).mapToInt(v -> max(0, v)).max().orElse(0)
				: 0; // defaults to 0 if list is null or empty

		int minReserveSoc = (ctrlEmergencyCapacityReserves != null)
				? ctrlEmergencyCapacityReserves.stream().map(ctrl -> ctrl.getActualReserveSoc().orElse(null)) // Defensive
																												// null
																												// handling
						.filter(Objects::nonNull).mapToInt(v -> max(0, v)).max().orElse(0)
				: 0; // defaults to 0 if list is null or empty

		int minLimiterSoc = (ctrlChargeDischargeLimiters != null)
				? ctrlChargeDischargeLimiters.stream().map(ctrl -> ctrl.getMinSoc().orElse(null)) // Defensive null
																									// handling
						.filter(Objects::nonNull).mapToInt(v -> max(0, v)).max().orElse(0)
				: 0; // defaults to 0 if list is null or empty

		// take the max value for min Soc out of three controllers
		int minSoc = max(minDischargeSoc, max(minReserveSoc, minLimiterSoc));

		// get the max. SoC
		int maxSoc = (ctrlChargeDischargeLimiters != null)
				? ctrlChargeDischargeLimiters.stream().map(ctrl -> ctrl.getMaxSoc().orElse(null)) // Defensive null
																									// handling
						.filter(Objects::nonNull).mapToInt(v -> min(100, v)) // no values above 100%
						.min().orElse(100)
				: 100; // defaults to 100 if list is null or empty


		// compute useable SoC range
		return new int[] { minSoc, maxSoc };
	}

	/**
	 * Returns the range of allow SoC. I.e. min: 30%, max: 95% -> 65%.
	 * 
	 * @param ctrlLimitTotalDischarges      the list of
	 *                                      {@link ControllerEssLimitTotalDischarge}
	 * @param ctrlEmergencyCapacityReserves the list of
	 *                                      {@link ControllerEssEmergencyCapacityReserve}
	 * @return the value in [%]
	 */
	public static int getEssSocRangePercentage(List<ControllerEssLimitTotalDischarge> ctrlLimitTotalDischarges,
			List<ControllerEssEmergencyCapacityReserve> ctrlEmergencyCapacityReserves) {
		return concat(//
				ctrlLimitTotalDischarges.stream() //
						.map(ctrl -> ctrl.getMinSoc().get()) //
						.filter(Objects::nonNull) //
						.mapToInt(v -> max(0, v)), // only positives
				ctrlEmergencyCapacityReserves.stream() //
						.map(ctrl -> ctrl.getActualReserveSoc().get()) //
						.filter(Objects::nonNull) //
						.mapToInt(v -> max(0, v))) // only positives
				.max().orElse(0);
	}

}
