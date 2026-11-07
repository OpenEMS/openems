package io.openems.edge.controller.io.heatingelement;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;

import io.openems.edge.controller.io.heatingelement.enums.Level;
import io.openems.edge.timedata.api.utils.CalculateActiveTime;

public class Utils {

	private Utils() {
	}

	public static record HeatingPhases(PhaseDef phase1, PhaseDef phase2, PhaseDef phase3) {
	}

	public static record CumulatedActiveTimes(CalculateActiveTime totalTimeLevel1, CalculateActiveTime totalTimeLevel2,
			CalculateActiveTime totalTimeLevel3, CalculateActiveTime totalTimeLevel4,
			CalculateActiveTime totalTimeLevel5, CalculateActiveTime totalTimeLevel6) {
	}

	/**
	 * Calculates the required Level based on the cumulated nominal power of the
	 * configured {@link HeatingElementStep}s.
	 *
	 * <p>
	 * The result is the highest Level whose cumulated Step power (i.e. the sum of
	 * the nominal power of the first N Steps) is still covered by the given excess
	 * power. This works for any number of Steps and any (possibly asymmetric) Step
	 * power, e.g. the 3 Steps of a single heating element or the 6 Steps of two
	 * heating elements in SEQUENTIAL or PARALLEL Distribution.
	 *
	 * @param excessPower the excess power in Watt
	 * @param steps       the ordered {@link HeatingElementStep}s
	 * @return the required level
	 */
	public static Level getRequiredLevelFromPower(long excessPower, List<HeatingElementStep> steps) {
		var levels = Level.values();
		var requiredLevel = Level.LEVEL_0;
		var cumulatedPower = 0L;
		for (var i = 0; i < steps.size(); i++) {
			cumulatedPower += steps.get(i).nominalPower();

			if (excessPower < cumulatedPower) {
				break;
			}

			requiredLevel = levels[i + 1];
		}
		return requiredLevel;
	}

	/**
	 * Calculates the required level based on the average power values from the
	 * phases.
	 * 
	 * @param excessPower the excess power in Watt
	 * @param phases      the phases
	 * @return the required level
	 */
	public static Level getRequiredLevelFromAvgPowers(long excessPower, HeatingPhases phases) {
		if (excessPower >= getTotalAvgPower(phases)) {
			return Level.LEVEL_3;
		} else if (excessPower >= phases.phase1().getAvgPower() + phases.phase2().getAvgPower()) {
			return Level.LEVEL_2;
		} else if (excessPower >= phases.phase1().getAvgPower()) {
			return Level.LEVEL_1;
		} else {
			return Level.LEVEL_0;
		}
	}

	/**
	 * Calculates the requiredPower to switch the levels.
	 *
	 * @param energyLimit        the energy limit in Wh
	 * @param sessionEnergy      the current session energy in Wh
	 * @param totalRemainingTime the remaining time for the session
	 * @return the requiredPower in Watt
	 */
	public static long getRequiredPower(long energyLimit, long sessionEnergy, Duration totalRemainingTime) {

		long remainingTimeInSec;
		if (isFinal10Minutes(totalRemainingTime)) {
			remainingTimeInSec = Math.max(totalRemainingTime.getSeconds(), 1);
		} else {
			remainingTimeInSec = totalRemainingTime.getSeconds() - 600;
		}
		double requiredPower = (energyLimit - sessionEnergy) * 3600 / (double) remainingTimeInSec;
		return Math.round(requiredPower / 10.0) * 10;
	}

	/**
	 * Calculates the time on the mode Energy the heating element should be forced
	 * heating.
	 *
	 * @param energyLimit   the energy limit in Wh
	 * @param sessionEnergy the current session energy in Wh
	 * @param phases        the phases
	 * @param endTime       the end time to reach the energy limit
	 * @param now           current time
	 * @return the time that the heating element should force to heat with one
	 *         phase.
	 */
	public static LocalTime getTimeToForceHeat(long energyLimit, long sessionEnergy, HeatingPhases phases,
			LocalTime endTime, LocalTime now) {
		double restConsumption = energyLimit - sessionEnergy;

		if (restConsumption <= 0) {
			return null;
		}

		/*
		 * Calculates the totalTime to reach the minimum limit by heating with the first
		 * phase, if there is a value in the AvgPower. If not, it will use the second
		 * phase or resp. third phase. If there is no AvgPower of any phase it returns
		 * null.
		 */
		double phasePower;
		if (phases.phase1().getAvgPower() != 0) {
			phasePower = phases.phase1().getAvgPower();
		} else if (phases.phase2().getAvgPower() != 0) {
			phasePower = phases.phase2().getAvgPower();
		} else if (phases.phase3().getAvgPower() != 0) {
			phasePower = phases.phase3().getAvgPower();
		} else {
			return null;
		}

		double totalTime = restConsumption / phasePower;

		var totalTimeInMinutes = totalTime * 60;
		// adding a 10 minutes buffer
		totalTimeInMinutes = Math.ceil(totalTimeInMinutes) + 10;
		var latestTime = endTime.minusMinutes((int) totalTimeInMinutes);

		if (latestTime.isBefore(now)) {
			return null;
		}
		return latestTime;
	}

	/**
	 * A helper method to check if the duration is equal or less than 10 minutes.
	 *
	 * @param totalRemainingTime the duration
	 * @return a boolean value, true if it's equal or less than 10 minutes
	 */
	public static boolean isFinal10Minutes(Duration totalRemainingTime) {
		return totalRemainingTime.getSeconds() <= 600;
	}

	/**
	 * A helper method to get the total average power of the phases.
	 *
	 * @param phases the phases
	 * @return the total average power in Watt
	 */
	public static int getTotalAvgPower(HeatingPhases phases) {
		return phases.phase1.getAvgPower() + phases.phase2.getAvgPower() + phases.phase3.getAvgPower();
	}
}
