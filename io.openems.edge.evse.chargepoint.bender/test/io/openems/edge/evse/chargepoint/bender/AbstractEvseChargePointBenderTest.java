package io.openems.edge.evse.chargepoint.bender;

import static io.openems.edge.evse.chargepoint.bender.AbstractEvseChargePointBender.interpreteState;
import static io.openems.edge.evse.chargepoint.bender.AbstractEvseChargePointBender.parseFirmwareVersion;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import io.openems.common.types.SemanticVersion;

public class AbstractEvseChargePointBenderTest {

	@Test
	public void shouldReturnTrueForValidStates() {
		assertTrue(interpreteState(VehicleState.STATE_B));
		assertTrue(interpreteState(VehicleState.STATE_C));
		assertTrue(interpreteState(VehicleState.STATE_D));
	}

	@Test
	public void shouldReturnFalseForInvalidStates() {
		assertFalse(interpreteState(VehicleState.STATE_A));
		assertFalse(interpreteState(VehicleState.STATE_E));
		assertFalse(interpreteState(VehicleState.UNDEFINED));
	}

	@Test
	public void parseSoftwareVersionTest() {
		// raw values from real test
		var registerValues = List.of(1, 5, 22);
		var firmwareVersion = parseFirmwareVersion(registerValues);
		assertEquals("1.5.22", firmwareVersion.toString());
		registerValues = new ArrayList<Integer>();
		// Three null values
		registerValues.add(null);
		registerValues.add(null);
		registerValues.add(null);
		firmwareVersion = parseFirmwareVersion(registerValues);
		assertEquals(SemanticVersion.ZERO, firmwareVersion);

	}
}
