package io.openems.edge.ess.core.power.optimizers;

import static io.openems.edge.common.type.Phase.SingleOrAllPhase.ALL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.math3.optim.PointValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.channel.Level;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.MetaEss;
import io.openems.edge.ess.core.power.data.TargetDirection;
import io.openems.edge.ess.core.power.solver.nearequal.SolverBySocOptimization;
import io.openems.edge.ess.power.api.Coefficient;
import io.openems.edge.ess.power.api.Coefficients;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.Inverter;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

public class KeepAllNearEqual {

	private static final PointValuePair NO_RESULT = null;
	private static final Logger LOG = LoggerFactory.getLogger(KeepAllNearEqual.class);

	/**
	 * Tries to distribute power nearly equal between inverters.
	 *
	 * @param coefficients   the {@link Coefficients}
	 * @param esss           the {@link ManagedSymmetricEss}
	 * @param allInverters   all {@link Inverter}s
	 * @param allConstraints all active {@link Constraint}s
	 * @param direction      the {@link TargetDirection}
	 * @param debugMode      is debugMode set
	 * @return a solution or null
	 */
	public static PointValuePair apply(Coefficients coefficients, List<ManagedSymmetricEss> esss,
			List<Inverter> allInverters, List<Constraint> allConstraints, TargetDirection direction,
			boolean debugMode) {

		if (esss.isEmpty()) {
			return NO_RESULT;
		}

		var setActivePower = getPowerSetPoint(esss, allConstraints, direction, Pwr.ACTIVE);
		var setReactivePower = getPowerSetPoint(esss, allConstraints, direction, Pwr.REACTIVE);

		if (Double.isNaN(setActivePower) && Double.isNaN(setReactivePower)) {
			return NO_RESULT;
		}

		var essList = getGenericEssList(esss);

		var activePowerSolved = solvePowerIfNotNaN(setActivePower, essList, Pwr.ACTIVE, direction, debugMode);
		var reactivePowerSolved = solvePowerIfNotNaN(setReactivePower, essList, Pwr.REACTIVE, direction, debugMode);

		var mergedResult = mergeResults(coefficients, esss, activePowerSolved, reactivePowerSolved);
		if (mergedResult == null) {
			return NO_RESULT;
		}

		try {
			var result = Arrays.stream(mergedResult)//
					.map(d -> reverseAbsoluteData(d, direction))//
					.toArray();
			return new PointValuePair(result, 0);
		} catch (Exception e) {
			return NO_RESULT;
		}
	}

	/**
	 * Solve it only if the setpower is present.
	 * 
	 * @param setPower  the Setpower
	 * @param essList   the {@link ManagedSymmetricEss}
	 * @param powerType the powerType {@link Pwr}
	 * @param direction the {@link TargetDirection}
	 * @param debugMode is debugMode set
	 * @return a solution
	 */
	private static PointValuePair solvePowerIfNotNaN(double setPower, List<ManagedSymmetricEss> essList, Pwr powerType,
			TargetDirection direction, boolean debugMode) {
		if (Double.isNaN(setPower)) {
			var defaultResult = new double[essList.size()];
			return new PointValuePair(defaultResult, 0);
		} else {
			return solvePower(setPower, essList, powerType, direction, debugMode);
		}
	}

