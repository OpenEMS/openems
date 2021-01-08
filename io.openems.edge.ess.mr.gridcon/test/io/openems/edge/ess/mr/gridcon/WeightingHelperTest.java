package io.openems.edge.ess.mr.gridcon;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.ess.mr.gridcon.helper.DummyBattery;

public class WeightingHelperTest {

	public static final double DELTA = 0.000001;

	@Test
	public final void testIsBatteryReady() {

		Battery b = null;
		// should return false if there is no battery
		boolean result = WeightingHelper.isBatteryReady(b);
		assertFalse(result);

		// should return false if battery is not started
		b = new DummyBattery();
		result = WeightingHelper.isBatteryReady(b);
		assertFalse(result);

		// should return true if battery is running
		try {
			b.start();
		} catch (OpenemsNamedException e) {
			fail(e.getMessage());
		}
		result = WeightingHelper.isBatteryReady(b);
		assertTrue(result);

		// should return false if battery is stopped
		try {
			b.stop();
		} catch (OpenemsNamedException e) {
			fail(e.getMessage());
		}
		result = WeightingHelper.isBatteryReady(b);
		assertFalse(result);
	}

	@Test
	public final void testGetWeightingForCharge() {
		Battery b = null;
		// should be '0' is battery is not there or not working
		double result = WeightingHelper.getWeightingForCharge(b);
		assertEquals(0, result, DELTA);

		b = new DummyBattery();
		result = WeightingHelper.getWeightingForCharge(b);
		assertEquals(0, result, DELTA);

		try {
			b.start();
		} catch (OpenemsNamedException e1) {
			fail(e1.getMessage());
		}
		result = WeightingHelper.getWeightingForCharge(b);
		double expected = DummyBattery.DEFAULT_MAX_CHARGE_CURRENT * DummyBattery.DEFAULT_VOLTAGE;

		assertNotEquals(0, result, DELTA);
		assertEquals(expected, result, DELTA);

		try {
			b.stop();
		} catch (OpenemsNamedException e) {
			fail(e.getMessage());
		}
		result = WeightingHelper.getWeightingForCharge(b);
		assertEquals(0, result, DELTA);
	}

	@Test
	public final void testGetWeightingForDischarge() {
		Battery b = null;
		// should be '0' is battery is not there or not working
		double result = WeightingHelper.getWeightingForDischarge(b);
		assertEquals(0, result, DELTA);

		b = new DummyBattery();
		result = WeightingHelper.getWeightingForDischarge(b);
		assertEquals(0, result, DELTA);

		try {
			b.start();
		} catch (OpenemsNamedException e) {
			fail(e.getMessage());
		}
		result = WeightingHelper.getWeightingForDischarge(b);
		double expected = DummyBattery.DEFAULT_MAX_DISCHARGE_CURRENT * DummyBattery.DEFAULT_VOLTAGE;

		assertNotEquals(0, result, DELTA);
		assertEquals(expected, result, DELTA);

		try {
			b.stop();
		} catch (OpenemsNamedException e) {
			fail(e.getMessage());
		}
		result = WeightingHelper.getWeightingForDischarge(b);
		assertEquals(0, result, DELTA);
	}

