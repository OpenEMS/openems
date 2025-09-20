package io.openems.edge.battery.protection.currenthandler;

import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.junit.Test;

import io.openems.common.test.TimeLeapClock;
import io.openems.edge.battery.protection.BatteryProtectionTest;
import io.openems.edge.common.linecharacteristic.PolyLine;
import io.openems.edge.common.test.DummyComponentManager;

public class MaxCurrentHandlerTest {

	@Test
	public void testGetMinCellVoltageToPercentLimit() {
		final TimeLeapClock clock = new TimeLeapClock(Instant.parse("2020-01-01T01:00:00.00Z"), ZoneOffset.UTC);
		final DummyComponentManager cm = new DummyComponentManager(clock);
		ChargeMaxCurrentHandler sut = ChargeMaxCurrentHandler
				.create(cm, BatteryProtectionTest.INITIAL_BMS_MAX_EVER_CURRENT) //
				.setVoltageToPercent(BatteryProtectionTest.CHARGE_VOLTAGE_TO_PERCENT) //
				.build();
		assertEquals(80, (double) sut.getMinCellVoltageToPercentLimit(3001), 0.1);
		assertEquals(8, (double) sut.getMinCellVoltageToPercentLimit(2960), 0.1);
		assertEquals(80, (double) sut.getMinCellVoltageToPercentLimit(3001), 0.1);
		assertEquals(53.867, (double) sut.getMinCellVoltageToPercentLimit(3500), 0.1);
		assertEquals(1.6, (double) sut.getMinCellVoltageToPercentLimit(3600), 0.1);
		// Limit is not opened up
		assertEquals(1.6, (double) sut.getMinCellVoltageToPercentLimit(3500), 0.1);
		assertEquals(0, (double) sut.getMinCellVoltageToPercentLimit(3700), 0.1);
	}

	@Test
	public void testGetMaxCellVoltageToPercentLimit() {
		final TimeLeapClock clock = new TimeLeapClock(Instant.parse("2020-01-01T01:00:00.00Z"), ZoneOffset.UTC);
		final DummyComponentManager cm = new DummyComponentManager(clock);
		ChargeMaxCurrentHandler sut = ChargeMaxCurrentHandler
				.create(cm, BatteryProtectionTest.INITIAL_BMS_MAX_EVER_CURRENT) //
				.setVoltageToPercent(BatteryProtectionTest.CHARGE_VOLTAGE_TO_PERCENT) //
				.build();
		assertEquals(80, (double) sut.getMinCellVoltageToPercentLimit(3450), 0.1);
		assertEquals(74.7, (double) sut.getMinCellVoltageToPercentLimit(3460), 0.1);
		assertEquals(69.5, (double) sut.getMinCellVoltageToPercentLimit(3470), 0.1);
	}

	@Test
	public void testDischargeOpenUpLimit() {
		final TimeLeapClock clock = new TimeLeapClock(Instant.parse("2020-01-01T01:00:00.00Z"), ZoneOffset.UTC);
		final DummyComponentManager cm = new DummyComponentManager(clock);
		DischargeMaxCurrentHandler sut = DischargeMaxCurrentHandler
				.create(cm, BatteryProtectionTest.INITIAL_BMS_MAX_EVER_CURRENT) //
				.setVoltageToPercent(BatteryProtectionTest.DISCHARGE_VOLTAGE_TO_PERCENT) //
				.build();
		assertEquals(32.5, (double) sut.getMinCellVoltageToPercentLimit(2950), 0.1);
		assertEquals(4, (double) sut.getMinCellVoltageToPercentLimit(2920), 0.1);
		assertEquals(4, (double) sut.getMinCellVoltageToPercentLimit(2901), 0.1);
		assertEquals(0, (double) sut.getMinCellVoltageToPercentLimit(2900), 0.1);
		assertEquals(0, (double) sut.getMinCellVoltageToPercentLimit(2899), 0.1);
		assertEquals(0, (double) sut.getMinCellVoltageToPercentLimit(2900), 0.1);
		assertEquals(0, (double) sut.getMinCellVoltageToPercentLimit(2901), 0.1);
		assertEquals(0, (double) sut.getMinCellVoltageToPercentLimit(2950), 0.1);
		assertEquals(0, (double) sut.getMinCellVoltageToPercentLimit(2950), 0.1);
		assertEquals(80, (double) sut.getMinCellVoltageToPercentLimit(3000), 0.1);
	}

