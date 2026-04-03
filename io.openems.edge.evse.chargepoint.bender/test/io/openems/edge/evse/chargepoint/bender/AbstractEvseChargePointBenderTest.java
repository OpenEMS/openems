package io.openems.edge.evse.chargepoint.bender;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.Test;

public class AbstractEvseChargePointBenderTest {

	@Test
	public void shouldReturnTrueForValidStates() {
		assertTrue(VehicleState.STATE_C.isReadyForCharging());
		assertTrue(VehicleState.STATE_D.isReadyForCharging());
	}

	@Test
	public void shouldReturnFalseForInvalidStates() {
		assertFalse(VehicleState.STATE_B.isReadyForCharging());
		assertFalse(VehicleState.STATE_A.isReadyForCharging());
		assertFalse(VehicleState.STATE_E.isReadyForCharging());
		assertFalse(VehicleState.UNDEFINED.isReadyForCharging());
	}

}
