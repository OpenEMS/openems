package io.openems.edge.controller.io.analog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class TestStatic {

	@Test
	public void calculateUsedPower() {

		assertEquals(6_000, (int) ControllerIoAnalogImpl.calculateUsedPower(12_000, 50f, PowerBehavior.LINEAR));

		assertEquals(2_400, (int) ControllerIoAnalogImpl.calculateUsedPower(12_000, 20f, PowerBehavior.LINEAR));

		assertEquals(9_000, (int) ControllerIoAnalogImpl.calculateUsedPower(12_000, 75f, PowerBehavior.LINEAR));

		assertEquals(6_000, (int) ControllerIoAnalogImpl.calculateUsedPower(12_000, 50f, PowerBehavior.NON_LINEAR));

		assertEquals(1_146, (int) ControllerIoAnalogImpl.calculateUsedPower(12_000, 20f, PowerBehavior.NON_LINEAR));

		assertEquals(10_243, (int) ControllerIoAnalogImpl.calculateUsedPower(12_000, 75f, PowerBehavior.NON_LINEAR));

	}

	@Test
	public void powerBehaviour() {

		// Linear Factor from Power
		assertEquals((Float) 0.2f, PowerBehavior.LINEAR.calculateFactorFromPower.apply(10000, 2000));

		// Non-linear Factor from Power (e.g. for (leading/trailing edge) devices)
		assertEquals((Float) 0.29516724f, PowerBehavior.NON_LINEAR.calculateFactorFromPower.apply(10000, 2000));

		// Linear Power from Factor
		assertEquals((Integer) 2000, PowerBehavior.LINEAR.calculatePowerFromFactor.apply(10000, 0.2f));

		// Non-linear Power from Factor
		assertEquals((Integer) 2000, PowerBehavior.NON_LINEAR.calculatePowerFromFactor.apply(10000, 0.29516724f));

		/*
		 * Null values
		 */
		assertNull(PowerBehavior.LINEAR.calculateFactorFromPower.apply(null, null));
		assertNull(PowerBehavior.LINEAR.calculateFactorFromPower.apply(null, 2000));
		assertNull(PowerBehavior.LINEAR.calculateFactorFromPower.apply(2000, null));
		assertNull(PowerBehavior.NON_LINEAR.calculateFactorFromPower.apply(null, null));
		assertNull(PowerBehavior.NON_LINEAR.calculateFactorFromPower.apply(null, 2000));
		assertNull(PowerBehavior.NON_LINEAR.calculateFactorFromPower.apply(2000, null));

		/*
		 * Range 0 - 10000 W in 100 W steps
		 */
		for (var i = 0; i <= 10000; i += 100) {
			assertEquals((Integer) i, //
					PowerBehavior.NON_LINEAR.calculatePowerFromFactor.apply(10000, //
							PowerBehavior.NON_LINEAR.calculateFactorFromPower.apply(10000, i)));
		}
	}
}
