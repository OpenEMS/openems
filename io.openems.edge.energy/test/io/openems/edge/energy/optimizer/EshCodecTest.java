package io.openems.edge.energy.optimizer;

import static java.util.stream.Collectors.joining;
import static java.util.stream.IntStream.range;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class EshCodecTest {

	@Test
	public void test() {
		final var simulator = SimulatorTest.DUMMY_SIMULATOR;
		final var gsc = simulator.gsc;
		final var codec = EshCodec.of(gsc, SimulationResult.EMPTY, false);

		var gt = codec.encoding().newInstance();

		var decoded = codec.decode(gt);
		var decodedString = "" //
				+ "[" //
				+ range(0, decoded[0].length) //
						.mapToObj(eshIndex -> "" //
								+ "[" //
								+ range(0, decoded.length) //
										.mapToObj(periodIndex -> "" //
												+ "[" //
												+ decoded[periodIndex][eshIndex] //
												+ "]") //
										.collect(joining(","))
								+ "]") //
						.collect(joining(",")) //
				+ "]";
		assertEquals(gt.toString(), decodedString);

		var encoded = codec.encode(decoded);
		System.out.println(encoded);
		assertEquals(gt, encoded);
	}

	@Test
	public void testNulls() {
		final var simulator = SimulatorTest.DUMMY_SIMULATOR;
		final var gsc = simulator.gsc;
		final var codec = EshCodec.of(gsc, InitialPopulationTest.PREVIOUS_RESULT, true);
		assertNull(codec.encode(new int[0][0]));
		assertNull(codec.encode(new int[1][0]));
		var oneone = codec.encode(new int[1][1]);
		assertEquals("[[[0]]]", oneone.toString());
		var gene = oneone.get(0).get(0);
		assertEquals(3, gene.max().intValue());
		assertEquals(0, gene.min().intValue());
		assertEquals(0, gene.allele().intValue());
	}

	@Test
	public void testPreviousResult() {
		final var simulator = SimulatorTest.DUMMY_SIMULATOR;
		final var gsc = simulator.gsc;
		final var codec = EshCodec.of(gsc, InitialPopulationTest.PREVIOUS_RESULT, true);

		var gt = codec.encoding().newInstance();
		var decoded = codec.decode(gt);
		var encoded = codec.encode(decoded);
		System.out.println(gt);
		System.out.println(encoded);
		System.out.println(encoded.get(0).get(1));
		assertEquals(gt.get(0).get(1).intValue(), encoded.get(0).get(1).intValue());
		assertEquals(2, encoded.get(0).get(0).intValue());
	}
}
