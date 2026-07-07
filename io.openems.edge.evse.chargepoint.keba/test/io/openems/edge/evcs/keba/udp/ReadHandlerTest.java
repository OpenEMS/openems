package io.openems.edge.evcs.keba.udp;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.edge.evcs.api.Status;
import io.openems.edge.evse.chargepoint.keba.common.enums.ChargingState;

public class ReadHandlerTest {

	@Test
	public void testGetStatus() {
		assertEquals(Status.READY_FOR_CHARGING, //
				ReadHandler.getStatus(ChargingState.READY_FOR_CHARGING, 0, 0));
	}
}
