package io.openems.edge.ess.core.power.optimizers;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.IntStream;

import org.apache.commons.math3.optim.PointValuePair;

import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.MetaEss;
import io.openems.edge.ess.core.power.data.TargetDirection;
import io.openems.edge.ess.core.power.solver.nearequal.SolveNearEqual;
import io.openems.edge.ess.power.api.Coefficients;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.Inverter;
import io.openems.edge.ess.power.api.Phase;
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

		var result = Arrays.stream(mergedResult)//
				.map(d -> reverseAbsoluteData.apply(d, direction))//
				.toArray();

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
			Arrays.fill(defaultResult, 0.0);
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
	private static double[] mergeResults(Coefficients coefficients, List<ManagedSymmetricEss> esss,
			PointValuePair activePower, PointValuePair reactivePower) {
		try {
			var values = coefficients.getAll().stream() //
					.mapToDouble(c -> {
						var nonMetaEss = esss.stream() //
								.filter(e -> !(e instanceof MetaEss)) //
								.map(ManagedSymmetricEss::id) //
								.toList();
						var indexOpt = IntStream.range(0, nonMetaEss.size()) //
								.filter(i -> nonMetaEss.get(i) == c.getEssId()) //
								.findFirst();
						if (indexOpt.isEmpty()) {
							return 0;
						}
						var map = switch (c.getPwr()) {
						case ACTIVE -> activePower;
						case REACTIVE -> reactivePower;
						};
						return map.getPoint()[indexOpt.getAsInt()];
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

		var selectedFunction = switch (direction) {
		case CHARGE -> getMinPowerFromEss;
		case DISCHARGE -> getMaxPowerFromEss;
		case KEEP_ZERO -> getZero;
		};

		var essUpperLimit = essList.stream()//
				.mapToDouble(ess -> {
					return selectedFunction.apply(ess, pwr);
				})//
				.boxed()//
				.toList();

		var size = essUpperLimit.size(); // omitting the cluster

		var essLowerLimit = essList.stream()//
				.mapToDouble(ess -> {
					var lowerLimit = selectedFunction.apply(ess, pwr);
					return (Math.abs(lowerLimit) >= power / size) ? 0.0 : lowerLimit - (lowerLimit * 0.01);
				})//
				.boxed()//
				.toList();

		var model = new SolveNearEqual();
		model.setUpperBound(getData(essUpperLimit, direction));
		model.setLowerBound(getData(essLowerLimit, direction));
		model.setpowerSetValue(power);

		try {
			return model.solve(size);
		} catch (Exception e) {
			return null;
		}
	}

	private static double getPowerSetPoint(List<ManagedSymmetricEss> esss, List<Constraint> allConstraints,
			TargetDirection direction, Pwr pwr) {

		var clusterEssId = esss.stream()//
				.filter(ess -> ess instanceof MetaEss)//
				.findFirst().get().id();

		var noPowerSetPoint = Double.NaN;

		return allConstraints.stream()//
				.filter(constraint -> constraint.getRelationship() == Relationship.EQUALS)
				.filter(constraint -> constraint.getCoefficients().length == 1)
				.filter(constraint -> clusterEssId.equals(constraint.getCoefficients()[0].getCoefficient().getEssId()))
				.filter(constraint -> constraint.getCoefficients()[0].getCoefficient().getPwr() == pwr)
				.mapToDouble(constraint -> constraint.getValue().get())//
				.map(c -> absoluteData.apply(c, direction))//
				.findFirst()//
				.orElse(noPowerSetPoint);
	}

	private static List<ManagedSymmetricEss> getGenericEssList(List<ManagedSymmetricEss> esss) {
		return esss.stream()//
				.filter(e -> !(e instanceof MetaEss))//
				.toList();
	}

	/**
	 * Gets data from a list and applies a transformation based on the specified
	 * target direction.
	 *
	 * @param listData  The list of Double data.
	 * @param direction The {@link TargetDirection}
	 * @return An array of transformed data.
	 */
	private static double[] getData(List<Double> listData, TargetDirection direction) {
		return listData.stream()//
				.mapToDouble(d -> absoluteData.apply(d, direction))//
				.toArray();
	}

	/**
	 * Function to calculate absolute value or zero based on the
	 * {@link TargetDirection}.
	 */
	private static BiFunction<Double, TargetDirection, Double> absoluteData = (d, direction) -> {
		return switch (direction) {
		case CHARGE -> Math.abs(d);
		case DISCHARGE -> d;
		case KEEP_ZERO -> 0.0;
		};
	};

	/**
	 * Function to calculate reverse absolute value or zero based on the
	 * {@link TargetDirection}.
	 */
	private static BiFunction<Double, TargetDirection, Double> reverseAbsoluteData = (d, direction) -> {
		return switch (direction) {
		case CHARGE -> -d;
		case DISCHARGE -> d;
		case KEEP_ZERO -> 0.0;
		};
	};

	/**
	 * Function to get the maximum power from a {@link ManagedSymmetricEss}.
	 */
	private static BiFunction<ManagedSymmetricEss, Pwr, Integer> getMaxPowerFromEss = (ess, pwr) -> {
		return ess.getPower().getMaxPower(ess, Phase.ALL, pwr);
	};

	/**
	 * Function to get the minimum power from a {@link ManagedSymmetricEss}.
	 */
	private static BiFunction<ManagedSymmetricEss, Pwr, Integer> getMinPowerFromEss = (ess, pwr) -> {
		return ess.getPower().getMinPower(ess, Phase.ALL, pwr);
	};

	/**
	 * Function to return zero.
	 */
	private static BiFunction<ManagedSymmetricEss, Pwr, Integer> getZero = (ess, pwr) -> {
		return 0;
	};

}
