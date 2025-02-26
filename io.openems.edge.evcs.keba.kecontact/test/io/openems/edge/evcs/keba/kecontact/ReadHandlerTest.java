package io.openems.edge.evcs.keba.kecontact;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import io.openems.edge.evcs.api.Status;

public class ReadHandlerTest {

	@Test
	public void testEnergySessionHandler() {
		var sut = new ReadHandler.EnergySessionHandler();
		sut.updatePlug(R2Plug.UNDEFINED);
		assertNull(sut.updateFromReport3(null));

		assertEquals(990, sut.updateFromReport3(990).intValue());

		sut.updatePlug(R2Plug.PLUGGED_ON_EVCS_AND_ON_EV);
		assertEquals(1000, sut.updateFromReport3(1000).intValue());

		sut.updatePlug(R2Plug.UNPLUGGED);
		assertNull(sut.updateFromReport3(1010));

		sut.updatePlug(R2Plug.PLUGGED_ON_EVCS_AND_ON_EV);
		assertNull(sut.updateFromReport3(1020));

		sut.updatePlug(R2Plug.PLUGGED_ON_EVCS_AND_ON_EV_AND_LOCKED);
		assertEquals(500, sut.updateFromReport3(500).intValue());

		sut.updatePlug(R2Plug.UNPLUGGED);
		assertNull(sut.updateFromReport3(510));

		sut.updatePlug(R2Plug.PLUGGED_ON_EVCS_AND_ON_EV_AND_LOCKED);
		assertNull(sut.updateFromReport3(510));

		sut.updatePlug(R2Plug.PLUGGED_ON_EVCS_AND_ON_EV_AND_LOCKED);
		assertEquals(20, sut.updateFromReport3(20).intValue());
	}

	@Test
	public void testGetStatus() {
		assertEquals(Status.READY_FOR_CHARGING, //
				ReadHandler.getStatus(R2State.READY, 0, 0));
	}
}
