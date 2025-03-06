package io.openems.edge.energy.optimizer;

import static io.jenetics.util.ISeq.toISeq;
import static java.lang.Math.min;
import static java.util.stream.Collectors.joining;
import static java.util.stream.IntStream.range;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

import io.jenetics.Genotype;
import io.jenetics.IntegerChromosome;
import io.jenetics.IntegerGene;
import io.jenetics.engine.InvertibleCodec;
import io.jenetics.util.Factory;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext;

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
 * int-array schedule:
 * 
 * <ul>
 * <li>First dimension: period, i.e. [0] is first period
 * <li>Second dimension: Mode index of
 * {@link EnergyScheduleHandler.WithDifferentModes}
 * </ul>
 */
public class EshCodec implements InvertibleCodec<int[][], IntegerGene> {

	private final Genotype<IntegerGene> gtf;
	private final Function<Genotype<IntegerGene>, int[][]> decoder;
	private final Function<int[][], Genotype<IntegerGene>> encoder;

	/**
	 * Creates an {@link EshCodec}.
	 * 
	 * @param goc                the {@link GlobalOptimizationContext}
	 * @param previousResult     the previous {@link SimulationResult}
	 * @param isFirstPeriodFixed is the first period fixed to the value of the
	 *                           previous result?
	 * @return new {@link EshCodec} or null
	 */
	public static EshCodec of(GlobalOptimizationContext goc, SimulationResult previousResult,
			boolean isFirstPeriodFixed) {
		final var chromosomes = goc.eshsWithDifferentModes().stream() //
				.map(esh -> IntegerChromosome.of(0, esh.getNumberOfAvailableModes(), goc.periods().size())) //
				.collect(toISeq());
		if (chromosomes.isEmpty()) {
			return null;
		}

		return new EshCodec(//
				// Genotype Factory
				Genotype.of(chromosomes), //

				// Decoder
				gt -> {
					final var numberOfPeriods = goc.periods().size();
					final var numberOfEshs = goc.eshsWithDifferentModes().size();
					final var schedule = new int[numberOfPeriods][numberOfEshs];

					for (var periodIndex = 0; periodIndex < numberOfPeriods; periodIndex++) {
						for (var eshIndex = 0; eshIndex < numberOfEshs; eshIndex++) {
							final int value = gt.get(eshIndex).get(periodIndex).intValue();
							final int mode;
							// TODO Valid Modes per Period should be read from ESH here.
							// Example: might be limited by a SmartConfig with JSCalendar payload
							if (isFirstPeriodFixed && periodIndex == 0) {
								final var time = goc.periods().get(periodIndex).time();
								final var esh = goc.eshsWithDifferentModes().get(eshIndex);
								mode = Optional.ofNullable(previousResult.schedules().get(esh))
										.flatMap(s -> Optional.ofNullable(s.get(time)).map(t -> t.modeIndex()))
										.orElse(value);
							} else {
								mode = gt.get(eshIndex).get(periodIndex).intValue();
							}
							schedule[periodIndex][eshIndex] = mode;
						}
					}
					return schedule;
				},

				// Encoder
				schedule -> {
					if (schedule.length == 0) {
						return null;
					}
					var numberOfEshs = min(schedule[0].length, goc.eshsWithDifferentModes().size());
					if (numberOfEshs == 0) {
						return null;
					}
					var numberOfPeriods = min(schedule.length, goc.periods().size());
					return Genotype.of(range(0, numberOfEshs) //
							.mapToObj(eshIndex -> {
								final var esh = goc.eshsWithDifferentModes().get(eshIndex);
								final var max = esh.getNumberOfAvailableModes();
								return IntegerChromosome.of(range(0, numberOfPeriods) //
										.mapToObj(periodIndex -> IntegerGene.of(//
												min(schedule[periodIndex][eshIndex], max - 1), 0, max)) //
										.collect(toISeq()));
							}) //
							.collect(toISeq()));
				});
	}

	private EshCodec(//
			Genotype<IntegerGene> gtf, //
			Function<Genotype<IntegerGene>, int[][]> decoder, //
			Function<int[][], Genotype<IntegerGene>> encoder) {
		this.gtf = gtf;
		this.decoder = decoder;
		this.encoder = encoder;
	}

	@Override
	public Factory<Genotype<IntegerGene>> encoding() {
		return this.gtf;
	}

	@Override
	public Function<Genotype<IntegerGene>, int[][]> decoder() {
		return this.decoder;
	}

	@Override
	public Function<int[][], Genotype<IntegerGene>> encoder() {
		return this.encoder;
	}

	/**
	 * Converts a Schedule to a human readable String.
	 * 
	 * @param schedule the Schedule
	 * @return the String
	 */
	public static String scheduleToString(int[][] schedule) {
		return "[" + Arrays.stream(schedule) //
				.map(arr -> "[" + Arrays.stream(arr).mapToObj(Integer::toString).collect(joining(",")) + "]") //
				.collect(joining(",")) + "]";
	}

	/**
	 * Converts a Collection of Schedules to a human readable String.
	 * 
	 * @param schedules Collection of Schedules
	 * @return the String
	 */
	public static String schedulesToString(Collection<int[][]> schedules) {
		return schedules.stream() //
				.map(EshCodec::scheduleToString) //
				.collect(joining("\n"));
	}

	/**
	 * Converts a Collection of Schedules to an array of human readable Strings.
	 * 
	 * @param schedules Collection of Schedules
	 * @return the String array
	 */
	public static String[] schedulesToStringArray(Collection<int[][]> schedules) {
		return schedules.stream() //
				.map(EshCodec::scheduleToString) //
				.toArray(String[]::new);
	}
}