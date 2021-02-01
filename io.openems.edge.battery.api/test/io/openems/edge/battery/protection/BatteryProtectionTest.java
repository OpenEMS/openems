package io.openems.edge.battery.protection;

import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.battery.protection.ChargeMaxCurrentHandler.ForceDischargeParams;
import io.openems.edge.battery.protection.DischargeMaxCurrentHandler.ForceChargeParams;
import io.openems.edge.battery.test.DummyBattery;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.linecharacteristic.PolyLine;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.TimeLeapClock;

public class BatteryProtectionTest {

	private final static double MAX_INCREASE_AMPERE_PER_SECOND = 0.5;

	private final static PolyLine CHARGE_VOLTAGE_TO_PERCENT = PolyLine.create() //
			.addPoint(Math.nextDown(3000), 0.1) //
			.addPoint(3000, 1) //
			.addPoint(3450, 1) //
			.addPoint(Math.nextDown(3650), 0.01) //
			.addPoint(3650, 0) //
			.build();

	private final static PolyLine CHARGE_TEMPERATURE_TO_PERCENT = PolyLine.create() //
			.addPoint(Math.nextDown(-10), 0) //
			.addPoint(-10, 0.215) //
			.addPoint(0, 0.215) //
			.addPoint(1, 0.325) //
			.addPoint(5, 0.325) //
			.addPoint(6, 0.65) //
			.addPoint(15, 0.65) //
			.addPoint(16, 1) //
			.addPoint(44, 1) //
			.addPoint(45, 0.65) //
			.addPoint(49, 0.65) //
			.addPoint(50, 0.325) //
			.addPoint(54, 0.325) //
			.addPoint(55, 0) //
			.build();

	private final static ForceDischargeParams FORCE_DISCHARGE = new ChargeMaxCurrentHandler.ForceDischargeParams(3660,
			3640, 3450);

	private final static PolyLine DISCHARGE_VOLTAGE_TO_PERCENT = PolyLine.create() //
			.addPoint(Math.nextDown(3000), 0) //
			.addPoint(3000, 1) //
			.addPoint(3700, 1) //
			.addPoint(Math.nextUp(3700), 0) //
			.build();

	private final static PolyLine DISCHARGE_TEMPERATURE_TO_PERCENT = PolyLine.create() //
			.addPoint(Math.nextDown(-10), 0) //
			.addPoint(-10, 0.215) //
			.addPoint(0, 0.215) //
			.addPoint(1, 1) //
			.addPoint(44, 1) //
			.addPoint(45, 0.865) //
			.addPoint(49, 0.865) //
			.addPoint(50, 0.325) //
			.addPoint(54, 0.325) //
			.addPoint(55, 0) //
			.build();

	private final static ForceChargeParams FORCE_CHARGE = new DischargeMaxCurrentHandler.ForceChargeParams(2850, 2910,
			3000);

	private final static int INITIAL_BMS_MAX_EVER_CURRENT = 80;

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		ORIGINAL_CHARGE_MAX_CURRENT(Doc.of(OpenemsType.INTEGER)), //
		ORIGINAL_DISCHARGE_MAX_CURRENT(Doc.of(OpenemsType.INTEGER));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	private static final String BATTERY_ID = "battery0";

	private final static ChannelAddress BATTERY_ORIGINAL_CHARGE_MAX_CURRENT = new ChannelAddress(BATTERY_ID,
			ChannelId.ORIGINAL_CHARGE_MAX_CURRENT.id());
	private final static ChannelAddress BATTERY_ORIGINAL_DISCHARGE_MAX_CURRENT = new ChannelAddress(BATTERY_ID,
			ChannelId.ORIGINAL_DISCHARGE_MAX_CURRENT.id());
	private final static ChannelAddress BATTERY_MIN_CELL_VOLTAGE = new ChannelAddress(BATTERY_ID, "MinCellVoltage");
	private final static ChannelAddress BATTERY_MAX_CELL_VOLTAGE = new ChannelAddress(BATTERY_ID, "MaxCellVoltage");
	private final static ChannelAddress BATTERY_MIN_CELL_TEMPERATURE = new ChannelAddress(BATTERY_ID,
			"MinCellTemperature");
	private final static ChannelAddress BATTERY_MAX_CELL_TEMPERATURE = new ChannelAddress(BATTERY_ID,
			"MaxCellTemperature");
	private final static ChannelAddress BATTERY_CHARGE_MAX_CURRENT = new ChannelAddress(BATTERY_ID, "ChargeMaxCurrent");
	private final static ChannelAddress BATTERY_DISCHARGE_MAX_CURRENT = new ChannelAddress(BATTERY_ID,
			"DischargeMaxCurrent");

