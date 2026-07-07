package io.openems.edge.evse.api.chargepoint.dummy;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.common.types.MeterType;

public class DummyEvseChargePointTest {

	@Test
	public void test() {
		var sut = new DummyEvseChargePoint("evseChargePoint0") //
				.withIsReadOnly(true);
		assertEquals(MeterType.CONSUMPTION_METERED, sut.getMeterType());

		sut.withIsReadOnly(false);
		assertEquals(MeterType.MANAGED_CONSUMPTION_METERED, sut.getMeterType());
	}
}
