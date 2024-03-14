package io.openems.edge.controller.ess.timeofusetariff.optimizer;

import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.BALANCING;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.CHARGE_GRID;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.DELAY_DISCHARGE;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.findFirstPeakIndex;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.findFirstValleyIndex;
import static java.util.Arrays.stream;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.google.common.math.Quantiles;

import io.jenetics.Genotype;
import io.jenetics.IntegerChromosome;
import io.jenetics.IntegerGene;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.controller.ess.timeofusetariff.StateMachine;

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
		var b = ImmutableList.<Genotype<IntegerGene>>builder(); //

		// All BALANCING
		b.add(Genotype.of(//
				IntStream.range(0, p.numberOfPeriods()) //
						.map(i -> states.indexOf(BALANCING)) //
						.mapToObj(state -> IntegerChromosome.of(IntegerGene.of(state, 0, p.states().length))) //
						.toList()));

		if (p.existingSchedule().length > 0 //
				&& Stream.of(p.existingSchedule()) //
						.anyMatch(s -> s != BALANCING)) {
			// Existing Schedule if available
			b.add(Genotype.of(//
					IntStream.range(0, p.numberOfPeriods()) //
							// Map to state index; not-found maps to '-1', corrected to '0'
							.map(i -> TypeUtils.fitWithin(0, p.states().length, states.indexOf(//
									p.existingSchedule().length > i //
											? p.existingSchedule()[i] //
											: BALANCING))) //
							.mapToObj(state -> IntegerChromosome.of(IntegerGene.of(state, 0, p.states().length))) //
							.toList()));
		}

		// Suggest different combinations of CHARGE_GRID and DELAY_CHARGE
		{
			var peakIndex = findFirstPeakIndex(findFirstValleyIndex(0, p.prices()), p.prices());
			var firstPrices = stream(p.prices()).limit(peakIndex).toArray();
			if (firstPrices.length > 0 && states.contains(CHARGE_GRID) && states.contains(DELAY_DISCHARGE)) {
				b.add(generateInitialGenotype(p.numberOfPeriods(), firstPrices, states, 5, 50));
				b.add(generateInitialGenotype(p.numberOfPeriods(), firstPrices, states, 5, 75));
				b.add(generateInitialGenotype(p.numberOfPeriods(), firstPrices, states, 10, 50));
				b.add(generateInitialGenotype(p.numberOfPeriods(), firstPrices, states, 10, 75));
			}
		}

		return b.build();
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
						.map(state -> IntegerChromosome.of(IntegerGene.of(states.indexOf(state), 0, states.size()))) //
						.toList());
	}

}