	/**
	 * Merges results of Active-Power and Reactive-Power to a {@link PointValuePair}
	 * compatible with default Solver.
	 * 
	 * @param coefficients  the {@link Coefficients}
	 * @param esss          the {@link ManagedSymmetricEss}s
	 * @param activePower   the {@link PointValuePair} for Active-Power
	 * @param reactivePower the {@link PointValuePair} for Reactive-Power
	 * @return new double array of merged results, or null if failed
	 */
	public static double[] mergeResults(Coefficients coefficients, List<ManagedSymmetricEss> esss,
			PointValuePair activePower, PointValuePair reactivePower) {
		try {
			List<Coefficient> allCoefficients = coefficients.getAll();
			double[] values = new double[allCoefficients.size()];

			List<String> healthyEssIds = new ArrayList<>();
			for (ManagedSymmetricEss ess : esss) {
				if (!(ess instanceof MetaEss) && !(ess.getState() == Level.FAULT)) {
					healthyEssIds.add(ess.id());
				}
			}

			// Iterate through coefficients and resolve values
			for (int i = 0; i < allCoefficients.size(); i++) {
				Coefficient c = allCoefficients.get(i);
				String essId = c.getEssId();

				boolean isInFault = false;
				for (ManagedSymmetricEss ess : esss) {
					if (essId.equals(ess.id()) && ess.getState() == Level.FAULT) {
						isInFault = true;
						break;
					}
				}

				if (isInFault) {
					// Set 0 for ESS in FAULT state
					values[i] = 0;
				} else {
					int index = healthyEssIds.indexOf(essId);
					if (index == -1) {
						values[i] = 0;
					} else {
						PointValuePair pointValuePair = switch (c.getPwr()) {
						case ACTIVE -> activePower;
						case REACTIVE -> reactivePower;
						};
						values[i] = pointValuePair.getPoint()[index];
					}
				}
			}

			return values;
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Solves the power optimization problem for a given set of coefficients, energy
	 * storage systems (ESSs), inverters, constraints, description, power type, and
	 * target direction.
	 *
	 * @param power     the power to be distributed
	 * @param essList   the {@link ManagedSymmetricEss}s
	 * @param pwr       The {@link Pwr}.
	 * @param direction The {@link TargetDirection}.
	 * @param debugMode is debugMode set
	 * @return The optimized solution The {@link PointValuePair}.
	 */
	private static PointValuePair solvePower(double power, List<ManagedSymmetricEss> essList, Pwr pwr,
			TargetDirection direction, boolean debugMode) {

		if (direction == TargetDirection.KEEP_ZERO) {
			var defaultResult = new double[essList.size()];
			return new PointValuePair(defaultResult, 0);
		}

		// Upper limits
		var essUpperLimit = essList.stream()//
				.map(ess -> new EssPower(ess.id(), getMaxPowerFromEss(ess, pwr))) //
				.mapToDouble(EssPower::power)//
				.toArray();

		// Lower limits
		var essLowerLimit = essList.stream()//
				.map(ess -> new EssPower(ess.id(), getMinPowerFromEss(ess, pwr))) //
				.mapToDouble(EssPower::power)//
				.toArray();

		var socList = getSocs(essList);

		// Solver
		var model = new SolverBySocOptimization.Builder()// 3
				.withUpperBound(essUpperLimit)//
				.withLowerBound(essLowerLimit)//
				.withSocs(socList)//
				.withPowerSetValue(power)//
				.withTargetDirection(direction)//
				.build();

		try {
			var result = model.solve(essUpperLimit.length);
			if (result == null) {
				return NO_RESULT;
			}

			if (debugMode) {
				String summary = SolverBySocOptimization.getCompactSolverSummary(//
						socList, // SOC distribution
						essUpperLimit, // upper bounds
						result.getPoint(), // solution values
						essLowerLimit, // lower bounds
						power // power set value
				);
				LOG.info(summary);
			}

			// Validate the solution respects individual ESS bounds
			if (!isValidSolution(result.getPoint(), essUpperLimit, essLowerLimit, power, direction)) {
				// cannot solve fall back to next solver
				return NO_RESULT;
			}

			return result;
		} catch (Exception e) {
			return NO_RESULT;
		}
	}

	private record EssPower(String id, double power) {
	}

	/**
	 * Validates that the solver solution respects individual ESS power limits.
	 * 
	 * @param solution       the solver solution array
	 * @param upperLimit     the upper bounds (discharge limits)
	 * @param lowerLimit     the lower bounds (charge limits)
	 * @param requestedPower the requested total power
	 * @param direction      the target direction
	 * @return true if solution is valid, false otherwise
	 */
	private static boolean isValidSolution(double[] solution, double[] upperLimit, double[] lowerLimit,
			double requestedPower, TargetDirection direction) {

		// Check individual ESS limits
		for (int i = 0; i < solution.length; i++) {
			if (direction == TargetDirection.CHARGE) {
				// For charging: solution is absolute value that will be negated
				// Check if -solution[i] would be within charge bounds [lowerLimit, 0]
				double chargeValue = -solution[i];
				if (solution[i] < 0 || chargeValue < lowerLimit[i]) {
					return false;
				}
			} else if (direction == TargetDirection.DISCHARGE) {
				// For discharging: solution should be between 0 and upperLimit
				if (solution[i] < 0 || solution[i] > upperLimit[i]) {
					return false;
				}
			}
		}

		// Check total power matches request (within tolerance)
		double totalPower = Arrays.stream(solution).sum();

		// For charge direction, we need to compare absolute values since solution will
		// be negated later
		double expectedTotal = (direction == TargetDirection.CHARGE) ? Math.abs(requestedPower) : requestedPower;

		var tolerance = 1000; // 1kW tolerance

		return !(Math.abs(totalPower - expectedTotal) > tolerance);
	}

	/**
	 * Get the Soc array from the esslist.
	 * 
	 * @param essList list of {@link ManagedSymmetricEss}
	 * @return double array of Soc
	 */
	private static double[] getSocs(List<ManagedSymmetricEss> essList) {
		return essList.stream()//
				.mapToDouble(ess -> ess.getSoc().get())//
				.toArray();
	}

	private static double getPowerSetPoint(List<ManagedSymmetricEss> esss, List<Constraint> allConstraints,
			TargetDirection direction, Pwr pwr) {

		var noPowerSetPoint = Double.NaN;

		var clusterEssId = esss.stream()//
				.filter(MetaEss.class::isInstance)//
				.findFirst()//
				.map(ManagedSymmetricEss::id)//
				.orElse(null);

		if (clusterEssId == null) {
			return noPowerSetPoint;
		}

		return allConstraints.stream()//
				.filter(constraint -> constraint.getRelationship() == Relationship.EQUALS)
				.filter(constraint -> constraint.getCoefficients().length == 1)
				.filter(constraint -> clusterEssId.equals(constraint.getCoefficients()[0].getCoefficient().getEssId()))
				.filter(constraint -> constraint.getCoefficients()[0].getCoefficient().getPwr() == pwr)
				.mapToDouble(constraint -> constraint.getValue().get())//
				.map(c -> absoluteData(c, direction))//
				.findFirst()//
				.orElse(noPowerSetPoint);
	}

	private static List<ManagedSymmetricEss> getGenericEssList(List<ManagedSymmetricEss> esss) {
		return esss.stream()//
				.filter(e -> !(e instanceof MetaEss))//
				.filter(e -> !(e.getState() == Level.FAULT))//
				.toList();
	}

	/**
	 * Calculate absolute value or zero based on the TargetDirection.
	 * 
	 * @param d         the input value to be processed
	 * @param direction the {@link TargetDirection}
	 * @return the processed value based on the direction
	 */
	private static double absoluteData(double d, TargetDirection direction) {
		return switch (direction) {
		case CHARGE -> Math.abs(d);
		case DISCHARGE -> d;
		case KEEP_ZERO -> 0.0;
		};
	}

	/**
	 * Calculate reverse absolute value or zero based on the TargetDirection.
	 * 
	 * @param d         the input value to be processed
	 * @param direction the {@link TargetDirection}
	 * @return the processed value based on the direction
	 */
	private static double reverseAbsoluteData(double d, TargetDirection direction) {
		return switch (direction) {
		case CHARGE -> -d;
		case DISCHARGE -> d;
		case KEEP_ZERO -> 0.0;
		};
	}

	/**
	 * Get the maximum power from a {@link ManagedSymmetricEss}.
	 * 
	 * @param ess the {@link ManagedSymmetricEss}
	 * @param pwr the {@link Pwr} of power
	 * @return the maximum available power in watts for the given parameters
	 */
	private static int getMaxPowerFromEss(ManagedSymmetricEss ess, Pwr pwr) {
		var maxPower = ess.getPower().getMaxPower(ess, ALL, pwr);
		if (maxPower > 0) {
			return maxPower;
		}

		// When constraint system gives negative/zero, use raw ESS discharge limit
		try {
			var allowedDischargeValue = ess.getAllowedDischargePower().getOrError();
			if (allowedDischargeValue > 0) {
				return allowedDischargeValue;
			}
		} catch (Exception e) {
			// Value not available, use fallback
		}
		return 1; // Fallback if null, invalid, or not available
	}

	/**
	 * Get the minimum power from a {@link ManagedSymmetricEss}.
	 * 
	 * @param ess the {@link ManagedSymmetricEss}
	 * @param pwr the {@link Pwr} of power
	 * @return the minimum available power in watts for the given parameters
	 */
	private static int getMinPowerFromEss(ManagedSymmetricEss ess, Pwr pwr) {
		var minPower = ess.getPower().getMinPower(ess, ALL, pwr);
		if (minPower <= 0) {
			return minPower;
		}

		// When constraint system gives positive, use raw ESS charge limit
		try {
			var allowedChargeValue = ess.getAllowedChargePower().getOrError();
			if (allowedChargeValue < 0) {
				return allowedChargeValue;
			}
		} catch (Exception e) {
			// Value not available, use fallback
		}
		return -1; // Fallback if null, invalid, or not available
	}
}