	@Test
	public final void testGetWeightingForNoPowerNoBatteryOrStoppedBatteries() {
		DummyBattery b1 = null;
		DummyBattery b2 = null;
		DummyBattery b3 = null;

		// No batteries
		Float[] expected = { 0f, 0f, 0f };
		Float[] actual = WeightingHelper.getWeightingForNoPower(b1, b2, b3);
		assertArrayEquals(expected, actual);

		// One battery not running
		b1 = new DummyBattery();
		expected = new Float[] { 0f, 0f, 0f };
		actual = WeightingHelper.getWeightingForNoPower(b1, b2, b3);
		assertArrayEquals(expected, actual);

		// One battery running
		try {
			b1.start();
		} catch (OpenemsNamedException e) {
			fail("Battery could not be started");
		}
		expected = new Float[] { 1f, 0f, 0f };
		actual = WeightingHelper.getWeightingForNoPower(b1, b2, b3);
		assertArrayEquals(expected, actual);

		// Two batteries, one not running
		b3 = new DummyBattery();
		expected = new Float[] { 1f, 0f, 0f };
		actual = WeightingHelper.getWeightingForNoPower(b1, b2, b3);
		assertArrayEquals(expected, actual);

		// Two batteries
		b3.setVoltage(790);
		try {
			b3.start();
		} catch (OpenemsNamedException e) {
			fail("Battery could not be started");
		}
		expected = new Float[] { 1f, 0f, 1f };
		actual = WeightingHelper.getWeightingForNoPower(b1, b2, b3);
		assertArrayEquals(expected, actual);

		// Two batteries, one not running
		try {
			b1.stop();
		} catch (OpenemsNamedException e1) {
			fail("Battery could not be stopped");
		}
		expected = new Float[] { 0f, 0f, 1f };
		actual = WeightingHelper.getWeightingForNoPower(b1, b2, b3);
		assertArrayEquals(expected, actual);

		// Three batteries, one not running
		b2 = new DummyBattery();
		b2.setVoltage(810);
		try {
			b2.start();
		} catch (OpenemsNamedException e) {
			fail("Battery could not be started");
		}
		expected = new Float[] { 0f, 1f, 1f };
		actual = WeightingHelper.getWeightingForNoPower(b1, b2, b3);
		assertArrayEquals(expected, actual);

		// Three batteries
		try {
			b1.start();
		} catch (OpenemsNamedException e) {
			fail("Battery could not be started");
		}
		expected = new Float[] { 1f, 1f, 1f };
		actual = WeightingHelper.getWeightingForNoPower(b1, b2, b3);
		assertArrayEquals(expected, actual);
	}

	@Test
	public final void testGetWeightingActivePowerZero() {
		DummyBattery b1 = null;
		DummyBattery b2 = null;
		DummyBattery b3 = null;
		int activePower = 0;

		Float[] expected = { 0f, 0f, 0f };
		Float[] actual = WeightingHelper.getWeighting(activePower, b1, b2, b3);
		assertArrayEquals(expected, actual);

		b1 = new DummyBattery();
		expected = new Float[] { 0f, 0f, 0f };
		actual = WeightingHelper.getWeighting(activePower, b1, b2, b3);
		assertArrayEquals(expected, actual);

		try {
			b1.start();
		} catch (OpenemsNamedException e) {
			fail("Battery could not be started");
		}
		expected = new Float[] { 1f, 0f, 0f };
		actual = WeightingHelper.getWeighting(activePower, b1, b2, b3);
		assertArrayEquals(expected, actual);

		b3 = new DummyBattery();
		expected = new Float[] { 1f, 0f, 0f };
		actual = WeightingHelper.getWeighting(activePower, b1, b2, b3);
		assertArrayEquals(expected, actual);

		b3.setVoltage(790);
		try {
			b3.start();
		} catch (OpenemsNamedException e) {
			fail("Battery could not be started");
		}
		expected = new Float[] { 1f, 0f, 1f };
		actual = WeightingHelper.getWeighting(activePower, b1, b2, b3);
		assertArrayEquals(expected, actual);

		try {
			b1.stop();
		} catch (OpenemsNamedException e1) {
			fail("Battery could not be stopped");
		}
		expected = new Float[] { 0f, 0f, 1f };
		actual = WeightingHelper.getWeighting(activePower, b1, b2, b3);
		assertArrayEquals(expected, actual);

		b2 = new DummyBattery();
		b2.setVoltage(810);
		try {
			b2.start();
		} catch (OpenemsNamedException e) {
			fail("Battery could not be started");
		}
		expected = new Float[] { 0f, 1f, 1f };
		actual = WeightingHelper.getWeighting(activePower, b1, b2, b3);
		assertArrayEquals(expected, actual);

		try {
			b1.start();
		} catch (OpenemsNamedException e) {
			fail("Battery could not be started");
		}
		expected = new Float[] { 1f, 1f, 1f };
		actual = WeightingHelper.getWeighting(activePower, b1, b2, b3);
		assertArrayEquals(expected, actual);
	}

