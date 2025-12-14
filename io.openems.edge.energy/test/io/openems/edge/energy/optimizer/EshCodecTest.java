package io.openems.edge.energy.optimizer;

import static io.openems.edge.energy.optimizer.SimulatorTest.DUMMY_PREVIOUS_RESULT;
import static io.openems.edge.energy.optimizer.SimulatorTest.DUMMY_SIMULATOR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class EshCodecTest {

	@Test
	public void testNulls() {
		final var simulator = DUMMY_SIMULATOR;
		final var goc = simulator.goc;
		final var mc = simulator.modeCombinations;
		final var codec = EshCodec.of(goc, mc, DUMMY_PREVIOUS_RESULT, true);
		assertNull(codec.encode(new int[0]));
		var one = codec.encode(new int[1]);
		assertEquals("[[[0]]]", one.toString());
		var gene = one.get(0).get(0);
		assertEquals(6, gene.max().intValue());
		assertEquals(0, gene.min().intValue());
		assertEquals(0, gene.allele().intValue());
	}

	@Test
	public void testPreviousResult() {
		final var simulator = DUMMY_SIMULATOR;
		final var goc = simulator.goc;
		final var mc = simulator.modeCombinations;
		final var previousResult = SimulationResult.fromQuarters(goc, new int[] { 3, 1, 1, 1 });

		final var codec = EshCodec.of(goc, mc, previousResult, true);

		var gt = codec.encoding().newInstance();
		var decoded = codec.decode(gt);
		var encoded = codec.encode(decoded);

		assertEquals(3 /* as defined in previousResult */, encoded.get(0).gene().intValue());
		assertEquals(gt.get(0).get(1).intValue(), encoded.get(0).get(1).intValue());
	}
}
