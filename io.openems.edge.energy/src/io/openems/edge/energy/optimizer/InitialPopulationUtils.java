package io.openems.edge.energy.optimizer;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static io.jenetics.util.ISeq.toISeq;

import java.time.ZonedDateTime;
import java.util.Map.Entry;
import java.util.function.Consumer;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Lists;

import io.jenetics.IntegerGene;
import io.jenetics.engine.EvolutionInit;
import io.openems.edge.energy.api.handler.DifferentModes.InitialPopulation;
import io.openems.edge.energy.api.handler.DifferentModes.InitialPopulation.Transition;
import io.openems.edge.energy.api.handler.DifferentModes.Period;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler.WithDifferentModes;
import io.openems.edge.energy.api.handler.EshWithDifferentModes;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext;

/**
 * This class helps finding good initial populations.
 */
public class InitialPopulationUtils {

	private InitialPopulationUtils() {
	}

	/**
	 * Generate initial population.
	 * 
	 * @param codec the {@link EshCodec}
	 * @return a {@link EvolutionInit}
	 */
	public static EvolutionInit<IntegerGene> generateInitialPopulation(EshCodec codec) {
		var ipsPerEsh = codec.goc.eshsWithDifferentModes().stream() //
				.filter(EshWithDifferentModes.class::isInstance) //
				.map(EshWithDifferentModes.class::cast) //
				.map(esh -> generateInitialPopulationPerEsh(codec, esh).stream() //
						.collect(toImmutableList())) //
				.collect(toImmutableList());

		return EvolutionInit.of(//
				Lists.cartesianProduct(ipsPerEsh).stream() //
						.map(ips -> codec.goc.periods().stream() //
								.map(p -> ips.stream() //
										.mapToInt(ip -> p.index() < ip.modeIndexes().length //
												? ip.modeIndexes()[p.index()] //
												: 0) // fallback
										.toArray()) //
								.toArray(int[][]::new)) //
						.map(codec::encode) //
						.collect(toISeq()),
				1 /* first generation */);
	}

	protected static ImmutableSortedMap<ZonedDateTime, Period.Transition> getScheduleFromPreviousResult(
			WithDifferentModes esh, SimulationResult previousResult) {
		return previousResult.schedules().entrySet().stream() //
				.filter(e -> e.getKey().getId().equals(esh.getId())) //
				.map(Entry::getValue) //
				.findFirst().orElse(null);
	}

	private static Period.Transition getCurrentPeriodFromSchedule(GlobalOptimizationContext goc, WithDifferentModes esh,
			ImmutableSortedMap<ZonedDateTime, Period.Transition> schedule) {
		if (schedule == null) {
			return null;
		}
		return schedule.get(goc.startTime());
	}

	private static void applyIsCurrentPeriodFixed(InitialPopulation.Transition ip,
			Period.Transition previousCurrentPeriod, boolean isCurrentPeriodFixed) {
		if (previousCurrentPeriod != null && isCurrentPeriodFixed) {
			ip.modeIndexes()[0] = previousCurrentPeriod.modeIndex();
		}
	}

	protected static InitialPopulation.Transition generateAllDefault(GlobalOptimizationContext goc,
			EshWithDifferentModes<?, ?, ?> esh) {
		return new InitialPopulation.Transition(goc.periods().stream() //
				.mapToInt(p -> esh.getDefaultModeIndex()) //
				.toArray()); //
	}

	protected static InitialPopulation.Transition generateFromPreviousSchedule(GlobalOptimizationContext goc,
			EshWithDifferentModes<?, ?, ?> esh, ImmutableSortedMap<ZonedDateTime, Period.Transition> schedule) {
		if (schedule == null) {
			return null;
		}
		return new InitialPopulation.Transition(goc.periods().stream() //
				.mapToInt(p -> schedule.entrySet().stream() //
						.filter(e -> e.getKey().isEqual(p.time())) //
						.map(e -> e.getValue().modeIndex()) //
						.findFirst() //
						.orElse(esh.getDefaultModeIndex())) //
				.toArray());
	}

	protected static ImmutableSet<Transition> generateInitialPopulationPerEsh(EshCodec codec,
			EshWithDifferentModes<?, ?, ?> esh) {
		// We use a Set to avoid duplicated Initial Populations
		final var ips = ImmutableSet.<InitialPopulation.Transition>builder();
		final var previousSchedule = getScheduleFromPreviousResult(esh, codec.previousResult);
		final var previousCurrentPeriod = getCurrentPeriodFromSchedule(codec.goc, esh, previousSchedule);
		final Consumer<InitialPopulation.Transition> addToResult = (ip) -> {
			if (ip == null) {
				return;
			}
			applyIsCurrentPeriodFixed(ip, previousCurrentPeriod, codec.isFirstPeriodFixed);
			ips.add(ip);
		};

		// All Default
		addToResult.accept(generateAllDefault(codec.goc, esh));
		// From Previous Schedule
		addToResult.accept(generateFromPreviousSchedule(codec.goc, esh, previousSchedule));
		// Initial Population provided by EnergyScheduleHandler
		esh.getInitialPopulation(codec.goc) //
				.forEach(ip -> addToResult.accept(ip));

		return ips.build();
	}
}