	@Test
	public final void testGetWeightingActivePowerCharge() {
		DummyBattery b1 = null;
		DummyBattery b2 = null;
		DummyBattery b3 = null;
		int activePower = -10;

		// no batteries
		Float[] expected = new Float[] { 0f, 0f, 0f };
		Float[] actual = WeightingHelper.getWeighting(activePower, b1, b2, b3);
		assertArrayEquals(expected, actual);

		// one battery not started
		b1 = new DummyBattery();
		expected = new Float[] { 0f, 0f, 0f };
		actual = WeightingHelper.getWeighting(activePower, b1, b2, b3);
		assertArrayEquals(expected, actual);

		// one battery started
		b1 = new DummyBattery();
		try {
			b1.start();
		} catch (OpenemsNamedException e) {
			fail("Battery could not be started");
		}
		float maxPower = DummyBattery.DEFAULT_VOLTAGE * DummyBattery.DEFAULT_MAX_CHARGE_CURRENT;
		expected = new Float[] { maxPower, 0f, 0f };
		actual = WeightingHelper.getWeighting(activePower, b1, b2, b3);
		assertArrayEquals(expected, actual);

		// three batteries, one battery started
		b1 = new DummyBattery();
		b2 = new DummyBattery();
		b3 = new DummyBattery();
		try {
			b2.start();
		} catch (OpenemsNamedException e) {
			fail("Battery could not be started");
		}
		maxPower = DummyBattery.DEFAULT_VOLTAGE * DummyBattery.DEFAULT_MAX_CHARGE_CURRENT;
		expected = new Float[] { 0f, maxPower, 0f };
		actual = WeightingHelper.getWeighting(activePower, b1, b2, b3);
		assertArrayEquals(expected, actual);

		// three batteries all started different voltages and current
		b1 = new DummyBattery();
		b2 = new DummyBattery();
		b3 = new DummyBattery();

		int b1Voltage = 650;
		int b1maxCurrent = 80;
		b1.setVoltage(b1Voltage);
		b1.setMaximalChargeCurrent(b1maxCurrent);

		int b2Voltage = 700;
		int b2maxCurrent = 80;
		b2.setVoltage(b2Voltage);
		b2.setMaximalChargeCurrent(b2maxCurrent);

		int b3Voltage = 800;
		int b3maxCurrent = 30;
		b3.setVoltage(b3Voltage);
		b3.setMaximalChargeCurrent(b3maxCurrent);

		try {
			b1.start();
		} catch (OpenemsNamedException e) {
			fail("Battery could not be started");
		}
		try {
			b2.start();
		} catch (OpenemsNamedException e) {
			fail("Battery could not be started");
		}
		try {
			b3.start();
		} catch (OpenemsNamedException e) {
			fail("Battery could not be started");
		}

		float maxPower1 = b1Voltage * b1maxCurrent;
		float maxPower2 = b2Voltage * b2maxCurrent;
		float maxPower3 = b3Voltage * b3maxCurrent;
		expected = new Float[] { maxPower1, maxPower2, maxPower3 };
		actual = WeightingHelper.getWeighting(activePower, b1, b2, b3);
		assertArrayEquals(expected, actual);
	}

	@Test
	public final void testGetWeightingActivePowerDischarge() {
		DummyBattery b1 = null;
		DummyBattery b2 = null;
		DummyBattery b3 = null;
		int activePower = 10;

		// no batteries
		Float[] expected = new Float[] { 0f, 0f, 0f };
		Float[] actual = WeightingHelper.getWeighting(activePower, b1, b2, b3);
		assertArrayEquals(expected, actual);

		// one battery not started
		b1 = new DummyBattery();
		expected = new Float[] { 0f, 0f, 0f };
		actual = WeightingHelper.getWeighting(activePower, b1, b2, b3);
		assertArrayEquals(expected, actual);

		// one battery started
		b1 = new DummyBattery();
		try {
			b1.start();
		} catch (OpenemsNamedException e) {
			fail("Battery could not be started");
		}
		float maxPower = DummyBattery.DEFAULT_VOLTAGE * DummyBattery.DEFAULT_MAX_DISCHARGE_CURRENT;
		expected = new Float[] { maxPower, 0f, 0f };
		actual = WeightingHelper.getWeighting(activePower, b1, b2, b3);
		assertArrayEquals(expected, actual);

		// three batteries, one battery started
		b1 = new DummyBattery();
		b2 = new DummyBattery();
		b3 = new DummyBattery();
		try {
			b2.start();
		} catch (OpenemsNamedException e) {
			fail("Battery could not be started");
		}
		maxPower = DummyBattery.DEFAULT_VOLTAGE * DummyBattery.DEFAULT_MAX_DISCHARGE_CURRENT;
		expected = new Float[] { 0f, maxPower, 0f };
		actual = WeightingHelper.getWeighting(activePower, b1, b2, b3);
		assertArrayEquals(expected, actual);

		// three batteries all started different voltages and current
		b1 = new DummyBattery();
		b2 = new DummyBattery();
		b3 = new DummyBattery();

		int b1Voltage = 650;
		int b1maxCurrent = 80;
		b1.setVoltage(b1Voltage);
		b1.setMaximalDischargeCurrent(b1maxCurrent);

		int b2Voltage = 700;
		int b2maxCurrent = 80;
		b2.setVoltage(b2Voltage);
		b2.setMaximalDischargeCurrent(b2maxCurrent);

		int b3Voltage = 800;
		int b3maxCurrent = 30;
		b3.setVoltage(b3Voltage);
		b3.setMaximalDischargeCurrent(b3maxCurrent);

		try {
			b1.start();
		} catch (OpenemsNamedException e) {
			fail("Battery could not be started");
		}
		try {
			b2.start();
		} catch (OpenemsNamedException e) {
			fail("Battery could not be started");
		}
		try {
			b3.start();
		} catch (OpenemsNamedException e) {
			fail("Battery could not be started");
		}

		float maxPower1 = b1Voltage * b1maxCurrent;
		float maxPower2 = b2Voltage * b2maxCurrent;
		float maxPower3 = b3Voltage * b3maxCurrent;
		expected = new Float[] { maxPower1, maxPower2, maxPower3 };
		actual = WeightingHelper.getWeighting(activePower, b1, b2, b3);
		assertArrayEquals(expected, actual);
	}

