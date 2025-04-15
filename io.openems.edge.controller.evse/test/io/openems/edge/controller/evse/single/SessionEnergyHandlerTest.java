package io.openems.edge.controller.evse.single;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.edge.evse.api.chargepoint.dummy.DummyEvseChargePoint;

public class SessionEnergyHandlerTest {

	@Test
	public void test() {
		var sut = new SessionEnergyHandler();
		var cp = new DummyEvseChargePoint("evseChargePoint0");

		assertEquals(0, sut.onBeforeProcessImage(cp));

		cp //
				.withIsReadyForCharging(false) //
				.withActiveProductionEnergy(1000);
		sut.onAfterProcessImage(cp);
		assertEquals(0, sut.onBeforeProcessImage(cp));

		cp //
				.withIsReadyForCharging(false) //
				.withActiveProductionEnergy(2000);
		sut.onAfterProcessImage(cp);
		assertEquals(0, sut.onBeforeProcessImage(cp));

		cp //
				.withIsReadyForCharging(true) //
				.withActiveProductionEnergy(2000);
		sut.onAfterProcessImage(cp);
		assertEquals(0, sut.onBeforeProcessImage(cp));

		cp //
				.withIsReadyForCharging(true) //
				.withActiveProductionEnergy(3000);
		sut.onAfterProcessImage(cp);
		assertEquals(1000, sut.onBeforeProcessImage(cp));

		cp //
				.withIsReadyForCharging(false) //
				.withActiveProductionEnergy(4000);
		sut.onAfterProcessImage(cp);
		assertEquals(0, sut.onBeforeProcessImage(cp));
	}
}
