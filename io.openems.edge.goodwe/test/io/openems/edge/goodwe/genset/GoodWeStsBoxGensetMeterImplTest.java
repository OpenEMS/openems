package io.openems.edge.goodwe.genset;

import static io.openems.edge.goodwe.genset.GoodWeStsBoxGensetMeterImpl.calculatePower;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.edge.common.sum.DummySum;

public class GoodWeStsBoxGensetMeterImplTest {

	@Test
	public void testCalculatePower() {
		assertEquals(0, calculatePower(//
				new DummySum(), //
				null));

		assertEquals(100, calculatePower(//
				new DummySum(), //
				100L));

		assertEquals(300, calculatePower(//
				new DummySum() //
						.withEssDischargePower(-200) //
						.withProductionAcActivePower(400), //
				100L));
	}

}
