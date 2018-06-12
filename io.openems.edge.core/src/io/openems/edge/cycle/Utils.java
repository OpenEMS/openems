package io.openems.edge.cycle;

import java.util.Optional;
import java.util.TreeMap;

import io.openems.edge.scheduler.api.Scheduler;

public class Utils {

	/**
	 * Called on change of Scheduler list: recalculates the commonCycleTime
	 * 
	 * @param schedulers
	 * @return commonCycleTime
	 */
	protected static int recalculateCommonCycleTime(TreeMap<Scheduler, Integer> schedulers) {
		// find greatest common divisor -> commonCycleTime
		int[] cycleTimes = new int[schedulers.size()];
		{
			int i = 0;
			for (Scheduler scheduler : schedulers.keySet()) {
				cycleTimes[i++] = scheduler.getCycleTime();
			}
		}
		return Utils.getGreatestCommonDivisor(cycleTimes).orElse(Scheduler.DEFAULT_CYCLE_TIME);
	}

	/**
	 * Called on change of Scheduler list: recalculates the commonCycleTime
	 * 
	 * @param schedulers.
	 *            "relativeCycleTime" is being updated for every scheduler
	 * @param commonCycleTime
	 * @return least common multiple of relativeCycleTimes (maxCycles)
	 */
	protected static int recalculateRelativeCycleTimes(TreeMap<Scheduler, Integer> schedulers, int commonCycleTime) {
		// fix relative cycleTime for all existing schedulers
		int[] relativeCycleTimes = new int[schedulers.size()];
		{
			int i = 0;
			for (Scheduler scheduler : schedulers.keySet()) {
				int relativeCycleTime = scheduler.getCycleTime() / commonCycleTime;
				schedulers.put(scheduler, relativeCycleTime);
				relativeCycleTimes[i++] = relativeCycleTime;
			}
		}
		// find least common multiple of relativeCycleTimes
		return Utils.getLeastCommonMultiple(relativeCycleTimes).orElse(1);
	}

	/**
	 * 
	 */

	// Source: https://stackoverflow.com/a/4202114/4137113
	protected static int getGreatestCommonDivisor(int a, int b) {
		while (b > 0) {
			int temp = b;
			b = a % b; // % is remainder
			a = temp;
		}
		return a;
	}

	protected static Optional<Integer> getGreatestCommonDivisor(int[] input) {
		if (input.length == 0) {
			return Optional.empty();
		}
		int result = input[0];
		for (int i = 1; i < input.length; i++) {
			result = getGreatestCommonDivisor(result, input[i]);
		}
		return Optional.of(result);
	}

	private static int getLeastCommonMultiple(int a, int b) {
		return a * (b / getGreatestCommonDivisor(a, b));
	}

	protected static Optional<Integer> getLeastCommonMultiple(int[] input) {
		if (input.length == 0) {
			return Optional.empty();
		}
		int result = input[0];
		for (int i = 1; i < input.length; i++) {
			result = getLeastCommonMultiple(result, input[i]);
		}
		return Optional.of(result);
	}
}
