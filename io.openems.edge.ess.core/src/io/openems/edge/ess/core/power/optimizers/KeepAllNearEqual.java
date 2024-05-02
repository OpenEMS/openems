package io.openems.edge.ess.core.power.optimizers;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
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

public class KeepAllNearEqual {

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
				.filter(d -> !d.isNaN())//
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

		var activePowerSolved = solvePower(coefficients, esss, allInverters, allConstraints, //
				"SetActivePowerEquals", Pwr.ACTIVE, direction);

		var reactivePowerSolved = solvePower(coefficients, esss, allInverters, allConstraints, //
				"SetReactivePowerEquals", Pwr.REACTIVE, direction);

		var mergedResult = mergeResults(coefficients, esss, activePowerSolved, reactivePowerSolved);

		var result = Arrays.stream(mergedResult)//
				.map(d -> reverseAbsoluteData.apply(d, direction)).toArray();

		return new PointValuePair(result, 0);
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
	 * @param coefficients   the {@link Coefficients}
	 * @param esss           the {@link ManagedSymmetricEss}s
	 * @param allInverters   The {@link Inverter}s
	 * @param allConstraints The {@link Constraint}s
	 * @param description    The description.
	 * @param pwr            The {@link Pwr}.
	 * @param direction      The {@link TargetDirection}.
	 * @return The optimized solution The {@link PointValuePair}.
	 */
	private static PointValuePair solvePower(Coefficients coefficients, List<ManagedSymmetricEss> esss,
			List<Inverter> allInverters, List<Constraint> allConstraints, String description, Pwr pwr,
			TargetDirection direction) {

		BiFunction<ManagedSymmetricEss, Pwr, Integer> selectedFunction = switch (direction) {
		case CHARGE -> getMinPowerFromEss;
		case DISCHARGE -> getMaxPowerFromEss;
		case KEEP_ZERO -> getZero;
		};

		var setPower = allConstraints.stream()//
				.filter(c -> c.getDescription().equals(description))//
				.mapToDouble(c -> c.getValue().get())//
				.map(c -> absoluteData.apply(c, direction))//
				.findFirst()//
				.orElse(0.0);

		var essUpperLimit = esss.stream()//
				.mapToDouble(ess -> {
					return ess instanceof MetaEss ? Double.NaN : selectedFunction.apply(ess, pwr);
				})//
				.boxed()//
				.collect(Collectors.toList());

		var essLowerLimit = esss.stream()//
				.mapToDouble(ess -> {
					if (ess instanceof MetaEss) {
						return Double.NaN;
					} else {
						var lowerLimit = selectedFunction.apply(ess, pwr);
						return (lowerLimit == setPower) ? 0.0 : lowerLimit - (lowerLimit * 0.01);
					}
				})//
				.boxed()//
				.collect(Collectors.toList());

		var model = new SolveNearEqual();
		model.setUpperBound(getData(essUpperLimit, direction));
		model.setLowerBound(getData(essLowerLimit, direction));
		model.setpowerSetValue(setPower);

		var size = essUpperLimit.size() - 1; // omitting the cluster

		try {
			return model.solve(size);
		} catch (Exception e) {
			return null;
		}
	}
}
