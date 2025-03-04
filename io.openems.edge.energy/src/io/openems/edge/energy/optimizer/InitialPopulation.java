package io.openems.edge.energy.optimizer;

import static io.jenetics.util.ISeq.toISeq;

import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Lists;

import io.jenetics.Gene;
import io.jenetics.Genotype;
import io.jenetics.IntegerGene;
import io.jenetics.util.ISeq;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler;
import io.openems.edge.energy.api.handler.EshWithDifferentModes;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext;

/**
 * This class helps finding good initial populations.
 */
public class InitialPopulation {

	private InitialPopulation() {
	}

	/**
	 * Generate initial population.
	 * 
	 * @param goc                  the {@link GlobalOptimizationContext}
	 * @param codec                the {@link EshCodec}
	 * @param previousResult       the {@link SimulationResult} of the previous
	 *                             optimization run
	 * @param isCurrentPeriodFixed fixes the {@link Gene} of the current period to
	 *                             the previousResult
	 * @return a List of {@link Genotype}s, entries can be null
	 */
	public static ISeq<Genotype<IntegerGene>> generateInitialPopulation(GlobalOptimizationContext goc, EshCodec codec,
			SimulationResult previousResult, boolean isCurrentPeriodFixed) {
		for (var energyScheduleHandler : goc.eshsWithDifferentModes()) {
			if (energyScheduleHandler instanceof EshWithDifferentModes esh) {
				esh.getInitialPopulations(goc);
			}
		}

		// TODO read good variations from ESHs.
		// Example: force charge car during cheapest hours
		return Stream //
				.concat(//
						variationsOfAllModesDefault(goc, previousResult, isCurrentPeriodFixed), //
						variationsFromExistingSimulationResult(goc, previousResult, isCurrentPeriodFixed)) //
				.filter(Objects::nonNull) //
				.distinct() //
				.map(codec::encode) //
				.collect(toISeq());
	}

	/**
	 * Builds Schedules with all Modes default and all possible variations for first
	 * adjustable period(s).
	 * 
	 * @param goc                  the {@link GlobalOptimizationContext}
	 * @param previousResult       the {@link SimulationResult} of the previous
	 *                             optimization run
	 * @param isCurrentPeriodFixed fixes the Mode of the first (current) period to
	 *                             the previousResult
	 * @return a Stream of Schedules
	 */
	protected static Stream<int[][]> variationsOfAllModesDefault(GlobalOptimizationContext goc,
			SimulationResult previousResult, boolean isCurrentPeriodFixed) {
		return generateAllVariations(goc) //
				.map(variation -> IntStream.range(0, goc.periods().size()) //
						.mapToObj(periodIndex -> {
							final var period = goc.periods().get(periodIndex);

							return IntStream.range(0, goc.eshsWithDifferentModes().size()) //
									.map(eshIndex -> {
										final var esh = goc.eshsWithDifferentModes().get(eshIndex);
										final var previousSchedule = previousResult.schedules()
												.getOrDefault(esh, ImmutableSortedMap.of()).get(period.time());

										if (periodIndex == 0 && isCurrentPeriodFixed && previousSchedule != null) {
											return previousSchedule.modeIndex(); // from previous result
										} else if (periodIndex < 2) {
											return variation.get(eshIndex); // variation of first period(s)
										} else {
											return esh.getDefaultModeIndex(); // ESH default Mode
										}
									}).toArray(); //
						}) //
						.toArray(int[][]::new));
	}

	/**
	 * Builds Schedules with all Modes from an existing {@link SimulationResult} and
	 * all possible variations for first adjustable period.
	 * 
	 * @param goc                  the {@link GlobalOptimizationContext}
	 * @param previousResult       the {@link SimulationResult} of the previous
	 *                             optimization run
	 * @param isCurrentPeriodFixed fixes the Mode of the first (current) period to
	 *                             the previousResult
	 * @return a Stream of Schedules
	 */
	protected static Stream<int[][]> variationsFromExistingSimulationResult(GlobalOptimizationContext goc,
			SimulationResult previousResult, boolean isCurrentPeriodFixed) {
		return generateAllVariations(goc) //
				.map(variation -> IntStream.range(0, goc.periods().size()) //
						.mapToObj(periodIndex -> {
							final var period = goc.periods().get(periodIndex);

							return IntStream.range(0, goc.eshsWithDifferentModes().size()) //
									.map(eshIndex -> {
										final var esh = goc.eshsWithDifferentModes().get(eshIndex);
										final var previousSchedule = previousResult.schedules()
												.getOrDefault(esh, ImmutableSortedMap.of()).get(period.time());

										if (((periodIndex == 0 && isCurrentPeriodFixed) || periodIndex > 1)
												&& previousSchedule != null) {
											return previousSchedule.modeIndex(); // from previous result
										} else if (periodIndex < 2) {
											return variation.get(eshIndex); // variation of first period(s)
										} else {
											return esh.getDefaultModeIndex(); // ESH default Mode
										}
									}).toArray(); //
						}) //
						.toArray(int[][]::new));

	}

	/**
	 * Generates all possible variations of
	 * {@link EnergyScheduleHandler.WithDifferentModes} indexes and Mode-index for a
	 * Period.
	 * 
	 * @param goc the {@link GlobalOptimizationContext}
	 * @return variations
	 */
	private static Stream<List<Integer>> generateAllVariations(GlobalOptimizationContext goc) {
		return Lists.cartesianProduct(//
				goc.eshsWithDifferentModes().stream() //
						.map(esh -> IntStream //
								.range(0, esh.getNumberOfAvailableModes()) //
								.mapToObj(Integer::valueOf) //
								.toList()) //
						.<List<Integer>>toArray(List[]::new) //
		).stream();
	}
}
