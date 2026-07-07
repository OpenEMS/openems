package io.openems.edge.battery.fenecon.home;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class BatteryFeneconHomeHardwareTypeTest {

	@Test
	void testBat64ModuleSerialNumber() {
		assertEquals("519110001918",
				BatteryFeneconHomeHardwareType.BATTERY_64.serialNrPrefixModule.apply(LocalDate.of(2025, 5, 29)));
		assertEquals("519110001918",
				BatteryFeneconHomeHardwareType.BATTERY_64.serialNrPrefixModule.apply(LocalDate.of(2025, 5, 30)));
		assertEquals("519110002567",
				BatteryFeneconHomeHardwareType.BATTERY_64.serialNrPrefixModule.apply(LocalDate.of(2025, 5, 31)));
		assertEquals("519110002567",
				BatteryFeneconHomeHardwareType.BATTERY_64.serialNrPrefixModule.apply(LocalDate.of(2025, 6, 1)));
	}

}