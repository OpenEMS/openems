package io.openems.edge.evcs.keba.udp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import io.openems.edge.evcs.api.Status;
import io.openems.edge.evse.chargepoint.keba.common.enums.CableState;
import io.openems.edge.evse.chargepoint.keba.common.enums.ChargingState;

public class ReadHandlerTest {

	@Test
	public void testEnergySessionHandler() {
		var sut = new ReadHandler.EnergySessionHandler();
		sut.updateCableState(CableState.UNDEFINED);
		assertNull(sut.updateFromReport3(null));

		assertEquals(990, sut.updateFromReport3(990).intValue());

		sut.updateCableState(CableState.PLUGGED_EV_NOT_LOCKED);
		assertEquals(1000, sut.updateFromReport3(1000).intValue());

		sut.updateCableState(CableState.UNPLUGGED);
		assertNull(sut.updateFromReport3(1010));

		sut.updateCableState(CableState.PLUGGED_EV_NOT_LOCKED);
		assertNull(sut.updateFromReport3(1020));

		sut.updateCableState(CableState.PLUGGED_AND_LOCKED);
		assertEquals(500, sut.updateFromReport3(500).intValue());

		sut.updateCableState(CableState.UNPLUGGED);
		assertNull(sut.updateFromReport3(510));

		sut.updateCableState(CableState.PLUGGED_AND_LOCKED);
		assertNull(sut.updateFromReport3(510));

		sut.updateCableState(CableState.PLUGGED_AND_LOCKED);
		assertEquals(20, sut.updateFromReport3(20).intValue());
	}

	@Test
	public void testGetStatus() {
		assertEquals(Status.READY_FOR_CHARGING, //
				ReadHandler.getStatus(ChargingState.READY_FOR_CHARGING, 0, 0));
	}
}
