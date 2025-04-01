package io.openems.edge.controller.evse.single;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.edge.evse.api.chargepoint.Status;
import io.openems.edge.evse.api.chargepoint.dummy.DummyEvseChargePoint;

public class SessionEnergyHandlerTest {

	@Test
	public void test() {
		var sut = new SessionEnergyHandler();
		var cp = new DummyEvseChargePoint("evseChargePoint0");

		assertEquals(0, sut.onBeforeProcessImage(cp));

		cp //
				.withStatus(Status.NOT_READY_FOR_CHARGING) //
				.withActiveProductionEnergy(1000);
		sut.onAfterProcessImage(cp);
		assertEquals(0, sut.onBeforeProcessImage(cp));

		cp //
				.withStatus(Status.NOT_READY_FOR_CHARGING) //
				.withActiveProductionEnergy(2000);
		sut.onAfterProcessImage(cp);
		assertEquals(0, sut.onBeforeProcessImage(cp));

		cp //
				.withStatus(Status.CHARGING) //
				.withActiveProductionEnergy(2000);
		sut.onAfterProcessImage(cp);
		assertEquals(0, sut.onBeforeProcessImage(cp));

		cp //
				.withStatus(Status.CHARGING) //
				.withActiveProductionEnergy(3000);
		sut.onAfterProcessImage(cp);
		assertEquals(1000, sut.onBeforeProcessImage(cp));

		cp //
				.withStatus(Status.STARTING) //
				.withActiveProductionEnergy(4000);
		sut.onAfterProcessImage(cp);
		assertEquals(0, sut.onBeforeProcessImage(cp));
	}
}
