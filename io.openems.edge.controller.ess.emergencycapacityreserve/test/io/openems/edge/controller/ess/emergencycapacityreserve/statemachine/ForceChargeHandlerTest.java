package io.openems.edge.controller.ess.emergencycapacityreserve.statemachine;

import static io.openems.edge.controller.ess.emergencycapacityreserve.statemachine.ForceChargeHandler.getAcPvProduction;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.edge.common.sum.DummySum;

public class ForceChargeHandlerTest {

	@Test
	public void testGetAcPvProduction() {
		var sum = new DummySum();

		// Fallback to 'zero' for null
		assertEquals(0, getAcPvProduction(sum));

		// Guarantee positive values
		assertEquals(0, getAcPvProduction(sum.withProductionAcActivePower(-100)));

		// Get positive values
		assertEquals(1234, getAcPvProduction(sum.withProductionAcActivePower(1234)));
	}

}
