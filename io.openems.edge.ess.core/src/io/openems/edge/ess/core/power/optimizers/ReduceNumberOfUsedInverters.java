package io.openems.edge.ess.core.power.optimizers;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.optim.PointValuePair;

import com.google.common.collect.Lists;

import io.openems.common.function.ThrowingFunction;
import io.openems.edge.ess.core.power.data.TargetDirection;
import io.openems.edge.ess.power.api.Inverter;

public class ReduceNumberOfUsedInverters {

	private TargetDirection activeTargetDirection = null;
	private int targetDirectionChangedSince = 0;
	private int lastLowestTrueIndex = -1;

	/**
	 * Finds the Inverters that are minimally required to fulfill all Constraints.
	 *
	 * <p>
	 * This method removes inverters till it finds a minimum setup. It uses an
	 * algorithm similarly to binary tree search to find the minimum required number
	 * of inverters.
	 *
	 * @param allInverters     a list of all inverters
	 * @param targetDirection  the target direction
	 * @param validateFunction a function that can tell if a setup is solvable with
	 *                         a given list of disabled Inverters.
	 * @return a list of target inverters
	 */
	public List<Inverter> apply(List<Inverter> allInverters, TargetDirection targetDirection,
			ThrowingFunction<List<Inverter>, PointValuePair, Exception> validateFunction) {
		// Only zero or one inverters available? No need to optimize.
		if (allInverters.size() < 2) {
			return allInverters;
		}

		// Change target direction only once in a while
		if (this.activeTargetDirection == null || this.targetDirectionChangedSince > 100) {
			this.activeTargetDirection = targetDirection;
		}
		if (this.activeTargetDirection != targetDirection) {
			this.targetDirectionChangedSince++;
		} else {
			this.targetDirectionChangedSince = 0;
		}

		// Inverters are by default sorted by weight descending. For DISCHARGE take list
		// as it is; for CHARGE reverse it. This prefers high-weight inverters (e.g.
		// high state-of-charge) on DISCHARGE and low-weight
		// inverters (e.g. low state-of-charge) on CHARGE.
		List<Inverter> sortedInverters;
		if (this.activeTargetDirection == TargetDirection.DISCHARGE) {
			sortedInverters = allInverters;
		} else {
			sortedInverters = Lists.reverse(allInverters);
		}

		/**
		 * Keeps tested solutions for enabled inverters:
		 *
		 * <ul>
		 * <li>null -> still needs to be tried
		 * <li>false -> this solution is not feasible
		 * <li>true -> this solution is feasible
		 * </ul>
		 */
		var testedSolutions = new Boolean[sortedInverters.size()];

		while (true) {
			// find first and last untested index
			var firstUntestedIndex = -1;
			for (var i = 0; i < testedSolutions.length; i++) {
				if (testedSolutions[i] == null) {
					firstUntestedIndex = i;
					break;
				}
			}
			var lastUntestedIndex = -1;
			for (var i = testedSolutions.length - 1; i > -1; i--) {
				if (testedSolutions[i] == null) {
					lastUntestedIndex = i;
					break;
				}
			}

			if (firstUntestedIndex == -1 || lastUntestedIndex == -1) {
				// No untested solution left? -> finished
				break;
			}

			final int testIndex;
			if (firstUntestedIndex == 0 && lastUntestedIndex == testedSolutions.length - 1
					&& this.lastLowestTrueIndex != -1) {
				// reload best result of last run; if this run is similar, this approach will
				// save some time
				testIndex = this.lastLowestTrueIndex;
			} else {
				testIndex = (firstUntestedIndex + lastUntestedIndex) / 2;
			}

			try {
				validateFunction.apply(getDisabledInverters(sortedInverters, testIndex));

				// solved successfully
				for (var i = lastUntestedIndex; i >= testIndex; i--) {
					testedSolutions[i] = true;
				}

			} catch (Exception e) {

				// solved unsuccessfully
				for (var i = firstUntestedIndex; i <= testIndex; i++) {
					testedSolutions[i] = false;
				}

			}
		}

		// lowestTrueIndex is the optimal solution
		var lowestTrueIndex = -1;
		for (var i = 0; i < testedSolutions.length; i++) {
			if (testedSolutions[i] == Boolean.TRUE) {
				lowestTrueIndex = i;
				break;
			}
		}
		this.lastLowestTrueIndex = lowestTrueIndex;

		// build result
		List<Inverter> result = new ArrayList<>(allInverters);
		if (lowestTrueIndex == -1) {
			// no solution -> do not disable any Inverters
		} else {
			var disabledInverters = getDisabledInverters(sortedInverters, lowestTrueIndex);
			for (Inverter disabledInverter : disabledInverters) {
				result.remove(disabledInverter);
			}
		}

		// get result in the order of preferred usage
		if (this.activeTargetDirection == TargetDirection.CHARGE) {
			result = Lists.reverse(result);
		}
		return result;
	}

	private static List<Inverter> getDisabledInverters(List<Inverter> allInverters, int index) {
		return allInverters.subList(index + 1, allInverters.size());
	}

}
