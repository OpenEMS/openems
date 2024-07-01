package io.openems.edge.ess.mr.gridcon;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.ess.mr.gridcon.helper.DummyBattery;

public class WeightingHelperTest {

	public static final double DELTA = 0.000001;

	@Test
	public final void testIsBatteryReady() throws OpenemsNamedException {

		Battery b = null;
		// should return false if there is no battery
		boolean result = WeightingHelper.isBatteryReady(b);
		assertFalse(result);

		// should return false if battery is not started
		b = new DummyBattery();
		result = WeightingHelper.isBatteryReady(b);
		assertFalse(result);

		// should return true if battery is running
		b.start();
		result = WeightingHelper.isBatteryReady(b);
		assertTrue(result);

		// should return false if battery is stopped
		b.stop();
		result = WeightingHelper.isBatteryReady(b);
		assertFalse(result);
	}

	@Test
	public final void testGetWeightingForCharge() throws OpenemsNamedException {
		Battery b = null;
		// should be '0' is battery is not there or not working
		double result = WeightingHelper.getWeightingForCharge(b);
		assertEquals(0, result, DELTA);

		b = new DummyBattery();
		result = WeightingHelper.getWeightingForCharge(b);
		assertEquals(0, result, DELTA);

		b.start();
		result = WeightingHelper.getWeightingForCharge(b);
		double expected = DummyBattery.DEFAULT_MAX_CHARGE_CURRENT * DummyBattery.DEFAULT_VOLTAGE;

		assertNotEquals(0, result, DELTA);
		assertEquals(expected, result, DELTA);

		b.stop();
		result = WeightingHelper.getWeightingForCharge(b);
		assertEquals(0, result, DELTA);
	}

	@Test
	public final void testGetWeightingForDischarge() throws OpenemsNamedException {
		Battery b = null;
		// should be '0' is battery is not there or not working
		double result = WeightingHelper.getWeightingForDischarge(b);
		assertEquals(0, result, DELTA);

		b = new DummyBattery();
		result = WeightingHelper.getWeightingForDischarge(b);
		assertEquals(0, result, DELTA);

		b.start();
		result = WeightingHelper.getWeightingForDischarge(b);
		double expected = DummyBattery.DEFAULT_MAX_DISCHARGE_CURRENT * DummyBattery.DEFAULT_VOLTAGE;

		assertNotEquals(0, result, DELTA);
		assertEquals(expected, result, DELTA);

		b.stop();
		result = WeightingHelper.getWeightingForDischarge(b);
		assertEquals(0, result, DELTA);
	}

	@Test
	public final void testGetWeightingForNoPowerNoBatteryOrStoppedBatteries() throws OpenemsNamedException {
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
		b1.start();
		expected = new Float[] { 1f, 0f, 0f };
		actual = WeightingHelper.getWeightingForNoPower(b1, b2, b3);
		assertArrayEquals(expected, actual);

		// Two batteries, one not running
		b3 = new DummyBattery();
		expected = new Float[] { 1f, 0f, 0f };
		actual = WeightingHelper.getWeightingForNoPower(b1, b2, b3);
		assertArrayEquals(expected, actual);

		// Two batteries
		b3 //
				.withVoltage(790) //
				.start();
		expected = new Float[] { 1f, 0f, 1f };
		actual = WeightingHelper.getWeightingForNoPower(b1, b2, b3);
		assertArrayEquals(expected, actual);

		// Two batteries, one not running
		b1.stop();
		expected = new Float[] { 0f, 0f, 1f };
		actual = WeightingHelper.getWeightingForNoPower(b1, b2, b3);
		assertArrayEquals(expected, actual);

		// Three batteries, one not running
		b2 = new DummyBattery() //
				.withVoltage(810);
		b2.start();
		expected = new Float[] { 0f, 1f, 1f };
		actual = WeightingHelper.getWeightingForNoPower(b1, b2, b3);
		assertArrayEquals(expected, actual);

		// Three batteries
		b1.start();
		expected = new Float[] { 1f, 1f, 1f };
		actual = WeightingHelper.getWeightingForNoPower(b1, b2, b3);
		assertArrayEquals(expected, actual);
	}

