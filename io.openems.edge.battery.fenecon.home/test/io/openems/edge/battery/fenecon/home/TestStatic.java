package io.openems.edge.battery.fenecon.home;

import static org.junit.Assert.assertEquals;

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
}
