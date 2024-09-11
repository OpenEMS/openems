package io.openems.edge.energy.optimizer;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.stream.IntStream;

import org.junit.Test;

import io.jenetics.Genotype;
import io.jenetics.IntegerChromosome;
import io.jenetics.IntegerGene;

public class SimulationResultTest {

	@Test
	public void test() {
		final var gsc = SimulatorTest.DUMMY_GSC;

		// Initialize EnergyScheduleHandlers
		for (var esh : gsc.handlers()) {
			esh.onBeforeSimulation(gsc);
		}

		var result = SimulationResult.fromQuarters(gsc, Genotype.of(//
				// ESH1 (BALANCING, DELAY_DISCHARGE, CHARGE_GRID)
				integerChromosomeOf(//
						0, 1, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, //
						0, 1, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, //
						0, 1, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, //
						0, 1, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, //
						0, 1, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, //
						0, 0, 1, 2), //
				// ESH2 (FOO, BAR)
				integerChromosomeOf(//
						0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, //
						0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, //
						0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, //
						0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, //
						0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, //
						0, 0, 1, 0)));

		assertEquals(1_126_154.844, result.cost(), 0.001);
	}

	/**
	 * Creates a {@link IntegerChromosome} from the given values.
	 * 
	 * @param values the int values
	 * @return the {@link IntegerChromosome}
	 */
	public static IntegerChromosome integerChromosomeOf(int... values) {
		if (values.length == 0) {
			return IntegerChromosome.of();
		}
		var max = IntStream.of(values).max().getAsInt();
		return IntegerChromosome.of(Arrays.stream(values) //
				.mapToObj(value -> IntegerGene.of(value, 0, max)) //
				.toList());
	}
}
