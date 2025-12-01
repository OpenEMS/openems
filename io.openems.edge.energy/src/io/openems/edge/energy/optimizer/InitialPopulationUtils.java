package io.openems.edge.energy.optimizer;

import static io.jenetics.util.ISeq.toISeq;

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import com.google.common.collect.Lists;

import io.jenetics.IntegerGene;
import io.jenetics.engine.EvolutionInit;
import io.openems.edge.energy.api.handler.DifferentModes.InitialPopulation;
import io.openems.edge.energy.api.handler.DifferentModes.InitialPopulation.Transition;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext;
import io.openems.edge.energy.optimizer.ModeCombinations.ModeCombination;

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
		final var result = new ArrayList<InitialPopulation.Transition>();
		final var previousSchedule = getScheduleFromPreviousResult(codec);
		final Consumer<InitialPopulation.Transition> addToResult = (ip) -> {
			if (ip == null) {
				return;
			}
			applyIsCurrentPeriodFixed(ip, previousSchedule.modeIndexes()[0], codec.isFirstPeriodFixed);
			result.add(ip);
		};

		// All Default
		addToResult.accept(generateAllDefault(codec.goc));
		// From Previous Schedule
		addToResult.accept(previousSchedule);

		// Cartesian Product of initial Populations provided by EnergyScheduleHandlers
		Lists.cartesianProduct(codec.goc.eshsWithDifferentModes().stream() //
				.map(esh -> esh.getInitialPopulation(codec.goc)) //
				.toList()).stream() //
				.forEach(ips -> {
					final var length = ips.stream() //
							.mapToInt(ip -> ip.modeIndexes().length).min();
					if (length.isEmpty()) {
						return;
					}
					addToResult.accept(new Transition(IntStream.range(0, length.getAsInt()) //
							.mapToObj(i -> IntStream.range(0, ips.size()) //
									.map(j -> ips.get(j).modeIndexes()[i]) //
									.toArray()) //
							.map(arr -> codec.modeCombinations.getFromModeIndexesOrDefault(arr)) //
							.mapToInt(ModeCombination::index) //
							.toArray()));
				});

		return EvolutionInit.of(result.stream() //
				.map(InitialPopulation.Transition::modeIndexes) //
				.map(codec::encode) //
				.distinct() //
				.collect(toISeq()), 1 /* first generation */);
	}

	protected static InitialPopulation.Transition getScheduleFromPreviousResult(EshCodec codec) {
		return new InitialPopulation.Transition(codec.goc.periods().stream() //
				.map(p -> codec.previousResult.periods().entrySet().stream() //
						.filter(e -> e.getKey().isEqual(p.time())) //
						.map(e -> e.getValue().modeCombination()) //
						.map(codec.modeCombinations::getMatchingOrDefault) //
						.findFirst() //
						.orElse(codec.modeCombinations.getDefault())) //
				.mapToInt(ModeCombination::index) //
				.toArray());
	}

	private static void applyIsCurrentPeriodFixed(InitialPopulation.Transition ip, int previousCurrentPeriod,
			boolean isCurrentPeriodFixed) {
		if (isCurrentPeriodFixed) {
			ip.modeIndexes()[0] = previousCurrentPeriod;
		}
	}

	protected static InitialPopulation.Transition generateAllDefault(GlobalOptimizationContext goc) {
		return new InitialPopulation.Transition(goc.periods().stream() //
				.mapToInt(p -> 0) // Index "0" is default Mode for all ESHs
				.toArray()); //
	}
}