	@Test
	public void test() throws Exception {
		System.out.println("Test");
		final DummyBattery battery = new DummyBattery(BATTERY_ID, ChannelId.values());
		final TimeLeapClock clock = new TimeLeapClock(Instant.parse("2020-01-01T01:00:00.00Z"), ZoneOffset.UTC);
		final DummyComponentManager cm = new DummyComponentManager(clock);
		final BatteryProtection sut = BatteryProtection.create(battery) //
				.setBmsAllowedChargeCurrent(ChannelId.ORIGINAL_CHARGE_MAX_CURRENT) //
				.setBmsAllowedDischargeCurrent(ChannelId.ORIGINAL_DISCHARGE_MAX_CURRENT) //
				.setChargeMaxCurrentHandler(ChargeMaxCurrentHandler.create(cm, INITIAL_BMS_MAX_EVER_CURRENT) //
						.setVoltageToPercent(CHARGE_VOLTAGE_TO_PERCENT) //
						.setTemperatureToPercent(CHARGE_TEMPERATURE_TO_PERCENT) //
						.setMaxIncreasePerSecond(MAX_INCREASE_AMPERE_PER_SECOND) //
						.setForceDischarge(FORCE_DISCHARGE) //
						.build()) //
				.setDischargeMaxCurrentHandler(DischargeMaxCurrentHandler.create(cm, INITIAL_BMS_MAX_EVER_CURRENT) //
						.setVoltageToPercent(DISCHARGE_VOLTAGE_TO_PERCENT)
						.setTemperatureToPercent(DISCHARGE_TEMPERATURE_TO_PERCENT) //
						.setMaxIncreasePerSecond(MAX_INCREASE_AMPERE_PER_SECOND) //
						.setForceCharge(FORCE_CHARGE) //
						.build()) //
				.build();
		new ComponentTest(new DummyBattery(BATTERY_ID)) //
				.addComponent(battery) //
				.next(new TestCase() //
						.input(BATTERY_ORIGINAL_CHARGE_MAX_CURRENT, 80) //
						.input(BATTERY_ORIGINAL_DISCHARGE_MAX_CURRENT, 80) //
						.input(BATTERY_MIN_CELL_VOLTAGE, 2950) //
						.input(BATTERY_MAX_CELL_VOLTAGE, 3300) //
						.input(BATTERY_MIN_CELL_TEMPERATURE, 16) //
						.input(BATTERY_MAX_CELL_TEMPERATURE, 17) //
						.onAfterProcessImage(() -> sut.apply()) //
						.output(BATTERY_CHARGE_MAX_CURRENT, 8) //
						.output(BATTERY_DISCHARGE_MAX_CURRENT, 0)) //
				.next(new TestCase() //
						.timeleap(clock, 900, ChronoUnit.MILLIS) //
						.input(BATTERY_MIN_CELL_VOLTAGE, 3000) //
						.onAfterProcessImage(() -> sut.apply()) //
						.output(BATTERY_CHARGE_MAX_CURRENT, 8) //
						.output(BATTERY_DISCHARGE_MAX_CURRENT, 0)) //
				.next(new TestCase() //
						.timeleap(clock, 900, ChronoUnit.MILLIS) //
						.input(BATTERY_MIN_CELL_VOLTAGE, 3050) //
						.onAfterProcessImage(() -> sut.apply()) //
						.output(BATTERY_CHARGE_MAX_CURRENT, 9) //
						.output(BATTERY_DISCHARGE_MAX_CURRENT, 1)) //
				.next(new TestCase() //
						.timeleap(clock, 10, ChronoUnit.SECONDS) //
						.onAfterProcessImage(() -> sut.apply()) //
						.output(BATTERY_CHARGE_MAX_CURRENT, 14) //
						.output(BATTERY_DISCHARGE_MAX_CURRENT, 6)) //
				.next(new TestCase() //
						.timeleap(clock, 10, ChronoUnit.MINUTES) //
						.input(BATTERY_MAX_CELL_VOLTAGE, 3300) //
						.onAfterProcessImage(() -> sut.apply()) //
						.output(BATTERY_CHARGE_MAX_CURRENT, 80) //
						.output(BATTERY_DISCHARGE_MAX_CURRENT, 80)) //
				.next(new TestCase() //
						.timeleap(clock, 10, ChronoUnit.MINUTES) //
						.input(BATTERY_MAX_CELL_VOLTAGE, 3499) //
						.onAfterProcessImage(() -> sut.apply()) //
						.output(BATTERY_CHARGE_MAX_CURRENT, 61) //
						.output(BATTERY_DISCHARGE_MAX_CURRENT, 80)) //
				.next(new TestCase() //
						.timeleap(clock, 10, ChronoUnit.MINUTES) //
						.input(BATTERY_MAX_CELL_VOLTAGE, 3649) //
						.onAfterProcessImage(() -> sut.apply()) //
						.output(BATTERY_CHARGE_MAX_CURRENT, 1) //
						.output(BATTERY_DISCHARGE_MAX_CURRENT, 80)) //
				.next(new TestCase() //
						.timeleap(clock, 10, ChronoUnit.MINUTES) //
						.input(BATTERY_MAX_CELL_VOLTAGE, 3649) //
						.onAfterProcessImage(() -> sut.apply()) //
						.output(BATTERY_CHARGE_MAX_CURRENT, 1) //
						.output(BATTERY_DISCHARGE_MAX_CURRENT, 80)) //
				.next(new TestCase() //
						.timeleap(clock, 10, ChronoUnit.MINUTES) //
						.input(BATTERY_MAX_CELL_VOLTAGE, 3650) //
						.onAfterProcessImage(() -> sut.apply()) //
						.output(BATTERY_CHARGE_MAX_CURRENT, 0) //
						.output(BATTERY_DISCHARGE_MAX_CURRENT, 80)) //
				.next(new TestCase("Start Force-Discharge") //
						.timeleap(clock, 1, ChronoUnit.SECONDS) //
						.input(BATTERY_MAX_CELL_VOLTAGE, 3660) //
						.onAfterProcessImage(() -> sut.apply()) //
						.output(BATTERY_CHARGE_MAX_CURRENT, -1) //
						.output(BATTERY_DISCHARGE_MAX_CURRENT, 80)) //
				.next(new TestCase("Force-Discharge") //
						.timeleap(clock, 1, ChronoUnit.SECONDS) //
						.input(BATTERY_MAX_CELL_VOLTAGE, 3640) //
						.onAfterProcessImage(() -> sut.apply()) //
						.output(BATTERY_CHARGE_MAX_CURRENT, -1) //
						.output(BATTERY_DISCHARGE_MAX_CURRENT, 80)) //
				.next(new TestCase("Block Charge #1") //
						.timeleap(clock, 1, ChronoUnit.SECONDS) //
						.input(BATTERY_MAX_CELL_VOLTAGE, 3639) //
						.onAfterProcessImage(() -> sut.apply()) //
						.output(BATTERY_CHARGE_MAX_CURRENT, 0) //
						.output(BATTERY_DISCHARGE_MAX_CURRENT, 80)) //
				.next(new TestCase("Block Charge #2") //
						.timeleap(clock, 1, ChronoUnit.SECONDS) //
						.input(BATTERY_MAX_CELL_VOLTAGE, 3600) //
						.onAfterProcessImage(() -> sut.apply()) //
						.output(BATTERY_CHARGE_MAX_CURRENT, 0) //
						.output(BATTERY_DISCHARGE_MAX_CURRENT, 80)) //
				.next(new TestCase("Block Charge #3") //
						.timeleap(clock, 1, ChronoUnit.SECONDS) //
						.input(BATTERY_MAX_CELL_VOLTAGE, 3450) //
						.onAfterProcessImage(() -> sut.apply()) //
						.output(BATTERY_CHARGE_MAX_CURRENT, 0) //
						.output(BATTERY_DISCHARGE_MAX_CURRENT, 80)) //
				.next(new TestCase("Finish Force-Discharge") //
						.timeleap(clock, 1, ChronoUnit.SECONDS) //
						.input(BATTERY_MAX_CELL_VOLTAGE, 3449) //
						.onAfterProcessImage(() -> sut.apply()) //
						.output(BATTERY_CHARGE_MAX_CURRENT, 1) //
						.output(BATTERY_DISCHARGE_MAX_CURRENT, 80)) //
				.next(new TestCase() //
						.timeleap(clock, 3, ChronoUnit.SECONDS) //
						.input(BATTERY_MAX_CELL_VOLTAGE, 3449) //
						.onAfterProcessImage(() -> sut.apply()) //
						.output(BATTERY_CHARGE_MAX_CURRENT, 2) //
						.output(BATTERY_DISCHARGE_MAX_CURRENT, 80)) //
		;
	}

