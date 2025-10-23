package io.openems.edge.controller.io.heatingelement;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZonedDateTime;

import com.google.common.collect.ImmutableList;

import io.openems.common.jscalendar.JSCalendar;
import io.openems.common.jscalendar.JSCalendar.Task;
import io.openems.edge.controller.io.heatingelement.enums.Level;
import io.openems.edge.timedata.api.utils.CalculateActiveTime;

public class Utils {

	private Utils() {
	}

	protected static record HighPeriod(Instant from, Instant to, Integer payload) {
	}

	protected static HighPeriod getNextHighPeriod(ZonedDateTime now, ImmutableList<Task<Payload>> schedule) {
		return JSCalendar.Tasks.getNextOccurence(schedule, now).map(ot -> {
			Integer payload = null;
			if (ot.payload() != null) {
				payload = ot.payload().sessionEnergy();
			}
			return new HighPeriod(ot.start().toInstant(), ot.start().plus(ot.duration()).toInstant(), payload);
		}).orElse(null);
	}

	public static record HeatingPhases(PhaseDef phase1, PhaseDef phase2, PhaseDef phase3) {
	}

	public static record CumulatedActiveTimes(CalculateActiveTime totalTimeLevel1, CalculateActiveTime totalTimeLevel2,
			CalculateActiveTime totalTimeLevel3) {
	}

	/**
	 * Calculates the required level based from a power value of a phase.
	 * 
	 * @param excessPower   the excess power in Watt
	 * @param powerPerPhase the configurable power value of a phase in Watt
	 * @return the required level
	 */
	public static Level getRequiredLevelFromPower(long excessPower, int powerPerPhase) {
		if (excessPower >= powerPerPhase * 3L) {
			return Level.LEVEL_3;
		} else if (excessPower >= powerPerPhase * 2L) {
			return Level.LEVEL_2;
		} else if (excessPower >= powerPerPhase) {
			return Level.LEVEL_1;
		} else {
			return Level.LEVEL_0;
		}
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
