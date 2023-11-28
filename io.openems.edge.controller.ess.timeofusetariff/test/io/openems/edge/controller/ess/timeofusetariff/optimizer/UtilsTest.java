package io.openems.edge.controller.ess.timeofusetariff.optimizer;

import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.calculateCharge100;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.interpolateArray;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.joinConsumptionPredictions;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.edge.common.sum.DummySum;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;

public class UtilsTest {

	@Test
	public void testInterpolateArrayFloat() {
		assertArrayEquals(new float[] { 123F, 123F, 234F, 234F, 345F }, //
				interpolateArray(new Float[] { null, 123F, 234F, null, 345F, null }), //
				0.0001F);

		assertArrayEquals(new float[] {}, //
				interpolateArray(new Float[] { null }), //
				0.0001F);
	}

	@Test
	public void testInterpolateArrayInteger() {
		assertArrayEquals(new int[] { 123, 123, 234, 234, 345 }, //
				interpolateArray(new Integer[] { null, 123, 234, null, 345, null }));

		assertArrayEquals(new int[] {}, //
				interpolateArray(new Integer[] { null }));

		assertArrayEquals(new int[] { 123, 123 }, //
				interpolateArray(new Integer[] { null, 123 }));

		assertArrayEquals(new int[] { 123 }, //
				interpolateArray(new Integer[] { 123, null }));
	}

	@Test
	public void testJoinConsumptionPredictions() {
		assertArrayEquals(//
				new Integer[] { 1, 2, 3, 4, 55, 66, 77, 88, 99 }, //
				joinConsumptionPredictions(4, //
						new Integer[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 }, //
						new Integer[] { 11, 22, 33, 44, 55, 66, 77, 88, 99 }));
	}

	@Test
	public void testCalculateCharge100() {
		assertEquals(-2500, calculateCharge100(//
				new DummyManagedSymmetricEss("ess0").withActivePower(-1000), //
				new DummySum().withGridActivePower(500), //
				/* maxChargePowerFromGrid */ 2000).intValue());

		// Would be 5000, but can never be positive
		assertEquals(0, calculateCharge100(//
				new DummyManagedSymmetricEss("ess0").withActivePower(1000), //
				new DummySum().withGridActivePower(9000), //
				/* maxChargePowerFromGrid */ 5000).intValue());
	}

}
