package io.openems.edge.controller.ess.timeofusetariff;

import static io.openems.edge.controller.ess.timeofusetariff.Utils.calculateChargeFromGridPower;
import static io.openems.edge.controller.ess.timeofusetariff.Utils.calculateDelayDischargePower;
import static io.openems.edge.controller.ess.timeofusetariff.Utils.interpolateArray;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.edge.common.sum.DummySum;
import io.openems.edge.ess.test.DummyHybridEss;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;

public class UtilsTest {

	@Test
	public void testCalculateChargeFromGridPower() {
		// ESS+Sum data not available -> fallback to 'chargeDischargeEnergy'
		assertEquals(-4000, calculateChargeFromGridPower(//
				-1000, // Charge 1 kWh/15min -> 4 kW
				new DummyManagedSymmetricEss("ess0"), //
				new DummySum(), //
				500).intValue());

		// Reduce ESS charge power if there is already buy-from-grid
		// real grid-power is 1800 (=2500-700) -> charge with 200
		assertEquals(-200, calculateChargeFromGridPower(//
				-1000, //
				new DummyManagedSymmetricEss("ess0").withActivePower(-700), //
				new DummySum().withGridActivePower(2500), //
				2000).intValue());

		// Currently feeding to grid: more charge power is possible
		// sell-to-grid is 1000 + limit 2000 -> charge with 3000
		assertEquals(-3000, calculateChargeFromGridPower(//
				-1000, //
				new DummyManagedSymmetricEss("ess0").withActivePower(0), //
				new DummySum().withGridActivePower(-1000), //
				2000).intValue());
	}

	@Test
	public void testCalculateDelayDischargePower() {
		assertEquals(4000, //
				calculateDelayDischargePower(1000, new DummyManagedSymmetricEss("ess0")).intValue());

		assertEquals(0, //
				calculateDelayDischargePower(0, new DummyManagedSymmetricEss("ess0")).intValue());

		assertEquals(-4000, //
				calculateDelayDischargePower(-1000, new DummyManagedSymmetricEss("ess0")).intValue());

		assertEquals(5000, //
				calculateDelayDischargePower(1000,
						new DummyHybridEss("ess0").withDcDischargePower(2000).withActivePower(3000)).intValue());

		assertEquals(4000, //
				calculateDelayDischargePower(1000, new DummyHybridEss("ess0")).intValue());
	}

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