	@Test
	public void testChargeOpenUpLimit() {
		final TimeLeapClock clock = new TimeLeapClock(Instant.parse("2020-01-01T01:00:00.00Z"), ZoneOffset.UTC);
		final DummyComponentManager cm = new DummyComponentManager(clock);
		ChargeMaxCurrentHandler sut = ChargeMaxCurrentHandler
				.create(cm, BatteryProtectionTest.INITIAL_BMS_MAX_EVER_CURRENT) //
				.setVoltageToPercent(BatteryProtectionTest.CHARGE_VOLTAGE_TO_PERCENT) //
				.build();
		assertEquals(80, (double) sut.getMaxCellVoltageToPercentLimit(3400), 0.1);
		assertEquals(80, (double) sut.getMaxCellVoltageToPercentLimit(3450), 0.1);
		assertEquals(53.9, (double) sut.getMaxCellVoltageToPercentLimit(3500), 0.1);
		assertEquals(27.7, (double) sut.getMaxCellVoltageToPercentLimit(3550), 0.1);
		assertEquals(1.6, (double) sut.getMaxCellVoltageToPercentLimit(3600), 0.1);
		assertEquals(1.6, (double) sut.getMaxCellVoltageToPercentLimit(3620), 0.1);
		// smallest ever is "0"
		assertEquals(0, (double) sut.getMaxCellVoltageToPercentLimit(3650), 0.1);
		assertEquals(0, (double) sut.getMaxCellVoltageToPercentLimit(3620), 0.1);
		assertEquals(0, (double) sut.getMaxCellVoltageToPercentLimit(3600), 0.1);
		assertEquals(0, (double) sut.getMaxCellVoltageToPercentLimit(3550), 0.1);
		assertEquals(0, (double) sut.getMaxCellVoltageToPercentLimit(3500), 0.1);
		assertEquals(0, (double) sut.getMaxCellVoltageToPercentLimit(3450), 0.1);
		assertEquals(0, (double) sut.getMaxCellVoltageToPercentLimit(3400), 0.1);
		// Open up fully only at 3350 mV
		assertEquals(80, (double) sut.getMaxCellVoltageToPercentLimit(3350), 0.1);

		assertEquals(80, (double) sut.getMaxCellVoltageToPercentLimit(3400), 0.1);
		assertEquals(80, (double) sut.getMaxCellVoltageToPercentLimit(3450), 0.1);
		assertEquals(53.9, (double) sut.getMaxCellVoltageToPercentLimit(3500), 0.1);
		assertEquals(27.7, (double) sut.getMaxCellVoltageToPercentLimit(3550), 0.1);
		assertEquals(1.6, (double) sut.getMaxCellVoltageToPercentLimit(3600), 0.1);
		// smallest ever is "1.6"
		assertEquals(1.6, (double) sut.getMaxCellVoltageToPercentLimit(3620), 0.1);
		assertEquals(1.6, (double) sut.getMaxCellVoltageToPercentLimit(3550), 0.1);
		assertEquals(1.6, (double) sut.getMaxCellVoltageToPercentLimit(3500), 0.1);
		assertEquals(1.6, (double) sut.getMaxCellVoltageToPercentLimit(3450), 0.1);
		assertEquals(1.6, (double) sut.getMaxCellVoltageToPercentLimit(3400), 0.1);
		// Open up fully only at 3350 mV
		assertEquals(80, (double) sut.getMaxCellVoltageToPercentLimit(3350), 0.1);
	}

	@Test
	public void testForceDischarge() {
		final TimeLeapClock clock = new TimeLeapClock(Instant.parse("2020-01-01T01:00:00.00Z"), ZoneOffset.UTC);
		final DummyComponentManager cm = new DummyComponentManager(clock);
		ChargeMaxCurrentHandler sut = ChargeMaxCurrentHandler.create(cm, 40) //
				.setForceDischarge(3660, 3640, 3450) //
				.build();
		// Before Force-Discharge limit -> no force discharge
		assertEquals(null, sut.getForceCurrent(3000, 3650));
		// Start WAIT_FOR_FORCE_MODE (60 seconds) -> no force discharge
		assertEquals(null, sut.getForceCurrent(3000, 3660));
		clock.leap(1, ChronoUnit.MINUTES);
		// Enter FORCE_MODE -> force discharge
		assertEquals(-2., sut.getForceCurrent(3000, 3660), 0.001);
		clock.leap(1, ChronoUnit.SECONDS);
		assertEquals(-2., sut.getForceCurrent(3000, 3650), 0.001);
		// Enter BLOCK_MODE -> no charge/discharge
		assertEquals(0, sut.getForceCurrent(3000, 3639), 0.001);
		assertEquals(0, sut.getForceCurrent(3000, 3600), 0.001);
		assertEquals(0, sut.getForceCurrent(3000, 3500), 0.001);
		// Ende Force-Discharge Mode
		assertEquals(null, sut.getForceCurrent(3000, 3449));
	}

