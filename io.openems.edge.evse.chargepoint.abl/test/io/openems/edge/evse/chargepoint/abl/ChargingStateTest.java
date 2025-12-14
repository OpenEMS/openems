package io.openems.edge.evse.chargepoint.abl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import io.openems.edge.evse.chargepoint.abl.enums.ChargingState;
import io.openems.edge.evse.chargepoint.abl.enums.Status;

/**
 * Unit tests for ChargingState enum.
 */
public class ChargingStateTest {

	@Test
	public void testFromValue() {
		assertEquals(ChargingState.A1, ChargingState.fromValue(0xA1));
		assertEquals(ChargingState.B1, ChargingState.fromValue(0xB1));
		assertEquals(ChargingState.B2, ChargingState.fromValue(0xB2));
		assertEquals(ChargingState.C2, ChargingState.fromValue(0xC2));
		assertEquals(ChargingState.C3, ChargingState.fromValue(0xC3));
		assertEquals(ChargingState.C4, ChargingState.fromValue(0xC4));
	}

	@Test
	public void testFromValueError() {
		assertEquals(ChargingState.F1, ChargingState.fromValue(0xF1));
		assertEquals(ChargingState.F2, ChargingState.fromValue(0xF2));
		assertEquals(ChargingState.F9, ChargingState.fromValue(0xF9));
		assertEquals(ChargingState.F10, ChargingState.fromValue(0xFA));
		assertEquals(ChargingState.F11, ChargingState.fromValue(0xFB));
	}

	@Test
	public void testFromValueUndefined() {
		assertEquals(ChargingState.UNDEFINED, ChargingState.fromValue(0xFF));
		assertEquals(ChargingState.UNDEFINED, ChargingState.fromValue(0x00));
	}

	@Test
	public void testStatusMapping() {
		// Normal operation states
		assertEquals(Status.NOT_READY_FOR_CHARGING, ChargingState.A1.status);
		assertEquals(Status.READY_FOR_CHARGING, ChargingState.B1.status);
		assertEquals(Status.READY_FOR_CHARGING, ChargingState.B2.status);
		assertEquals(Status.CHARGING, ChargingState.C2.status);
		assertEquals(Status.CHARGING, ChargingState.C3.status);
		assertEquals(Status.CHARGING, ChargingState.C4.status);

		// Setup/disabled states
		assertEquals(Status.NOT_READY_FOR_CHARGING, ChargingState.E0.status);
		assertEquals(Status.NOT_READY_FOR_CHARGING, ChargingState.E1.status);
		assertEquals(Status.NOT_READY_FOR_CHARGING, ChargingState.E2.status);
		assertEquals(Status.NOT_READY_FOR_CHARGING, ChargingState.E3.status);

		// Error states
		assertEquals(Status.ERROR, ChargingState.F1.status);
		assertEquals(Status.ERROR, ChargingState.F2.status);
		assertEquals(Status.ERROR, ChargingState.F9.status);
	}

	@Test
	public void testGetValue() {
		assertEquals(0xA1, ChargingState.A1.getValue());
		assertEquals(0xC2, ChargingState.C2.getValue());
		assertEquals(0xF9, ChargingState.F9.getValue());
	}

	@Test
	public void testGetName() {
		assertNotNull(ChargingState.A1.getName());
		assertNotNull(ChargingState.C2.getName());
		assertNotNull(ChargingState.F9.getName());
	}

	@Test
	public void testAllStatesHaveStatus() {
		for (ChargingState state : ChargingState.values()) {
			if (state != ChargingState.UNDEFINED) {
				assertNotNull("State " + state + " should have a status", state.status);
			}
		}
	}
}
