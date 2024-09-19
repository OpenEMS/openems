package io.openems.edge.energy.optimizer;

import java.util.List;
import java.util.stream.IntStream;

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
	 * Generate quick Schedules.
	 * 
	 * @param gsc              the {@link GlobalSimulationsContext}
	 * @param simulationResult the existing {@link SimulationResult}, or null
	 * @return a List of {@link Genotype}s, entries can be null
	 */
	public static List<Genotype<IntegerGene>> generateQuickSchedules(GlobalSimulationsContext gsc,
			SimulationResult simulationResult) {
		return List.of(//
				allStatesDefault(gsc), //
				fromExistingSimulationResult(gsc, simulationResult) //
		);
	}

	/**
	 * Builds a {@link Genotype} with all states default. `null` if no
	 * {@link EnergyScheduleHandler}s match the criteria.
	 * 
	 * @param gsc the {@link GlobalSimulationsContext}
	 * @return the {@link Genotype} or null
	 */
	protected static Genotype<IntegerGene> allStatesDefault(GlobalSimulationsContext gsc) {
		return toGenotypeOrNull(gsc.handlers().stream() //
				.filter(EnergyScheduleHandler.WithDifferentStates.class::isInstance) //
				.map(EnergyScheduleHandler.WithDifferentStates.class::cast) //
				.map(esh -> {
					final var defaultState = esh.getDefaultStateIndex();
					final var noOfStates = esh.getAvailableStates().length;
					return IntegerChromosome.of(IntStream.range(0, gsc.periods().size()) //
							.mapToObj(i -> IntegerGene.of(defaultState, 0, noOfStates)) //
							.toList());
				}) //
				.toList());
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

}