	@Test
	public final void testGetWeightingActivePowerZero() throws OpenemsNamedException {
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

		b1.start();
		expected = new Float[] { 1f, 0f, 0f };
		actual = WeightingHelper.getWeighting(activePower, b1, b2, b3);
		assertArrayEquals(expected, actual);

		b3 = new DummyBattery();
		expected = new Float[] { 1f, 0f, 0f };
		actual = WeightingHelper.getWeighting(activePower, b1, b2, b3);
		assertArrayEquals(expected, actual);

		b3 //
				.withVoltage(790) //
				.start();
		expected = new Float[] { 1f, 0f, 1f };
		actual = WeightingHelper.getWeighting(activePower, b1, b2, b3);
		assertArrayEquals(expected, actual);

		b1.stop();
		expected = new Float[] { 0f, 0f, 1f };
		actual = WeightingHelper.getWeighting(activePower, b1, b2, b3);
		assertArrayEquals(expected, actual);

		b2 = new DummyBattery() //
				.withVoltage(810);
		b2.start();
		expected = new Float[] { 0f, 1f, 1f };
		actual = WeightingHelper.getWeighting(activePower, b1, b2, b3);
		assertArrayEquals(expected, actual);

		b1.start();
		expected = new Float[] { 1f, 1f, 1f };
		actual = WeightingHelper.getWeighting(activePower, b1, b2, b3);
		assertArrayEquals(expected, actual);
	}

	@Test
	public final void testGetWeightingActivePowerCharge() throws OpenemsNamedException {
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
		b1.start();
		float maxPower = DummyBattery.DEFAULT_VOLTAGE * DummyBattery.DEFAULT_MAX_CHARGE_CURRENT;
		expected = new Float[] { maxPower, 0f, 0f };
		actual = WeightingHelper.getWeighting(activePower, b1, b2, b3);
		assertArrayEquals(expected, actual);

		// three batteries, one battery started
		b1 = new DummyBattery();
		b2 = new DummyBattery();
		b3 = new DummyBattery();
		b2.start();
		maxPower = DummyBattery.DEFAULT_VOLTAGE * DummyBattery.DEFAULT_MAX_CHARGE_CURRENT;
		expected = new Float[] { 0f, maxPower, 0f };
		actual = WeightingHelper.getWeighting(activePower, b1, b2, b3);
		assertArrayEquals(expected, actual);

		// three batteries all started different voltages and current
		final int b1Voltage = 650;
		final int b1maxCurrent = 80;
		final int b2Voltage = 700;
		final int b2maxCurrent = 80;
		final int b3Voltage = 800;
		final int b3maxCurrent = 30;

		b1 = new DummyBattery() //
				.withVoltage(b1Voltage) //
				.withChargeMaxCurrent(b1maxCurrent);
		b2 = new DummyBattery() //
				.withVoltage(b2Voltage) //
				.withChargeMaxCurrent(b2maxCurrent);
		b3 = new DummyBattery() //
				.withVoltage(b3Voltage) //
				.withChargeMaxCurrent(b3maxCurrent);

		b1.start();
		b2.start();
		b3.start();

		float maxPower1 = b1Voltage * b1maxCurrent;
		float maxPower2 = b2Voltage * b2maxCurrent;
		float maxPower3 = b3Voltage * b3maxCurrent;
		expected = new Float[] { maxPower1, maxPower2, maxPower3 };
		actual = WeightingHelper.getWeighting(activePower, b1, b2, b3);
		assertArrayEquals(expected, actual);
	}

