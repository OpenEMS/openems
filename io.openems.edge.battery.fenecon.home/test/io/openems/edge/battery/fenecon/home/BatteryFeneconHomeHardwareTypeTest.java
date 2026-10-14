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
				BatteryFeneconHomeHardwareType.BATTERY_64.serialNrPrefixModule.apply(LocalDate.of(2026, 6, 11)));
		assertEquals("519111002959",
				BatteryFeneconHomeHardwareType.BATTERY_64.serialNrPrefixModule.apply(LocalDate.of(2026, 6, 12)));
		assertEquals("519111002959",
				BatteryFeneconHomeHardwareType.BATTERY_64.serialNrPrefixModule.apply(LocalDate.of(2026, 10, 5)));
	}

}