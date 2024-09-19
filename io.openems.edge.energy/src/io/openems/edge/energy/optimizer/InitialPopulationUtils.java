package io.openems.edge.energy.optimizer;

import java.util.stream.IntStream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;

import io.jenetics.Genotype;
import io.jenetics.IntegerChromosome;
import io.jenetics.IntegerGene;
import io.openems.edge.energy.api.EnergyScheduleHandler;
import io.openems.edge.energy.api.simulation.GlobalSimulationsContext;

public class InitialPopulationUtils {

	private InitialPopulationUtils() {
	}

	/**
	 * Builds an initial population:
	 * 
	 * <ol>
	 * <li>Schedule with all periods default
	 * <li>Schedule from lastSchedules, i.e. the result of last optimization run
	 * </ol>
	 * 
	 * <p>
	 * NOTE: providing an "all periods default" Schedule as first Genotype makes
	 * sure, that this one wins in case there are other results with same cost, e.g.
	 * when battery never gets empty anyway.
	 * 
	 * @param gsc            the {@link GlobalSimulationsContext} of the current run
	 * @param previousResult the {@link SimulationResult} of the previous
	 *                       optimization run
	 * @return the {@link Genotype}
	 */
	public static ImmutableList<Genotype<IntegerGene>> buildInitialPopulation(GlobalSimulationsContext gsc,
			SimulationResult previousResult) {
		var b = ImmutableList.<Genotype<IntegerGene>>builder(); //

		// All default
		b.add(allStatesDefault(gsc));

		// Existing Schedule
		b.add(Genotype.of(gsc.handlers().stream() //
				.filter(EnergyScheduleHandler.WithDifferentStates.class::isInstance) //
				.map(EnergyScheduleHandler.WithDifferentStates.class::cast) //
				.map(esh -> {
					final var previousSchedule = previousResult.schedules().getOrDefault(esh, ImmutableSortedMap.of());
					final var defaultState = esh.getDefaultStateIndex();
					final var noOfStates = esh.getAvailableStates().length;
					return IntegerChromosome.of(gsc.periods().stream() //
							.map(p -> {
								var previousState = previousSchedule.get(p.time());
								var state = previousState == null //
										? defaultState //
										: previousState.stateIndex();
								return IntegerGene.of(state, 0, noOfStates);
							}) //
							.toList());
				}) //
				.toList()));

		return b.build();
	}

	protected static Genotype<IntegerGene> allStatesDefault(GlobalSimulationsContext gsc) {
		return Genotype.of(gsc.handlers().stream() //
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
}
