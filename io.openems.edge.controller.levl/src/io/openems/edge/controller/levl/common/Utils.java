package io.openems.edge.controller.levl.common;

import static io.openems.edge.common.type.TypeUtils.fitWithin;
import static java.lang.Math.min;
import static java.lang.Math.round;

import java.util.UUID;
import java.util.function.Consumer;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.utils.JsonUtils;

public class Utils {

	public static final double HUNDRED_PERCENT = 100.0;
	public static final double MILLISECONDS_PER_SECOND = 1000.0;
	public static final long SECONDS_PER_HOUR = 3600L;

	private Utils() {
	}

	/**
	 * Applies an efficiency to a power/energy outside of the battery.
	 * 
	 * <p>
	 * Negative values for charge; positive for discharge
	 * 
	 * @param value             power/energy to which the efficiency should be
	 *                          applied
	 * @param efficiencyPercent efficiency which should be applied
	 * @return the power/energy inside the battery after applying the efficiency
	 */
	public static long applyEfficiency(long value, double efficiencyPercent) {
		if (value <= 0) { // charge
			return multiplyByEfficiency(value, efficiencyPercent);
		}

		// discharge
		return divideByEfficiency(value, efficiencyPercent);
	}

	/**
	 * Unapplies an efficiency to a power/energy inside of the battery.
	 * 
	 * <p>
	 * Negative values for charge; positive for discharge
	 * 
	 * @param value             power/energy to which the efficiency should be
	 *                          unapplied
	 * @param efficiencyPercent efficiency which should be unapplied
	 * @return the power/energy outside the battery after unapplying the efficiency
	 */
	public static long unapplyEfficiency(long value, double efficiencyPercent) {
		if (value <= 0) { // charge
			return divideByEfficiency(value, efficiencyPercent);
		}

		// discharge
		return multiplyByEfficiency(value, efficiencyPercent);
	}

	private static long divideByEfficiency(long value, double efficiencyPercent) {
		return Math.round(value / (efficiencyPercent / 100));
	}

	private static long multiplyByEfficiency(long value, double efficiencyPercent) {
		return Math.round(value * efficiencyPercent / 100);
	}

	/**
	 * Calculates the soc of the primary use case based on physical soc and tracked
	 * levl soc.
	 * 
	 * @param physicalSocWs the physical soc [Ws]
	 * @param levlSocWs     the levl soc [Ws]
	 * @param essCapacityWs the ess capacity [Ws]
	 * 
	 * @return the soc of the primary use case
	 */
	public static long calculatePucSoc(long physicalSocWs, long levlSocWs, long essCapacityWs) {
		var pucSoc = physicalSocWs - levlSocWs;

		// handle case of pucSoc out of bounds (e.g. due to rounding)
		if (pucSoc < 0) {
			return 0;
		} else if (pucSoc > essCapacityWs) {
			return essCapacityWs;
		}
		return pucSoc;
	}

	/**
	 * Calculates the power of the primary use case, taking into account the ess
	 * power limits and the soc limits.
	 * 
	 * <p>
	 * Positive = discharge; negative = charge
	 * 
	 * @param log           the debug logger
	 * @param gridPower     the active power of the meter [W]
	 * @param essPower      the active power of the ess [W]
	 * @param pucSocWs      the soc of the puc [Ws]
	 * @param essCapacityWs the ess capacity [Ws]
	 * @param minEssPower   the minimum possible power of the ess [W]
	 * @param maxEssPower   the maximum possible power of the ess [W]
	 * @param efficiency    the efficiency of the system [%]
	 * @param cycleTimeS    the configured openems cycle time [seconds]
	 * 
	 * @return the puc battery power for the next cycle [W]
	 */
	public static int calculatePucBatteryPower(Consumer<String> log, int gridPower, int essPower, long pucSocWs,
			long essCapacityWs, int minEssPower, int maxEssPower, double efficiency, double cycleTimeS) {
		log.accept("### calculatePucBatteryPower ###");
		log.accept(String.format(
				"Parameters: gridPower=%d, essPower=%d, pucSocWs=%d, essCapacityWs=%d, minEssPower=%d, "
						+ "maxEssPower=%d, efficiency=%.2f, cycleTimeS=%.2f",
				gridPower, essPower, pucSocWs, essCapacityWs, minEssPower, maxEssPower, efficiency, cycleTimeS));
		// calculate pucPower without any limits
		var pucBatteryPower = gridPower + essPower;
		log.accept("pucBatteryPower without limits: " + pucBatteryPower);

		// apply ess power limits
		pucBatteryPower = fitWithin(minEssPower, maxEssPower, pucBatteryPower);
		log.accept("pucBatteryPower with ess power limits: " + pucBatteryPower);

		// apply soc bounds
		pucBatteryPower = applyPucSocBounds(pucBatteryPower, pucSocWs, essCapacityWs, efficiency, cycleTimeS);
		log.accept("pucBatteryPower with ess power and soc limits: " + pucBatteryPower);
		return pucBatteryPower;
	}