	@Test
	public void testMaxIncreasePerSecond() {
		final TimeLeapClock clock = new TimeLeapClock(Instant.parse("2020-01-01T01:00:00.00Z"), ZoneOffset.UTC);
		final DummyComponentManager cm = new DummyComponentManager(clock);
		ChargeMaxCurrentHandler sut = ChargeMaxCurrentHandler.create(cm, 40) //
				.setMaxIncreasePerSecond(BatteryProtectionTest.MAX_INCREASE_AMPERE_PER_SECOND) //
				.build();
		sut.lastCurrentLimit = 0.;
		sut.lastResultTimestamp = Instant.now(clock);

		clock.leap(1, ChronoUnit.SECONDS);
		assertEquals(0.5, (double) sut.getMaxIncreaseAmpereLimit(), 0.001);
		sut.lastCurrentLimit = 0.5;

		clock.leap(1, ChronoUnit.SECONDS);
		assertEquals(1, (double) sut.getMaxIncreaseAmpereLimit(), 0.001);
		sut.lastCurrentLimit = 1.;

		clock.leap(800, ChronoUnit.MILLIS);
		assertEquals(1.4, (double) sut.getMaxIncreaseAmpereLimit(), 0.001);
	}

	@Test
	public void testChargeVoltageToPercent() {
		PolyLine p = BatteryProtectionTest.CHARGE_VOLTAGE_TO_PERCENT;
		assertEquals(0.1, p.getValue(2500), 0.001);
		assertEquals(0.1, p.getValue(3000), 0.001);
		assertEquals(1, p.getValue(Math.nextUp(3000)), 0.001);
		assertEquals(1, p.getValue(3450), 0.001);
		assertEquals(0.673, p.getValue(3500), 0.001);
		assertEquals(0.02, p.getValue(3600), 0.001);
		assertEquals(0.02, p.getValue(Math.nextDown(3650)), 0.001);
		assertEquals(0, p.getValue(3650), 0.001);
		assertEquals(0, p.getValue(4000), 0.001);
	}

	@Test
	public void testChargeTemperatureToPercent() {
		PolyLine p = BatteryProtectionTest.CHARGE_TEMPERATURE_TO_PERCENT;
		assertEquals(0, p.getValue(-20), 0.001);
		assertEquals(0, p.getValue(Math.nextDown(-10)), 0.001);
		assertEquals(0.215, p.getValue(-10), 0.001);
		assertEquals(0.215, p.getValue(0), 0.001);
		assertEquals(0.27, p.getValue(0.5), 0.001);
		assertEquals(0.325, p.getValue(1), 0.001);
		assertEquals(0.325, p.getValue(5), 0.001);
		assertEquals(0.4875, p.getValue(5.5), 0.001);
		assertEquals(0.65, p.getValue(6), 0.001);
		assertEquals(0.65, p.getValue(15), 0.001);
		assertEquals(0.825, p.getValue(15.5), 0.001);
		assertEquals(1, p.getValue(16), 0.001);
		assertEquals(1, p.getValue(44), 0.001);
		assertEquals(0.825, p.getValue(44.5), 0.001);
		assertEquals(0.65, p.getValue(45), 0.001);
		assertEquals(0.65, p.getValue(49), 0.001);
		assertEquals(0.4875, p.getValue(49.5), 0.001);
		assertEquals(0.325, p.getValue(50), 0.001);
		assertEquals(0.325, p.getValue(54), 0.001);
		assertEquals(0.1625, p.getValue(54.5), 0.001);
		assertEquals(0, p.getValue(55), 0.001);
		assertEquals(0, p.getValue(100), 0.001);
	}

}
