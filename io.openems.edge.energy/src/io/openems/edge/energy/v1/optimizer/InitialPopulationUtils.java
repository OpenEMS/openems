package io.openems.edge.energy.v1.optimizer;

import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.BALANCING;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.CHARGE_GRID;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.DELAY_DISCHARGE;
import static io.openems.edge.energy.api.EnergyUtils.findFirstPeakIndex;
import static io.openems.edge.energy.api.EnergyUtils.findFirstValleyIndex;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;

import com.google.common.collect.ImmutableList;
import com.google.common.math.Quantiles;

import io.jenetics.Genotype;
import io.jenetics.IntegerChromosome;
import io.jenetics.IntegerGene;
import io.openems.edge.controller.ess.timeofusetariff.StateMachine;
import io.openems.edge.energy.v1.optimizer.Params.OptimizePeriod;

public class InitialPopulationUtils {

	private InitialPopulationUtils() {
	}

	/**
	 * Builds an initial population:
	 * 
	 * <ol>
	 * <li>Schedule with all periods BALANCING
	 * <li>Schedule from currently existing Schedule, i.e. the bestGenotype of last
	 * optimization run
	 * </ol>
	 * 
	 * <p>
	 * NOTE: providing an "all periods BALANCING" Schedule as first Genotype makes
	 * sure, that this one wins in case there are other results with same cost, e.g.
	 * when battery never gets empty anyway.
	 * 
	 * @param p the {@link Params}
	 * @return the {@link Genotype}
	 */
	public static ImmutableList<Genotype<IntegerGene>> buildInitialPopulation(Params p) {
		var states = List.of(p.states());
		if (!states.contains(BALANCING)) {
			throw new IllegalArgumentException("State option BALANCING is always required!");
		}

		var b = ImmutableList.<Genotype<IntegerGene>>builder(); //

		// All BALANCING
		b.add(Genotype.of(//
				IntStream.range(0, p.optimizePeriods().size()) //
						.map(i -> states.indexOf(BALANCING)) //
						.mapToObj(state -> IntegerChromosome.of(IntegerGene.of(state, 0, p.states().length))) //
						.toList()));

		if (p.existingSchedule().values().stream() //
				.anyMatch(s -> s != BALANCING)) {
			// Existing Schedule if available
			b.add(Genotype.of(//
					p.optimizePeriods().stream() //
							.map(op -> Optional.ofNullable(p.existingSchedule().get(op.time())).orElse(BALANCING))
							.map(state -> IntegerChromosome.of(IntegerGene.of(//
									toIndex(states, state), 0, p.states().length))) //
							.toList()));
		}

		// Suggest different combinations of CHARGE_GRID and DELAY_CHARGE
		{
			var prices = p.optimizePeriods().stream() //
					.mapToDouble(OptimizePeriod::price) //
					.toArray();
			var peakIndex = findFirstPeakIndex(findFirstValleyIndex(0, prices), prices);
			var firstPrices = Arrays.stream(prices).limit(peakIndex).toArray();
			final BiConsumer<Integer, Integer> addInitialGenotype = (chargeGridPercentile,
					delayDischargePercentile) -> b.add(generateInitialGenotype(p.optimizePeriods().size(), firstPrices,
							states, chargeGridPercentile, delayDischargePercentile));
			if (firstPrices.length > 0 && states.contains(CHARGE_GRID) && states.contains(DELAY_DISCHARGE)) {
				addInitialGenotype.accept(5, 50);
				addInitialGenotype.accept(5, 75);
				addInitialGenotype.accept(10, 50);
				addInitialGenotype.accept(10, 75);
			}
		}

		return b.build();
	}

	private static int toIndex(List<StateMachine> states, StateMachine state) {
		var result = states.indexOf(state);
		if (result != -1) {
			return result;
		}
		return states.indexOf(BALANCING);
	}

	private static Genotype<IntegerGene> generateInitialGenotype(int numberOfPeriods, double[] prices,
			List<StateMachine> states, int chargeGridPercentile, int delayDischargePercentile) {
		var percentiles = Quantiles.percentiles().indexes(chargeGridPercentile, delayDischargePercentile)
				.compute(prices);
		return Genotype.of(//
				IntStream.range(0, numberOfPeriods) //
						.mapToObj(i -> {
							if (i >= prices.length) {
								return BALANCING;
							}
							var price = prices[i];
							return price <= percentiles.get(chargeGridPercentile) //
									? CHARGE_GRID //
									: price <= percentiles.get(delayDischargePercentile) //
											? DELAY_DISCHARGE //
											: BALANCING;
						}) //
						.map(state -> IntegerChromosome.of(IntegerGene.of(toIndex(states, state), 0, states.size()))) //
						.toList());
	}

}