	@Test
	public void testChargeVoltageToPercent() {
		PolyLine p = CHARGE_VOLTAGE_TO_PERCENT;
		assertEquals(0.1, p.getValue(2500), 0.001);
		assertEquals(0.1, p.getValue(Math.nextDown(3000)), 0.001);
		assertEquals(1, p.getValue(3000), 0.001);
		assertEquals(1, p.getValue(3450), 0.001);
		assertEquals(0.752, p.getValue(3500), 0.001);
		assertEquals(0.257, p.getValue(3600), 0.001);
		assertEquals(0.01, p.getValue(Math.nextDown(3650)), 0.001);
		assertEquals(0, p.getValue(3650), 0.001);
		assertEquals(0, p.getValue(4000), 0.001);
	}

	@Test
	public void testChargeTemperatureToPercent() {
		PolyLine p = CHARGE_TEMPERATURE_TO_PERCENT;
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

	@Test
	public void testForceDischarge() {
		final TimeLeapClock clock = new TimeLeapClock(Instant.parse("2020-01-01T01:00:00.00Z"), ZoneOffset.UTC);
		final DummyComponentManager cm = new DummyComponentManager(clock);
		ChargeMaxCurrentHandler sut = ChargeMaxCurrentHandler.create(cm, 40) //
				.setForceDischarge(3660, 3640, 3450) //
				.build();
		assertEquals(null, sut.getForceCurrent(3000, 3650));
		assertEquals(-1., sut.getForceCurrent(3000, 3660), 0.001);
		assertEquals(-1., sut.getForceCurrent(3000, 3650), 0.001);
		assertEquals(0, sut.getForceCurrent(3000, 3639), 0.001);
		assertEquals(0, sut.getForceCurrent(3000, 3600), 0.001);
		assertEquals(0, sut.getForceCurrent(3000, 3500), 0.001);
		assertEquals(null, sut.getForceCurrent(3000, 3449));
	}

	@Test
	public void testMaxIncreasePerSecond() {
		final TimeLeapClock clock = new TimeLeapClock(Instant.parse("2020-01-01T01:00:00.00Z"), ZoneOffset.UTC);
		final DummyComponentManager cm = new DummyComponentManager(clock);
		ChargeMaxCurrentHandler sut = ChargeMaxCurrentHandler.create(cm, 40) //
				.setMaxIncreasePerSecond(MAX_INCREASE_AMPERE_PER_SECOND) //
				.build();
		sut.lastMaxIncreaseAmpereLimit = 0.;
		sut.lastResultTimestamp = Instant.now(clock);

		clock.leap(1, ChronoUnit.SECONDS);
		assertEquals(0.5, (double) sut.getMaxIncreaseAmpereLimit(), 0.001);
		sut.lastMaxIncreaseAmpereLimit = 0.5;

		clock.leap(1, ChronoUnit.SECONDS);
		assertEquals(1, (double) sut.getMaxIncreaseAmpereLimit(), 0.001);
		sut.lastMaxIncreaseAmpereLimit = 1.;

		clock.leap(800, ChronoUnit.MILLIS);
		assertEquals(1.4, (double) sut.getMaxIncreaseAmpereLimit(), 0.001);
	}
}