	/**
	 * Checks and corrects the puc battery power if it would exceed the upper or
	 * lower limits of the soc.
	 * 
	 * @param pucPower      the calculated pucPower [W]
	 * @param pucSocWs      the soc of the puc [Ws]
	 * @param essCapacityWs the ess capacity [Ws]
	 * @param efficiency    the efficiency of the system [%]
	 * @param cycleTimeS    the configured openems cycle time [seconds]
	 * @return the restricted pucPower [W]
	 */
	private static int applyPucSocBounds(int pucPower, long pucSocWs, long essCapacityWs, double efficiency,
			double cycleTimeS) {
		var dischargeEnergyLowerBoundWs = pucSocWs - essCapacityWs;
		var dischargeEnergyUpperBoundWs = pucSocWs;

		var powerLowerBound = unapplyEfficiency(round(dischargeEnergyLowerBoundWs / cycleTimeS), efficiency);
		var powerUpperBound = unapplyEfficiency(round(dischargeEnergyUpperBoundWs / cycleTimeS), efficiency);

		if (powerLowerBound > 0) {
			powerLowerBound = 0;
		}
		if (powerUpperBound < 0) {
			powerUpperBound = 0;
		}

		return (int) fitWithin(powerLowerBound, powerUpperBound, pucPower);
	}

	/**
	 * Calculates the battery power for the levl use case considering various
	 * constraints.
	 * 
	 * <p>
	 * positive = discharge; negative = charge
	 * 
	 * @param log                      the debug logger
	 * @param remainingLevlEnergyWs    the remaining energy that has to be realized
	 *                                 for levl [Ws]
	 * @param pucBatteryPower          the puc battery power [W]
	 * @param minEssPower              the minimum possible power of the ess [W]
	 * @param maxEssPower              the maximum possible power of the ess [W]
	 * @param pucGridPower             the active power of the puc on the meter [W]
	 * @param buyFromGridLimit         maximum power that may be bought from the
	 *                                 grid [W]
	 * @param sellToGridLimit          maximum power that may be sold to the grid
	 *                                 [W]
	 * @param nextPucSocWs             the calculated puc soc for the next cycle
	 *                                 [Ws]
	 * @param levlSocWs                the current levl soc [Ws]
	 * @param socLowerBoundLevlPercent the lower levl soc limit [%]
	 * @param socUpperBoundLevlPercent the upper levl soc limit [%]
	 * @param essCapacityWs            the ess capacity [Ws]
	 * @param influenceSellToGrid      whether it's allowed to influence sell to
	 *                                 grid
	 * @param efficiency               the efficiency of the system [%]
	 * @param cycleTimeS               the configured openems cycle time [seconds]
	 * @return the levl battery power [W]
	 */
	public static int calculateLevlBatteryPower(Consumer<String> log, long remainingLevlEnergyWs, int pucBatteryPower,
			int minEssPower, int maxEssPower, int pucGridPower, long buyFromGridLimit, long sellToGridLimit,
			long nextPucSocWs, long levlSocWs, double socLowerBoundLevlPercent, double socUpperBoundLevlPercent,
			long essCapacityWs, boolean influenceSellToGrid, double efficiency, double cycleTimeS) {

		log.accept("### calculateLevlBatteryPowerW ###");
		log.accept(String.format(
				"Parameters: remainingLevlEnergyWs=%d, pucBatteryPower=%d, minEssPower=%d, maxEssPower=%d, "
						+ "pucGridPower=%d, buyFromGridLimit=%d, sellToGridLimit=%d, nextPucSocWs=%d, levlSocWs=%d, "
						+ "socLowerBoundLevlPercent=%.2f, socUpperBoundLevlPercent=%.2f, essCapacityWs=%d, "
						+ "influenceSellToGrid=%b, efficiency=%.2f, cycleTimeS=%.2f",
				remainingLevlEnergyWs, pucBatteryPower, minEssPower, maxEssPower, pucGridPower, buyFromGridLimit,
				sellToGridLimit, nextPucSocWs, levlSocWs, socLowerBoundLevlPercent, socUpperBoundLevlPercent,
				essCapacityWs, influenceSellToGrid, efficiency, cycleTimeS));

		var levlPower = round(remainingLevlEnergyWs / (double) cycleTimeS);
		log.accept("Initial levlPower: " + levlPower);

		levlPower = applyBatteryPowerLimitsToLevlPower(levlPower, pucBatteryPower, minEssPower, maxEssPower);
		log.accept("LevlPower after applyBatteryPowerLimits: " + levlPower);

		levlPower = applySocBoundariesToLevlPower(levlPower, nextPucSocWs, levlSocWs, socLowerBoundLevlPercent,
				socUpperBoundLevlPercent, essCapacityWs, efficiency, cycleTimeS);
		log.accept("LevlPower after applySocBoundaries: " + levlPower);

		levlPower = applyGridPowerLimitsToLevlPower(levlPower, pucGridPower, buyFromGridLimit, sellToGridLimit);
		log.accept("LevlPower after applyGridPowerLimits: " + levlPower);

		levlPower = applyInfluenceSellToGridConstraint(levlPower, pucGridPower, influenceSellToGrid);
		log.accept("LevlPower after applyInfluenceSellToGridConstraint: " + levlPower);

		return (int) levlPower;
	}

