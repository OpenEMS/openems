package io.openems.edge.controller.ess.timeofusetariff.optimizer;

import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.interpolateArray;
import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

public class UtilsTest {

	@Test
	public void testinterpolateArrayFloat() {
		assertArrayEquals(new float[] { 123F, 123F, 234F, 234F, 345F, 345F }, //
				interpolateArray(new Float[] { null, 123F, 234F, null, 345F, null }), //
				0.0001F);
	}

	@Test
	public void testinterpolateArrayInteger() {
		assertArrayEquals(new int[] { 123, 123, 234, 234, 345, 345 }, //
				interpolateArray(new Integer[] { null, 123, 234, null, 345, null }));
	}

}
