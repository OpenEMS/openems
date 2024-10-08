package io.openems.edge.energy.optimizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableSortedMap;

import io.jenetics.Genotype;
import io.jenetics.IntegerChromosome;
import io.jenetics.IntegerGene;
import io.openems.edge.energy.api.EnergyScheduleHandler;
import io.openems.edge.energy.api.simulation.GlobalSimulationsContext;

/**
 * This class helps finding good Schedules that are quickly available.
 */
public class QuickSchedules {

	private QuickSchedules() {
	}

	/**
	 * Finds the best quick Schedule, i.e. the one with the lowest cost.
	 * 
	 * @param cache            the {@link GenotypeCache}
	 * @param gsc              the {@link GlobalSimulationsContext}
	 * @param simulationResult the existing {@link SimulationResult}, or null
	 * @return the winner {@link Genotype}; or null
	 */
	public static Genotype<IntegerGene> findBestQuickSchedule(GenotypeCache cache, GlobalSimulationsContext gsc,
			SimulationResult simulationResult) {
		double lowestCost = 0.;
		Genotype<IntegerGene> bestGt = null;
		for (var gt : generateQuickSchedules(gsc, simulationResult)) {
			var cost = Simulator.calculateCost(cache, gsc, gt);
			if (bestGt == null || cost < lowestCost) {
				bestGt = gt;
				lowestCost = cost;
			}
		}
		return bestGt;
	}

	/**
	 * Generate quick Schedules.
	 * 
	 * @param gsc              the {@link GlobalSimulationsContext}
	 * @param simulationResult the existing {@link SimulationResult}, or null
	 * @return a List of {@link Genotype}s, entries can be null
	 */
	public static List<Genotype<IntegerGene>> generateQuickSchedules(GlobalSimulationsContext gsc,
			SimulationResult simulationResult) {
		return Stream //
				.concat(//
						variationsOfAllStatesDefault(gsc), //
						variationsFromExistingSimulationResult(gsc, simulationResult)) //
				.filter(Objects::nonNull) //
				.distinct() //
				.toList();
	}

	/**
	 * Builds {@link Genotype}s with all states default and all possible variations
	 * for first period.
	 * 
	 * @param gsc the {@link GlobalSimulationsContext}
	 * @return a Stream of {@link Genotype}s or nulls
	 */
	protected static Stream<Genotype<IntegerGene>> variationsOfAllStatesDefault(GlobalSimulationsContext gsc) {
		return generateAllCombinations(gsc).stream() //
				.map(combination -> toGenotypeOrNull(gsc.handlers().stream() //
						.filter(EnergyScheduleHandler.WithDifferentStates.class::isInstance) //
						.map(EnergyScheduleHandler.WithDifferentStates.class::cast) //
						.map(esh -> {
							final var defaultState = esh.getDefaultStateIndex();
							final var noOfStates = esh.getAvailableStates().length;
							final var firstState = combination.get(esh);
							return IntegerChromosome.of(IntStream.range(0, gsc.periods().size()) //
									.map(i -> i == 0 //
											? firstState // first period
											: defaultState) // remaining periods
									.mapToObj(state -> IntegerGene.of(state, 0, noOfStates)) //
									.toList());
						}) //
						.toList()));
	}

	/**
	 * Builds {@link Genotype}s with all states from an existing
	 * {@link SimulationResult} and all possible variations for first period.
	 * 
	 * @param gsc              the {@link GlobalSimulationsContext}
	 * @param simulationResult the {@link SimulationResult}
	 * @return a Stream of {@link Genotype}s or nulls
	 */
	protected static Stream<Genotype<IntegerGene>> variationsFromExistingSimulationResult(GlobalSimulationsContext gsc,
			SimulationResult simulationResult) {
		return generateAllCombinations(gsc).stream() //
				.map(combination -> toGenotypeOrNull(gsc.handlers().stream() //
						.filter(EnergyScheduleHandler.WithDifferentStates.class::isInstance) //
						.map(EnergyScheduleHandler.WithDifferentStates.class::cast) //
						.map(esh -> {
							final var firstState = combination.get(esh);
							final var existingSchedule = simulationResult.schedules().getOrDefault(esh,
									ImmutableSortedMap.of());
							final var defaultState = esh.getDefaultStateIndex();
							final var noOfStates = esh.getAvailableStates().length;
							return IntegerChromosome.of(IntStream.range(0, gsc.periods().size()) //
									.map(i -> {
										if (i == 0) { //
											return firstState; // first period
										}
										// remaining periods
										var period = gsc.periods().get(i);
										var previousState = existingSchedule.get(period.time());
										if (previousState != null) {
											return previousState.stateIndex();
										}
										return defaultState;
									}) //
									.mapToObj(state -> IntegerGene.of(state, 0, noOfStates)) //
									.toList());
						}) //
						.toList()));
	}