	@Test
	public final void testGetWeightingActivePowerDischarge() throws OpenemsNamedException {
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
		b1.start();
		float maxPower = DummyBattery.DEFAULT_VOLTAGE * DummyBattery.DEFAULT_MAX_DISCHARGE_CURRENT;
		expected = new Float[] { maxPower, 0f, 0f };
		actual = WeightingHelper.getWeighting(activePower, b1, b2, b3);
		assertArrayEquals(expected, actual);

		// three batteries, one battery started
		b1 = new DummyBattery();
		b2 = new DummyBattery();
		b3 = new DummyBattery();
		b2.start();
		maxPower = DummyBattery.DEFAULT_VOLTAGE * DummyBattery.DEFAULT_MAX_DISCHARGE_CURRENT;
		expected = new Float[] { 0f, maxPower, 0f };
		actual = WeightingHelper.getWeighting(activePower, b1, b2, b3);
		assertArrayEquals(expected, actual);

		// three batteries all started different voltages and current
		final int b1Voltage = 650;
		final int b1maxCurrent = 80;
		final int b2Voltage = 700;
		final int b2maxCurrent = 80;
		final int b3Voltage = 800;
		final int b3maxCurrent = 30;

		b1 = new DummyBattery() //
				.withVoltage(b1Voltage) //
				.withDischargeMaxCurrent(b1maxCurrent);
		b2 = new DummyBattery() //
				.withVoltage(b2Voltage) //
				.withDischargeMaxCurrent(b2maxCurrent);
		b3 = new DummyBattery() //
				.withVoltage(b3Voltage) //
				.withDischargeMaxCurrent(b3maxCurrent);

		b1.start();
		b2.start();
		b3.start();

		float maxPower1 = b1Voltage * b1maxCurrent;
		float maxPower2 = b2Voltage * b2maxCurrent;
		float maxPower3 = b3Voltage * b3maxCurrent;
		expected = new Float[] { maxPower1, maxPower2, maxPower3 };
		actual = WeightingHelper.getWeighting(activePower, b1, b2, b3);
		assertArrayEquals(expected, actual);
	}

	@Test
	public final void testGetStringControlMode() throws OpenemsNamedException {
		DummyBattery b1 = null;
		DummyBattery b2 = null;
		DummyBattery b3 = null;

		int expected = 0;
		int actual = WeightingHelper.getStringControlMode(b1, b2, b3);
		assertEquals(expected, actual);

		b2 = new DummyBattery();
		expected = 0;
		actual = WeightingHelper.getStringControlMode(b1, b2, b3);

		b2.start();
		expected = 8;
		actual = WeightingHelper.getStringControlMode(b1, b2, b3);
		assertEquals(expected, actual);

		b2.stop();
		expected = 0;
		actual = WeightingHelper.getStringControlMode(b1, b2, b3);
		assertEquals(expected, actual);

		b1 = new DummyBattery();
		expected = 0;
		actual = WeightingHelper.getStringControlMode(b1, b2, b3);
		assertEquals(expected, actual);

		b1.start();
		expected = 1;
		actual = WeightingHelper.getStringControlMode(b1, b2, b3);
		assertEquals(expected, actual);

		b2.start();
		expected = 9;
		actual = WeightingHelper.getStringControlMode(b1, b2, b3);
		assertEquals(expected, actual);

		b3 = new DummyBattery();
		b1.stop();
		b2.stop();
		expected = 0;
		actual = WeightingHelper.getStringControlMode(b1, b2, b3);
		assertEquals(expected, actual);

		b3.start();
		expected = 64;
		actual = WeightingHelper.getStringControlMode(b1, b2, b3);
		assertEquals(expected, actual);

		b1.start();
		expected = 65;
		actual = WeightingHelper.getStringControlMode(b1, b2, b3);
		assertEquals(expected, actual);

		b2.start();
		expected = 73;
		actual = WeightingHelper.getStringControlMode(b1, b2, b3);
		assertEquals(expected, actual);

		b1.stop();
		expected = 72;
		actual = WeightingHelper.getStringControlMode(b1, b2, b3);
		assertEquals(expected, actual);
	}

}
