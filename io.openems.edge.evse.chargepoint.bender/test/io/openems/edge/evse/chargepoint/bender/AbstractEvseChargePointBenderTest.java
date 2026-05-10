package io.openems.edge.evse.chargepoint.bender;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.Test;

public class AbstractEvseChargePointBenderTest {

	@Test
	public void shouldReturnTrueForValidVehicleStates() {
	    assertTrue(VehicleState.STATE_C.isEvConnected);
	    assertTrue(VehicleState.STATE_D.isEvConnected);
	    assertTrue(VehicleState.STATE_B.isEvConnected);
	}

	@Test
	public void shouldReturnFalseForInvalidVehicleStates() {
	    assertFalse(VehicleState.STATE_A.isEvConnected);
	    assertFalse(VehicleState.STATE_E.isEvConnected);
	    assertFalse(VehicleState.UNDEFINED.isEvConnected);
	}

	@Test
	public void shouldReturnTrueForReadyOcppStates() {
	    assertTrue(OcppState.AVAILABLE.isReadyForCharging);
	    assertTrue(OcppState.PREPARING.isReadyForCharging);
	    assertTrue(OcppState.CHARGING.isReadyForCharging);
	    assertTrue(OcppState.SUSPENDED_EVSE.isReadyForCharging);
	    assertTrue(OcppState.SUSPENDED_EV.isReadyForCharging);
	}

	@Test
	public void shouldReturnFalseForNotReadyOcppStates() {
	    assertFalse(OcppState.UNDEFINED.isReadyForCharging);
	    assertFalse(OcppState.FINISHING.isReadyForCharging);
	    assertFalse(OcppState.RESERVED.isReadyForCharging);
	    assertFalse(OcppState.UNAVAILABLE.isReadyForCharging);
	    assertFalse(OcppState.FAULTED.isReadyForCharging);
	}

}