	@Test
	public final void testGetStringControlMode() {
		DummyBattery b1 = null;
		DummyBattery b2 = null;
		DummyBattery b3 = null;

		int expected = 0;
		int actual = WeightingHelper.getStringControlMode(b1, b2, b3);
		assertEquals(expected, actual);

		b2 = new DummyBattery();
		expected = 0;
		actual = WeightingHelper.getStringControlMode(b1, b2, b3);

		try {
			b2.start();
		} catch (OpenemsNamedException e) {
			fail("Battery could not be started");
		}
		expected = 8;
		actual = WeightingHelper.getStringControlMode(b1, b2, b3);
		assertEquals(expected, actual);

		try {
			b2.stop();
		} catch (OpenemsNamedException e1) {
			fail("Battery could not be stopped");
		}
		expected = 0;
		actual = WeightingHelper.getStringControlMode(b1, b2, b3);
		assertEquals(expected, actual);

		b1 = new DummyBattery();
		expected = 0;
		actual = WeightingHelper.getStringControlMode(b1, b2, b3);
		assertEquals(expected, actual);

		try {
			b1.start();
		} catch (OpenemsNamedException e) {
			fail("Battery could not be started");
		}
		expected = 1;
		actual = WeightingHelper.getStringControlMode(b1, b2, b3);
		assertEquals(expected, actual);

		try {
			b2.start();
		} catch (OpenemsNamedException e) {
			fail("Battery could not be started");
		}
		expected = 9;
		actual = WeightingHelper.getStringControlMode(b1, b2, b3);
		assertEquals(expected, actual);

		b3 = new DummyBattery();
		try {
			b1.stop();
		} catch (OpenemsNamedException e1) {
			fail("Battery could not be stopped");
		}
		try {
			b2.stop();
		} catch (OpenemsNamedException e1) {
			fail("Battery could not be stopped");
		}
		expected = 0;
		actual = WeightingHelper.getStringControlMode(b1, b2, b3);
		assertEquals(expected, actual);

		try {
			b3.start();
		} catch (OpenemsNamedException e) {
			fail("Battery could not be started");
		}
		expected = 64;
		actual = WeightingHelper.getStringControlMode(b1, b2, b3);
		assertEquals(expected, actual);

		try {
			b1.start();
		} catch (OpenemsNamedException e) {
			fail("Battery could not be started");
		}
		expected = 65;
		actual = WeightingHelper.getStringControlMode(b1, b2, b3);
		assertEquals(expected, actual);

		try {
			b2.start();
		} catch (OpenemsNamedException e) {
			fail("Battery could not be started");
		}
		expected = 73;
		actual = WeightingHelper.getStringControlMode(b1, b2, b3);
		assertEquals(expected, actual);

		try {
			b1.stop();
		} catch (OpenemsNamedException e) {
			fail("Battery could not be stopped");
		}
		expected = 72;
		actual = WeightingHelper.getStringControlMode(b1, b2, b3);
		assertEquals(expected, actual);
	}

}
