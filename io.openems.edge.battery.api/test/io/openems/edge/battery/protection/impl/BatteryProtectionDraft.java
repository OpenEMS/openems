package io.openems.edge.battery.protection.impl;

import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.junit.Test;

import io.openems.edge.battery.protection.BatteryProtection;
import io.openems.edge.battery.protection.ChargeMaxCurrentHandler;
import io.openems.edge.battery.protection.DischargeMaxCurrentHandler;
import io.openems.edge.battery.protection.ChargeMaxCurrentHandler.ForceDischargeParams;
import io.openems.edge.battery.protection.DischargeMaxCurrentHandler.ForceChargeParams;
import io.openems.edge.common.linecharacteristic.PolyLine;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.TimeLeapClock;

/*
 * All Voltage in [mV]. All Percentage in range [0,1]. All Temperature in
 * [degC].
 */

public class BatteryProtectionDraft {
//
//	@Test
//	public void test2() {
//		final TimeLeapClock clock = new TimeLeapClock(Instant.parse("2020-01-01T01:00:00.00Z"), ZoneOffset.UTC);
//		final DummyComponentManager cm = new DummyComponentManager(clock);
//
//		ChargeMaxCurrentHandler maxChargeCurrentHandler = ChargeMaxCurrentHandler.create(cm, 40) //
//				.setVoltageToPercent(CHARGE_VOLTAGE_TO_PERCENT) //
//				.setTemperatureToPercent(CHARGE_TEMPERATURE_TO_PERCENT) //
//				.setMaxIncreasePerSecond(MAX_INCREASE_AMPERE_PER_SECOND) //
//				.setForceDischarge(FORCE_DISCHARGE) //
//				.build();
//
//		// Min-Cell-Voltage
//		assertEquals(0.1 /* 10 % */, (double) maxChargeCurrentHandler.voltageToPercent.getValue(2950), 0.1);
//
//		// Max-Cell-Voltage
//		assertEquals(1 /* 100 % */, (double) maxChargeCurrentHandler.voltageToPercent.getValue(3300), 0.1);
//
//		// Min-Cell-Temperature
//		assertEquals(1 /* 100 % */, (double) maxChargeCurrentHandler.temperatureToPercent.getValue(16), 0.1);
//
//		// Max-Cell-Temperature
//		assertEquals(1 /* 100 % */, (double) maxChargeCurrentHandler.temperatureToPercent.getValue(16), 0.1);
//
//		// 10 % -> 4 A; 100 % -> 40 A
//		assertEquals(4, (double) maxChargeCurrentHandler.percentToAmpere(0.1), 0.1);
//		assertEquals(40, (double) maxChargeCurrentHandler.percentToAmpere(1.), 0.1);
//
//		// Integration test
//		assertEquals(4, maxChargeCurrentHandler.calculateCurrentLimit(2950, 3300, 16, 17, 40));
//		clock.leap(900, ChronoUnit.MILLIS);
//		assertEquals(4, maxChargeCurrentHandler.calculateCurrentLimit(3000, 3300, 16, 17, 40));
//		clock.leap(900, ChronoUnit.MILLIS);
//		assertEquals(5, maxChargeCurrentHandler.calculateCurrentLimit(3050, 3300, 16, 17, 40));
//		clock.leap(10, ChronoUnit.SECONDS);
//		assertEquals(10, maxChargeCurrentHandler.calculateCurrentLimit(3050, 3300, 16, 17, 40));
//		clock.leap(1, ChronoUnit.HOURS);
//		assertEquals(40, maxChargeCurrentHandler.calculateCurrentLimit(3050, 3300, 16, 17, 40));
//		clock.leap(1, ChronoUnit.HOURS);
//		assertEquals(40, maxChargeCurrentHandler.calculateCurrentLimit(3050, 3449, 16, 17, 40));
//		clock.leap(1, ChronoUnit.HOURS);
//		assertEquals(1, maxChargeCurrentHandler.calculateCurrentLimit(3400, 3649, 16, 17, 40));
//		clock.leap(1, ChronoUnit.HOURS);
//		assertEquals(0, maxChargeCurrentHandler.calculateCurrentLimit(3400, 3650, 16, 17, 40));
//
//		// Start Force-Discharge
//		clock.leap(1, ChronoUnit.HOURS);
//		assertEquals(-1, maxChargeCurrentHandler.calculateCurrentLimit(3400, 3660, 16, 17, 40));
//		assertEquals(-1, maxChargeCurrentHandler.calculateCurrentLimit(3400, 3650, 16, 17, 40));
//		// Block Charge
//		clock.leap(1, ChronoUnit.SECONDS);
//		assertEquals(0, maxChargeCurrentHandler.calculateCurrentLimit(3400, 3639, 16, 17, 40));
//		assertEquals(0, maxChargeCurrentHandler.calculateCurrentLimit(3400, 3600, 16, 17, 40));
//		assertEquals(0, maxChargeCurrentHandler.calculateCurrentLimit(3400, 3450, 16, 17, 40));
//		// Finish Force-Discharge logic
//		clock.leap(2, ChronoUnit.SECONDS);
//		assertEquals(1, maxChargeCurrentHandler.calculateCurrentLimit(3400, 3449, 16, 17, 40));
//		clock.leap(2, ChronoUnit.SECONDS);
//		assertEquals(2, maxChargeCurrentHandler.calculateCurrentLimit(3400, 3449, 16, 17, 40));
//
//		PolyLine.printAsCsv(maxChargeCurrentHandler.voltageToPercent);
//		PolyLine.printAsCsv(maxChargeCurrentHandler.temperatureToPercent);
//	}
//
//	@Test
//	public void testChargeVoltageToPercent() {
//		PolyLine p = CHARGE_VOLTAGE_TO_PERCENT;
//		assertEquals(0.1, p.getValue(2500), 0.001);
//		assertEquals(0.1, p.getValue(Math.nextDown(3000)), 0.001);
//		assertEquals(1, p.getValue(3000), 0.001);
//		assertEquals(1, p.getValue(3450), 0.001);
//		assertEquals(0.752, p.getValue(3500), 0.001);
//		assertEquals(0.257, p.getValue(3600), 0.001);
//		assertEquals(0.01, p.getValue(Math.nextDown(3650)), 0.001);
//		assertEquals(0, p.getValue(3650), 0.001);
//		assertEquals(0, p.getValue(4000), 0.001);
//	}
//
//	@Test
//	public void testChargeTemperatureToPercent() {
//		PolyLine p = CHARGE_TEMPERATURE_TO_PERCENT;
//		assertEquals(0, p.getValue(-20), 0.001);
//		assertEquals(0, p.getValue(Math.nextDown(-10)), 0.001);
//		assertEquals(0.215, p.getValue(-10), 0.001);
//		assertEquals(0.215, p.getValue(0), 0.001);
//		assertEquals(0.27, p.getValue(0.5), 0.001);
//		assertEquals(0.325, p.getValue(1), 0.001);
//		assertEquals(0.325, p.getValue(5), 0.001);
//		assertEquals(0.4875, p.getValue(5.5), 0.001);
//		assertEquals(0.65, p.getValue(6), 0.001);
//		assertEquals(0.65, p.getValue(15), 0.001);
//		assertEquals(0.825, p.getValue(15.5), 0.001);
//		assertEquals(1, p.getValue(16), 0.001);
//		assertEquals(1, p.getValue(44), 0.001);
//		assertEquals(0.825, p.getValue(44.5), 0.001);
//		assertEquals(0.65, p.getValue(45), 0.001);
//		assertEquals(0.65, p.getValue(49), 0.001);
//		assertEquals(0.4875, p.getValue(49.5), 0.001);
//		assertEquals(0.325, p.getValue(50), 0.001);
//		assertEquals(0.325, p.getValue(54), 0.001);
//		assertEquals(0.1625, p.getValue(54.5), 0.001);
//		assertEquals(0, p.getValue(55), 0.001);
//		assertEquals(0, p.getValue(100), 0.001);
//	}
//
//	@Test
//	public void testMaxIncreasePerSecond() {
//		final TimeLeapClock clock = new TimeLeapClock(Instant.parse("2020-01-01T01:00:00.00Z"), ZoneOffset.UTC);
//		final DummyComponentManager cm = new DummyComponentManager(clock);
//		ChargeMaxCurrentHandler sut = ChargeMaxCurrentHandler.create(cm, 40) //
//				.setMaxIncreasePerSecond(MAX_INCREASE_AMPERE_PER_SECOND) //
//				.build();
//		sut.lastMaxIncreaseAmpereLimit = 0.;
//		sut.lastResultTimestamp = Instant.now(clock);
//
//		clock.leap(1, ChronoUnit.SECONDS);
//		assertEquals(0.5, (double) sut.getMaxIncreaseAmpereLimit(), 0.001);
//		sut.lastMaxIncreaseAmpereLimit = 0.5;
//
//		clock.leap(1, ChronoUnit.SECONDS);
//		assertEquals(1, (double) sut.getMaxIncreaseAmpereLimit(), 0.001);
//		sut.lastMaxIncreaseAmpereLimit = 1.;
//
//		clock.leap(800, ChronoUnit.MILLIS);
//		assertEquals(1.4, (double) sut.getMaxIncreaseAmpereLimit(), 0.001);
//	}
//
//	@Test
//	public void testForceDischarge() {
//		final TimeLeapClock clock = new TimeLeapClock(Instant.parse("2020-01-01T01:00:00.00Z"), ZoneOffset.UTC);
//		final DummyComponentManager cm = new DummyComponentManager(clock);
//		ChargeMaxCurrentHandler sut = ChargeMaxCurrentHandler.create(cm, 40) //
//				.setForceDischarge(3660, 3640, 3450) //
//				.build();
//		assertEquals(null, sut.getForceCurrent(3000, 3650));
//		assertEquals(-1., sut.getForceCurrent(3000, 3660), 0.001);
//		assertEquals(-1., sut.getForceCurrent(3000, 3650), 0.001);
//		assertEquals(0, sut.getForceCurrent(3000, 3639), 0.001);
//		assertEquals(0, sut.getForceCurrent(3000, 3600), 0.001);
//		assertEquals(0, sut.getForceCurrent(3000, 3500), 0.001);
//		assertEquals(null, sut.getForceCurrent(3000, 3449));
//	}

}
