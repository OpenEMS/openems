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
import io.openems.edge.energy.api.EnergyScheduleHandler;
import io.openems.edge.energy.api.simulation.GlobalSimulationsContext;

/**
 * This class helps finding good initial populations.
 */
public class InitialPopulation {

	private InitialPopulation() {
	}

	/**
	 * Generate initial population.
	 * 
	 * @param gsc                  the {@link GlobalSimulationsContext}
	 * @param codec                the {@link EshCodec}
	 * @param previousResult       the {@link SimulationResult} of the previous
	 *                             optimization run
	 * @param isCurrentPeriodFixed fixes the {@link Gene} of the current period to
	 *                             the previousResult
	 * @return a List of {@link Genotype}s, entries can be null
	 */
	public static ISeq<Genotype<IntegerGene>> generateInitialPopulation(GlobalSimulationsContext gsc, EshCodec codec,
			SimulationResult previousResult, boolean isCurrentPeriodFixed) {
		return Stream //
				.concat(//
						variationsOfAllStatesDefault(gsc, previousResult, isCurrentPeriodFixed), //
						variationsFromExistingSimulationResult(gsc, previousResult, isCurrentPeriodFixed)) //
				.filter(Objects::nonNull) //
				.distinct() //
				.map(codec::encode) //
				.collect(toISeq());
	}

	/**
	 * Builds Schedules with all states default and all possible variations for
	 * first adjustable period(s).
	 * 
	 * @param gsc                  the {@link GlobalSimulationsContext}
	 * @param previousResult       the {@link SimulationResult} of the previous
	 *                             optimization run
	 * @param isCurrentPeriodFixed fixes the state of the first (current) period to
	 *                             the previousResult
	 * @return a Stream of Schedules
	 */
	protected static Stream<int[][]> variationsOfAllStatesDefault(GlobalSimulationsContext gsc,
			SimulationResult previousResult, boolean isCurrentPeriodFixed) {
		return generateAllVariations(gsc) //
				.map(variation -> IntStream.range(0, gsc.periods().size()) //
						.mapToObj(periodIndex -> {
							final var period = gsc.periods().get(periodIndex);

							return IntStream.range(0, gsc.eshsWithDifferentStates().size()) //
									.map(eshIndex -> {
										final var esh = gsc.eshsWithDifferentStates().get(eshIndex);
										final var previousSchedule = previousResult.schedules()
												.getOrDefault(esh, ImmutableSortedMap.of()).get(period.time());

										if (periodIndex == 0 && isCurrentPeriodFixed && previousSchedule != null) {
											return previousSchedule.stateIndex(); // from previous result
										} else if (periodIndex < 2) {
											return variation.get(eshIndex); // variation of first period(s)
										} else {
											return esh.getDefaultStateIndex(); // ESH default state
										}
									}).toArray(); //
						}) //
						.toArray(int[][]::new));
	}

	/**
	 * Builds Schedules with all states from an existing {@link SimulationResult}
	 * and all possible variations for first adjustable period.
	 * 
	 * @param gsc                  the {@link GlobalSimulationsContext}
	 * @param previousResult       the {@link SimulationResult} of the previous
	 *                             optimization run
	 * @param isCurrentPeriodFixed fixes the state of the first (current) period to
	 *                             the previousResult
	 * @return a Stream of Schedules
	 */
	protected static Stream<int[][]> variationsFromExistingSimulationResult(GlobalSimulationsContext gsc,
			SimulationResult previousResult, boolean isCurrentPeriodFixed) {
		return generateAllVariations(gsc) //
				.map(variation -> IntStream.range(0, gsc.periods().size()) //
						.mapToObj(periodIndex -> {
							final var period = gsc.periods().get(periodIndex);

							return IntStream.range(0, gsc.eshsWithDifferentStates().size()) //
									.map(eshIndex -> {
										final var esh = gsc.eshsWithDifferentStates().get(eshIndex);
										final var previousSchedule = previousResult.schedules()
												.getOrDefault(esh, ImmutableSortedMap.of()).get(period.time());

										if (((periodIndex == 0 && isCurrentPeriodFixed) || periodIndex > 1)
												&& previousSchedule != null) {
											return previousSchedule.stateIndex(); // from previous result
										} else if (periodIndex < 2) {
											return variation.get(eshIndex); // variation of first period(s)
										} else {
											return esh.getDefaultStateIndex(); // ESH default state
										}
									}).toArray(); //
						}) //
						.toArray(int[][]::new));

	}

	/**
	 * Generates all possible variations of
	 * {@link EnergyScheduleHandler.WithDifferentStates} indexes and state-index for
	 * a Period.
	 * 
	 * @param gsc {@link GlobalSimulationsContext}
	 * @return variations
	 */
	private static Stream<List<Integer>> generateAllVariations(GlobalSimulationsContext gsc) {
		return Lists.cartesianProduct(//
				gsc.eshsWithDifferentStates().stream() //
						.map(esh -> IntStream //
								.range(0, esh.getAvailableStates().length) //
								.mapToObj(Integer::valueOf) //
								.toList()) //
						.<List<Integer>>toArray(List[]::new) //
		).stream();
	}
}
