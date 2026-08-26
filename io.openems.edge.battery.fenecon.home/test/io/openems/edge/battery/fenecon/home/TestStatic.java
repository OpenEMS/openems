package io.openems.edge.battery.fenecon.home;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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

		assertEquals(MinVoltageSubState.ABOVE_LIMIT, BatteryFeneconHomeImpl.getMinVoltageSubState(2800, null, -2000));
		assertEquals(MinVoltageSubState.ABOVE_LIMIT, BatteryFeneconHomeImpl.getMinVoltageSubState(2800, null, null));
		assertEquals(MinVoltageSubState.ABOVE_LIMIT, BatteryFeneconHomeImpl.getMinVoltageSubState(2800, null, 2000));
		assertEquals(MinVoltageSubState.BELOW_LIMIT, BatteryFeneconHomeImpl.getMinVoltageSubState(2800, 2700, null));
	}

	@Test
	public void testParseEmsPowerConsumption() {
		// 0x01F4 -> 500 mA, on-grid
		assertEquals(Integer.valueOf(500), BatteryFeneconHomeImpl.parseEmsPowerConsumption(0x01F4));

		// 0x81F4 -> 500 mA, off-grid
		assertEquals(Integer.valueOf(500), BatteryFeneconHomeImpl.parseEmsPowerConsumption(0x81F4));

		// Edge cases
		assertEquals(Integer.valueOf(0), BatteryFeneconHomeImpl.parseEmsPowerConsumption(0x0000));
		assertEquals(Integer.valueOf(0), BatteryFeneconHomeImpl.parseEmsPowerConsumption(0x8000));
		assertEquals(Integer.valueOf(0x7FFF), BatteryFeneconHomeImpl.parseEmsPowerConsumption(0x7FFF));
		assertEquals(Integer.valueOf(0x7FFF), BatteryFeneconHomeImpl.parseEmsPowerConsumption(0xFFFF));

		assertNull(BatteryFeneconHomeImpl.parseEmsPowerConsumption(null));
	}

	@Test
	public void testParseEmsOffGrid() {
		// 0x81F4 -> off-grid = true
		assertEquals(Boolean.TRUE, BatteryFeneconHomeImpl.parseEmsOffGrid(0x81F4));

		// 0x01F4 -> on-grid = false
		assertEquals(Boolean.FALSE, BatteryFeneconHomeImpl.parseEmsOffGrid(0x01F4));

		// Edge cases
		assertEquals(Boolean.FALSE, BatteryFeneconHomeImpl.parseEmsOffGrid(0x0000));
		assertEquals(Boolean.TRUE, BatteryFeneconHomeImpl.parseEmsOffGrid(0x8000));
		assertEquals(Boolean.TRUE, BatteryFeneconHomeImpl.parseEmsOffGrid(0xFFFF));

		// Null input
		assertNull(BatteryFeneconHomeImpl.parseEmsOffGrid(null));
	}
}
