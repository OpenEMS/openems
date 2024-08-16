package io.openems.edge.battery.fenecon.home;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import io.openems.edge.battery.fenecon.home.BatteryFeneconHomeImpl.MinVoltageSubState;

public class TestStatic {

	@Test
	public void testGetMinVoltageSubState() {

		assertEquals(MinVoltageSubState.BELOW_LIMIT_CHARGING,
				BatteryFeneconHomeImpl.getMinVoltageSubState(2800, 2700, -2000));

		assertEquals(MinVoltageSubState.BELOW_LIMIT, BatteryFeneconHomeImpl.getMinVoltageSubState(2800, 2600, 0));
		assertEquals(MinVoltageSubState.BELOW_LIMIT, BatteryFeneconHomeImpl.getMinVoltageSubState(2800, 2600, 2000));

		assertEquals(MinVoltageSubState.ABOVE_LIMIT,
				BatteryFeneconHomeImpl.getMinVoltageSubState(2800, Integer.MAX_VALUE, 0));
		assertEquals(MinVoltageSubState.ABOVE_LIMIT, BatteryFeneconHomeImpl.getMinVoltageSubState(2800, 2900, 1000));
	}

	@Test
	public void testCalculateTowerNumberFromSoftwareVersion() {

		List<Integer> nullList = Arrays.asList(1, null, null, null, null);

		assertNull(BatteryFeneconHomeImpl //
				.calculateTowerNumberFromSoftwareVersion(nullList));

		assertNull(BatteryFeneconHomeImpl //
				.calculateTowerNumberFromSoftwareVersion(Arrays.asList(1, null, null, null, null)));

		assertNull(BatteryFeneconHomeImpl //
				.calculateTowerNumberFromSoftwareVersion(Arrays.asList(null, null, null, null, 1)));

		assertNull(BatteryFeneconHomeImpl //
				.calculateTowerNumberFromSoftwareVersion(Arrays.asList(1, null, 0, null, null)));

		assertNull(BatteryFeneconHomeImpl //
				.calculateTowerNumberFromSoftwareVersion(Arrays.asList(null, 1, 0, null, null)));

		// End-Condition met
		assertEquals(1, (int) BatteryFeneconHomeImpl //
				.calculateTowerNumberFromSoftwareVersion(Arrays.asList(1, 0, null, null, null)));

		assertEquals(1, (int) BatteryFeneconHomeImpl //
				.calculateTowerNumberFromSoftwareVersion(Arrays.asList(1, 256, null, null, null)));

		assertEquals(1, (int) BatteryFeneconHomeImpl //
				.calculateTowerNumberFromSoftwareVersion(Arrays.asList(1, 256, null, null, null)));

		assertEquals(2, (int) BatteryFeneconHomeImpl //
				.calculateTowerNumberFromSoftwareVersion(Arrays.asList(1, 2, 0, 0, 1)));

		assertEquals(3, (int) BatteryFeneconHomeImpl //
				.calculateTowerNumberFromSoftwareVersion(Arrays.asList(1, 2, 3, 0, 0)));

		assertEquals(4, (int) BatteryFeneconHomeImpl //
				.calculateTowerNumberFromSoftwareVersion(Arrays.asList(4, 4, 4, 4, 0)));

		// Exceptionally not null
		assertEquals(1, (int) BatteryFeneconHomeImpl //
				.calculateTowerNumberFromSoftwareVersion(Arrays.asList(256, 0, 0, 0, 0)));
	}
}