	/**
	 * Applies battery power limits to the levl power.
	 * 
	 * @param levlPower       the levl battery power [W]
	 * @param pucBatteryPower the puc battery power [W]
	 * @param minEssPower     the minimum possible power of the ess [W]
	 * @param maxEssPower     the maximum possible power of the ess [W]
	 * @return the restricted levl battery power [W]
	 */
	private static long applyBatteryPowerLimitsToLevlPower(long levlPower, int pucBatteryPower, int minEssPower,
			int maxEssPower) {
		var levlPowerLowerBound = Long.valueOf(minEssPower) - pucBatteryPower;
		var levlPowerUpperBound = Long.valueOf(maxEssPower) - pucBatteryPower;
		return fitWithin(levlPowerLowerBound, levlPowerUpperBound, levlPower);
	}

	/**
	 * Applies influence sell to grid constraint to the levl power.
	 * 
	 * @param levlPower           the levl battery power [W]
	 * @param pucGridPower        the active power of the puc on the meter [W]
	 * @param influenceSellToGrid whether it's allowed to influence sell to grid
	 * @return the restricted levl battery power [W]
	 */
	private static long applyInfluenceSellToGridConstraint(long levlPower, int pucGridPower,
			boolean influenceSellToGrid) {
		if (!influenceSellToGrid) {
			if (pucGridPower < 0) {
				// if primary use case sells to grid, levl isn't allowed to do anything
				levlPower = 0;
			} else {
				// if primary use case buys from grid, levl can sell maximum this amount to grid
				levlPower = min(levlPower, pucGridPower);
			}
		}
		return levlPower;
	}

	/**
	 * Applies upper and lower soc bounderies to the levl power.
	 * 
	 * @param levlPower                the levl battery power [W]
	 * @param nextPucSocWs             the calculated puc soc for the next cycle
	 *                                 [Ws]
	 * @param levlSocWs                the current levl soc [Ws]
	 * @param socLowerBoundLevlPercent the lower levl soc limit [%]
	 * @param socUpperBoundLevlPercent the upper levl soc limit [%]
	 * @param essCapacityWs            the ess capacity [Ws]
	 * @param efficiency               the efficiency of the system [%]
	 * @param cycleTimeS               the configured openems cycle time [seconds]
	 * @return the restricted levl battery power [W]
	 */
	private static long applySocBoundariesToLevlPower(long levlPower, long nextPucSocWs, long levlSocWs,
			double socLowerBoundLevlPercent, double socUpperBoundLevlPercent, long essCapacityWs, double efficiency,
			double cycleTimeS) {
		var levlSocLowerBoundWs = round(socLowerBoundLevlPercent / HUNDRED_PERCENT * essCapacityWs) - nextPucSocWs;
		var levlSocUpperBoundWs = round(socUpperBoundLevlPercent / HUNDRED_PERCENT * essCapacityWs) - nextPucSocWs;

		if (levlSocLowerBoundWs > 0) {
			levlSocLowerBoundWs = 0;
		}
		if (levlSocUpperBoundWs < 0) {
			levlSocUpperBoundWs = 0;
		}

		var levlDischargeEnergyLowerBoundWs = -(levlSocUpperBoundWs - levlSocWs);
		var levlDischargeEnergyUpperBoundWs = -(levlSocLowerBoundWs - levlSocWs);

		var levlPowerLowerBound = unapplyEfficiency(round(levlDischargeEnergyLowerBoundWs / cycleTimeS), efficiency);
		var levlPowerUpperBound = unapplyEfficiency(round(levlDischargeEnergyUpperBoundWs / cycleTimeS), efficiency);

		return fitWithin(levlPowerLowerBound, levlPowerUpperBound, levlPower);
	}

	/**
	 * Applies grid power limits to the levl power.
	 * 
	 * @param levlPower        the levl battery power [W]
	 * @param pucGridPower     the active power of the puc on the meter [W]
	 * @param buyFromGridLimit maximum power that may be bought from the grid [W]
	 * @param sellToGridLimit  maximum power that may be sold to the grid [W]
	 * @return the restricted levl battery power [W]
	 */
	private static long applyGridPowerLimitsToLevlPower(long levlPower, int pucGridPower, long buyFromGridLimit,
			long sellToGridLimit) {
		var levlPowerLowerBound = -(buyFromGridLimit - pucGridPower);
		var levlPowerUpperBound = -(sellToGridLimit - pucGridPower);
		return fitWithin(levlPowerLowerBound, levlPowerUpperBound, levlPower);
	}

	/**
	 * Generates a JsonRpc Response.
	 * 
	 * @param requestId     the request ID
	 * @param levlRequestId the levl request ID
	 * @return the {@link JsonrpcResponseSuccess}
	 * @throws OpenemsNamedException on error
	 */
	public static JsonrpcResponseSuccess generateResponse(UUID requestId, String levlRequestId)
			throws OpenemsNamedException {
		return new GenericJsonrpcResponseSuccess(requestId, JsonUtils.buildJsonObject() //
				.addProperty("levlRequestId", levlRequestId) //
				.build());
	}
}