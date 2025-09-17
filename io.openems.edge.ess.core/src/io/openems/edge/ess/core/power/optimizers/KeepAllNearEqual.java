package io.openems.edge.ess.core.power.optimizers;

import static io.openems.edge.common.type.Phase.SingleOrAllPhase.ALL;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.math3.optim.PointValuePair;

import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.MetaEss;
import io.openems.edge.ess.core.power.data.TargetDirection;
import io.openems.edge.ess.core.power.solver.nearequal.SolverBySocOptimization;
import io.openems.edge.ess.power.api.Coefficients;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.Inverter;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

public class KeepAllNearEqual {

	/**
	 * Tries to distribute power nearly equal between inverters.
	 *
	 * @param coefficients   the {@link Coefficients}
	 * @param esss           the {@link ManagedSymmetricEss}
	 * @param allInverters   all {@link Inverter}s
	 * @param allConstraints all active {@link Constraint}s
	 * @param direction      the {@link TargetDirection}
	 * @return a solution or null
	 */
	public static PointValuePair apply(Coefficients coefficients, List<ManagedSymmetricEss> esss,
			List<Inverter> allInverters, List<Constraint> allConstraints, TargetDirection direction) {
		if (esss.isEmpty()) {
			return null;
		}

		var setActivePower = getPowerSetPoint(esss, allConstraints, direction, Pwr.ACTIVE);
		var setReactivePower = getPowerSetPoint(esss, allConstraints, direction, Pwr.REACTIVE);

		if (Double.isNaN(setActivePower) && Double.isNaN(setReactivePower)) {
			return null;
		}

		var essList = getGenericEssList(esss);

		var activePowerSolved = solvePowerIfNotNaN(setActivePower, essList, Pwr.ACTIVE, direction);
		var reactivePowerSolved = solvePowerIfNotNaN(setReactivePower, essList, Pwr.REACTIVE, direction);

		var mergedResult = mergeResults(coefficients, esss, activePowerSolved, reactivePowerSolved);
		if (mergedResult == null) {
			return null;
		}

		var result = Arrays.stream(mergedResult)//
				.map(d -> reverseAbsoluteData(d, direction))//
				.toArray();
		if (result.length == 0) {
			return null;
		}
		return new PointValuePair(result, 0);
	}

	/**
	 * Solve it only if the setpower is present.
	 * 
	 * @param setPower  the Setpower
	 * @param essList   the {@link ManagedSymmetricEss}
	 * @param powerType the powerType {@link Pwr}
	 * @param direction the {@link TargetDirection}
	 * @return a solution
	 */
	private static PointValuePair solvePowerIfNotNaN(double setPower, List<ManagedSymmetricEss> essList, Pwr powerType,
			TargetDirection direction) {
		if (Double.isNaN(setPower)) {
			var defaultResult = new double[essList.size()];
			return new PointValuePair(defaultResult, 0);
		} else {
			return solvePower(setPower, essList, powerType, direction);
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
	 * @return new {@link PointValuePair}
	 */
	public static double[] mergeResults(Coefficients coefficients, List<ManagedSymmetricEss> esss,
			PointValuePair activePower, PointValuePair reactivePower) {
		try {
			var values = coefficients.getAll().stream() //
					.mapToDouble(c -> {
						var nonMetaEss = esss.stream() //
								.filter(e -> !(e instanceof MetaEss)) //
								.map(ManagedSymmetricEss::id) //
								.toList();

						var index = nonMetaEss.indexOf(c.getEssId());
						if (index == -1) {
							return 0;
						}

						final var pointValuePair = switch (c.getPwr()) {
						case ACTIVE -> activePower;
						case REACTIVE -> reactivePower;
						};
						return pointValuePair.getPoint()[index];
					}).toArray();
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
	 * @return The optimized solution The {@link PointValuePair}.
	 */
	private static PointValuePair solvePower(double power, List<ManagedSymmetricEss> essList, Pwr pwr,
			TargetDirection direction) {

		if (direction == TargetDirection.KEEP_ZERO) {
			var defaultResult = new double[essList.size()];
			return new PointValuePair(defaultResult, 0);
		}

		var essUpperLimit = essList.stream()//
				.mapToDouble(ess -> getMaxPowerFromEss(ess, pwr))//
				.toArray();

		var size = essUpperLimit.length; // omitting the cluster

		var essLowerLimit = essList.stream()//
				.mapToDouble(ess -> getMinPowerFromEss(ess, pwr))//
				.toArray();

		var model = new SolverBySocOptimization.Builder()//
				.withUpperBound(essUpperLimit)//
				.withLowerBound(essLowerLimit)//
				.withSocs(getSocs(essList))//
				.withPowerSetValue(power)//
				.withTargetDirection(direction)//
				.build();

		try {
			return model.solve(size);
		} catch (Exception e) {
			return null;
		}
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

		var clusterEssId = esss.stream()//
				.filter(MetaEss.class::isInstance)//
				.findFirst().get().id();

		var noPowerSetPoint = Double.NaN;

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
		return ess.getPower().getMaxPower(ess, ALL, pwr);
	}

	/**
	 * Get the minimum power from a {@link ManagedSymmetricEss}.
	 * 
	 * @param ess the {@link ManagedSymmetricEss} *
	 * @param pwr the {@link Pwr} of power
	 * @return the maximum available power in watts for the given parameters
	 */
	private static int getMinPowerFromEss(ManagedSymmetricEss ess, Pwr pwr) {
		return ess.getPower().getMinPower(ess, ALL, pwr);
	}
}