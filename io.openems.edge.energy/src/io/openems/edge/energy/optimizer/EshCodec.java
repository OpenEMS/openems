package io.openems.edge.energy.optimizer;

import static io.jenetics.util.ISeq.toISeq;
import static java.lang.Math.min;
import static java.util.stream.IntStream.range;

import java.util.function.Function;
import java.util.stream.IntStream;

import io.jenetics.Genotype;
import io.jenetics.IntegerChromosome;
import io.jenetics.IntegerGene;
import io.jenetics.engine.InvertibleCodec;
import io.jenetics.util.Factory;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext;
import io.openems.edge.energy.optimizer.ModeCombinations.ModeCombination;

/**
 * {@link InvertibleCodec} implementation to convert between {@link Genotype}
 * and int-array representation of a schedule.
 *
 * <p>
 * Genotype:
 * 
 * <ul>
 * <li>Separate Chromosome per EnergyScheduleHandler WithDifferentModes
 * <li>Chromosome length = number of periods
 * <li>Integer-Genes represent the Mode
 * </ul>
 * 
 * <p>
 * int-array schedule: index is the period; value is the index of the
 * {@link ModeCombination}.
 */
public class EshCodec implements InvertibleCodec<int[], IntegerGene> {

	public final GlobalOptimizationContext goc;
	public final SimulationResult previousResult;
	public final boolean isFirstPeriodFixed;
	public final ModeCombinations modeCombinations;

	private final Genotype<IntegerGene> gtf;
	private final Function<Genotype<IntegerGene>, int[]> decoder;
	private final Function<int[], Genotype<IntegerGene>> encoder;

	/**
	 * Creates an {@link EshCodec}.
	 * 
	 * @param goc                the {@link GlobalOptimizationContext}
	 * @param modeCombinations   the possible {@link ModeCombinations}
	 * @param previousResult     the previous {@link SimulationResult}
	 * @param isFirstPeriodFixed is the first period fixed to the value of the
	 *                           previous result?
	 * @return new {@link EshCodec} or null
	 */
	public static EshCodec of(GlobalOptimizationContext goc, ModeCombinations modeCombinations,
			SimulationResult previousResult, boolean isFirstPeriodFixed) {
		final var numberOfModeCombinations = modeCombinations.size();
		final var numberOfPeriods = goc.periods().size();
		final var genotype = Genotype.of(//
				IntegerChromosome.of(0, numberOfModeCombinations, numberOfPeriods));
		if (genotype.isEmpty()) {
			return null;
		}

		return new EshCodec(//
				// Genotype Factory
				genotype, //
				// Mode Combinations
				modeCombinations, //

				// Decoder
				gt -> {
					return IntStream.range(0, numberOfPeriods) //
							.map(periodIndex -> {
								if (isFirstPeriodFixed && periodIndex == 0) {
									// Find ModeCombination that matches with previous result
									var time = goc.periods().get(periodIndex).time();
									var period = previousResult.periods().get(time);
									if (period != null) {
										var previousMode = previousResult.periods().get(time).modeCombination();
										for (var modeIndex = 0; modeIndex < modeCombinations.size(); modeIndex++) {
											var thisMode = modeCombinations.get(modeIndex);
											if (previousMode.modes().stream() //
													.allMatch(prev -> thisMode.modes().stream() //
															.anyMatch(m -> m.name().equals(prev.name())))) {
												// Found matching ModeCombination -> return
												return modeIndex;
											}
										}
									}
								}
								// Map ModeCombination Index of given period
								return gt.get(0).get(periodIndex).intValue();
							}) //
							.toArray();
				},

				// Encoder
				schedule -> {
					if (schedule.length == 0 || modeCombinations.isEmpty()) {
						return null;
					}
					return Genotype.of(//
							IntegerChromosome.of(//
									range(0, min(schedule.length, numberOfPeriods)) //
											.mapToObj(periodIndex -> IntegerGene.of(//
													min(schedule[periodIndex], numberOfModeCombinations - 1), //
													0, numberOfModeCombinations)) //
											.collect(toISeq())));
				}, //
				goc, previousResult, isFirstPeriodFixed);
	}

	private EshCodec(//
			Genotype<IntegerGene> gtf, //
			ModeCombinations modeCombinations, //
			Function<Genotype<IntegerGene>, int[]> decoder, //
			Function<int[], Genotype<IntegerGene>> encoder, //
			GlobalOptimizationContext goc, //
			SimulationResult previousResult, //
			boolean isFirstPeriodFixed) {
		this.gtf = gtf;
		this.modeCombinations = modeCombinations;
		this.decoder = decoder;
		this.encoder = encoder;
		this.goc = goc;
		this.previousResult = previousResult;
		this.isFirstPeriodFixed = isFirstPeriodFixed;
	}

	@Override
	public Factory<Genotype<IntegerGene>> encoding() {
		return this.gtf;
	}

	@Override
	public Function<Genotype<IntegerGene>, int[]> decoder() {
		return this.decoder;
	}

	@Override
	public Function<int[], Genotype<IntegerGene>> encoder() {
		return this.encoder;
	}
}