	/**
	 * Builds a {@link Genotype} of an existing {@link SimulationResult}.
	 * 
	 * @param gsc              the {@link GlobalSimulationsContext}
	 * @param simulationResult the {@link SimulationResult}
	 * @return the {@link Genotype} or null
	 */
	protected static Genotype<IntegerGene> fromExistingSimulationResult(GlobalSimulationsContext gsc,
			SimulationResult simulationResult) {
		if (simulationResult == null) {
			return null;
		}
		return toGenotypeOrNull(gsc.handlers().stream() //
				.filter(EnergyScheduleHandler.WithDifferentStates.class::isInstance) //
				.map(EnergyScheduleHandler.WithDifferentStates.class::cast) //
				.map(esh -> {
					final var existingSchedule = simulationResult.schedules().getOrDefault(esh,
							ImmutableSortedMap.of());
					final var defaultState = esh.getDefaultStateIndex();
					final var noOfStates = esh.getAvailableStates().length;
					return IntegerChromosome.of(gsc.periods().stream() //
							.map(p -> {
								var previousState = existingSchedule.get(p.time());
								var state = previousState == null //
										? defaultState //
										: previousState.stateIndex();
								return IntegerGene.of(state, 0, noOfStates);
							}) //
							.toList());
				}) //
				.toList());
	}

	private static Genotype<IntegerGene> toGenotypeOrNull(List<IntegerChromosome> cs) {
		if (cs.isEmpty()) {
			return null;
		}
		return Genotype.of(cs);
	}

	/**
	 * Generates all possible combinations of
	 * {@link EnergyScheduleHandler.WithDifferentStates} and state-index for a
	 * Period.
	 * 
	 * @param gsc {@link GlobalSimulationsContext}
	 * @return combinations
	 */
	@SuppressWarnings("rawtypes")
	private static List<Map<EnergyScheduleHandler.WithDifferentStates, Integer>> generateAllCombinations(
			GlobalSimulationsContext gsc) {
		if (gsc == null) {
			return List.of();
		}

		var eshs = gsc.handlers().stream() //
				.filter(EnergyScheduleHandler.WithDifferentStates.class::isInstance) //
				.map(EnergyScheduleHandler.WithDifferentStates.class::cast) //
				.toList();

		List<Map<EnergyScheduleHandler.WithDifferentStates, Integer>> result = new ArrayList<>();
		generateCombinationsRecursive(eshs, 0, new HashMap<>(), result);
		return result;
	}

	@SuppressWarnings("rawtypes")
	private static void generateCombinationsRecursive(List<EnergyScheduleHandler.WithDifferentStates> inputList,
			int index, Map<EnergyScheduleHandler.WithDifferentStates, Integer> currentCombination,
			List<Map<EnergyScheduleHandler.WithDifferentStates, Integer>> result) {
		// Base case: If we've added a combination for each input, add the result to the
		// list.
		if (index == inputList.size()) {
			result.add(new HashMap<>(currentCombination)); // Add a copy of the current map
			return;
		}

		// Get the current input
		var currentInput = inputList.get(index);

		// Loop through all possible values for this input, from 0 to maxValue
		for (int value = 0; value < currentInput.getAvailableStates().length; value++) {
			currentCombination.put(currentInput, value); // Set this value in the map
			// Recur to the next input
			generateCombinationsRecursive(inputList, index + 1, currentCombination, result);
			currentCombination.remove(currentInput); // Backtrack
		}
	}
}
