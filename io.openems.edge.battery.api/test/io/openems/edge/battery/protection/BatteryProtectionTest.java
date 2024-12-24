package io.openems.edge.battery.protection;

import static io.openems.edge.battery.api.Battery.ChannelId.CHARGE_MAX_CURRENT;
import static io.openems.edge.battery.api.Battery.ChannelId.DISCHARGE_MAX_CURRENT;
import static io.openems.edge.battery.api.Battery.ChannelId.MAX_CELL_TEMPERATURE;
import static io.openems.edge.battery.api.Battery.ChannelId.MAX_CELL_VOLTAGE;
import static io.openems.edge.battery.api.Battery.ChannelId.MIN_CELL_TEMPERATURE;
import static io.openems.edge.battery.api.Battery.ChannelId.MIN_CELL_VOLTAGE;
import static io.openems.edge.battery.protection.BatteryProtection.ChannelId.BP_CHARGE_BMS;
import static io.openems.edge.battery.protection.BatteryProtection.ChannelId.BP_DISCHARGE_BMS;
import static io.openems.edge.common.startstop.StartStoppable.ChannelId.START_STOP;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.SECONDS;

import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.Test;

import io.openems.common.channel.Unit;
import io.openems.common.test.TimeLeapClock;
import io.openems.common.types.OpenemsType;
import io.openems.edge.battery.protection.currenthandler.ChargeMaxCurrentHandler;
import io.openems.edge.battery.protection.currenthandler.DischargeMaxCurrentHandler;
import io.openems.edge.battery.protection.force.ForceCharge;
import io.openems.edge.battery.protection.force.ForceDischarge;
import io.openems.edge.battery.test.DummyBattery;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.linecharacteristic.PolyLine;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;

public class BatteryProtectionTest {

	public static final double MAX_INCREASE_AMPERE_PER_SECOND = 0.5;

	public static final PolyLine CHARGE_VOLTAGE_TO_PERCENT = PolyLine.create() //
			.addPoint(3000, 0.1) //
			.addPoint(Math.nextUp(3000), 1) //
			.addPoint(3350, 1) //
			.addPoint(3450, 0.9999) //
			.addPoint(3600, 0.02) //
			.addPoint(Math.nextDown(3650), 0.02) //
			.addPoint(3650, 0) //
			.build();

	public static final PolyLine CHARGE_TEMPERATURE_TO_PERCENT = PolyLine.create() //
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

	public static final ForceDischarge.Params FORCE_DISCHARGE = new ForceDischarge.Params(3660, 3640, 3450);

	public static final PolyLine DISCHARGE_VOLTAGE_TO_PERCENT = PolyLine.create() //
			.addPoint(2900, 0) //
			.addPoint(Math.nextUp(2900), 0.05) //
			.addPoint(2920, 0.05) //
			.addPoint(3000, 1) //
			.addPoint(3700, 1) //
			.addPoint(Math.nextUp(3700), 0) //
			.build();

	public static final PolyLine DISCHARGE_TEMPERATURE_TO_PERCENT = PolyLine.create() //
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

	public static final ForceCharge.Params FORCE_CHARGE = new ForceCharge.Params(2850, 2910, 3000);

	public static final int INITIAL_BMS_MAX_EVER_CURRENT = 80;

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		ORIGINAL_CHARGE_MAX_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE)), //
		ORIGINAL_DISCHARGE_MAX_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE)); //

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

	@Test
	public void test() throws Exception {
		final var battery = new DummyBattery(BATTERY_ID);
		final var clock = new TimeLeapClock(Instant.parse("2020-01-01T01:00:00.00Z"), ZoneOffset.UTC);
		final var cm = new DummyComponentManager(clock);
		final var sut = BatteryProtection.create(battery) //
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
		new ComponentTest(battery) //
				.next(new TestCase() //
						.input(START_STOP, StartStop.START) //
						.input(BP_CHARGE_BMS, 80) //
						.input(BP_DISCHARGE_BMS, 80) //
						.input(MIN_CELL_VOLTAGE, 2950) //
						.input(MAX_CELL_VOLTAGE, 3300) //
						.input(MIN_CELL_TEMPERATURE, 16) //
						.input(MAX_CELL_TEMPERATURE, 17) //
						.onAfterProcessImage(() -> sut.apply()) //
						.output(CHARGE_MAX_CURRENT, 0) //
						.output(DISCHARGE_MAX_CURRENT, 0)) //
				.next(new TestCase("open, but maxIncreaseAmpereLimit") //
						.timeleap(clock, 2, SECONDS) //
						.input(MIN_CELL_VOLTAGE, 3000) //
						.onAfterProcessImage(() -> sut.apply()) //
						.output(CHARGE_MAX_CURRENT, 1) //
						.output(DISCHARGE_MAX_CURRENT, 1)) //
				.next(new TestCase() //
						.timeleap(clock, 2, SECONDS) //
						.input(MIN_CELL_VOLTAGE, 3050) //
						.onAfterProcessImage(() -> sut.apply()) //
						.output(CHARGE_MAX_CURRENT, 2) //
						.output(DISCHARGE_MAX_CURRENT, 2)) //
				.next(new TestCase() //
						.timeleap(clock, 10, SECONDS) //
						.onAfterProcessImage(() -> sut.apply()) //
						.output(CHARGE_MAX_CURRENT, 7) //
						.output(DISCHARGE_MAX_CURRENT, 7)) //
				.next(new TestCase() //
						.timeleap(clock, 10, MINUTES) //
						.input(MAX_CELL_VOLTAGE, 3300) //
						.onAfterProcessImage(() -> sut.apply()) //
						.output(CHARGE_MAX_CURRENT, 80) //
						.output(DISCHARGE_MAX_CURRENT, 80)) //
				.next(new TestCase() //
						.timeleap(clock, 10, MINUTES) //
						.input(MAX_CELL_VOLTAGE, 3499) //
						.onAfterProcessImage(() -> sut.apply()) //
						.output(CHARGE_MAX_CURRENT, 54) //
						.output(DISCHARGE_MAX_CURRENT, 80)) //
				.next(new TestCase() //
						.timeleap(clock, 10, MINUTES) //
						.input(MAX_CELL_VOLTAGE, 3649) //
						.onAfterProcessImage(() -> sut.apply()) //
						.output(CHARGE_MAX_CURRENT, 2) //
						.output(DISCHARGE_MAX_CURRENT, 80)) //
				.next(new TestCase() //
						.timeleap(clock, 10, MINUTES) //
						.input(MAX_CELL_VOLTAGE, 3649) //
						.onAfterProcessImage(() -> sut.apply()) //
						.output(CHARGE_MAX_CURRENT, 2) //
						.output(DISCHARGE_MAX_CURRENT, 80)) //
				.next(new TestCase() //
						.timeleap(clock, 10, MINUTES) //
						.input(MAX_CELL_VOLTAGE, 3650) //
						.onAfterProcessImage(() -> sut.apply()) //
						.output(CHARGE_MAX_CURRENT, 0) //
						.output(DISCHARGE_MAX_CURRENT, 80)) //
				.next(new TestCase("Start Force-Discharge: wait 60 seconds") //
						.timeleap(clock, 1, SECONDS) //
						.input(MAX_CELL_VOLTAGE, 3660) //
						.onAfterProcessImage(() -> sut.apply()) //
						.output(CHARGE_MAX_CURRENT, 0) //
						.output(DISCHARGE_MAX_CURRENT, 80)) //
				.next(new TestCase("Start Force-Discharge") //
						.timeleap(clock, 60, SECONDS) //
						.input(MAX_CELL_VOLTAGE, 3660) //
						.onAfterProcessImage(() -> sut.apply()) //
						.output(CHARGE_MAX_CURRENT, -2) //
						.output(DISCHARGE_MAX_CURRENT, 80)) //
				.next(new TestCase("Force-Discharge") //
						.timeleap(clock, 1, SECONDS) //
						.input(MAX_CELL_VOLTAGE, 3640) //
						.onAfterProcessImage(() -> sut.apply()) //
						.output(CHARGE_MAX_CURRENT, -2) //
						.output(DISCHARGE_MAX_CURRENT, 80)) //
				.next(new TestCase("Block Charge #1") //
						.timeleap(clock, 1, SECONDS) //
						.input(MAX_CELL_VOLTAGE, 3639) //
						.onAfterProcessImage(() -> sut.apply()) //
						.output(CHARGE_MAX_CURRENT, -1) //
						.output(DISCHARGE_MAX_CURRENT, 80)) //
				.next(new TestCase("Block Charge #1 still reduce by 1") //
						.timeleap(clock, 1, SECONDS) //
						.input(MAX_CELL_VOLTAGE, 3638) //
						.onAfterProcessImage(() -> sut.apply()) //
						.output(CHARGE_MAX_CURRENT, -1) //
						.output(DISCHARGE_MAX_CURRENT, 80)) //
				.next(new TestCase("Block Charge #1 still reduce by 1") //
						.timeleap(clock, 1, SECONDS) //
						.input(MAX_CELL_VOLTAGE, 3610) //
						.onAfterProcessImage(() -> sut.apply()) //
						.output(CHARGE_MAX_CURRENT, 0) //
						.output(DISCHARGE_MAX_CURRENT, 80)) //
				.next(new TestCase("Block Charge #2") //
						.timeleap(clock, 1, SECONDS) //
						.input(MAX_CELL_VOLTAGE, 3600) //
						.onAfterProcessImage(() -> sut.apply()) //
						.output(CHARGE_MAX_CURRENT, 0) //
						.output(DISCHARGE_MAX_CURRENT, 80)) //

				.next(new TestCase("Start Force-Discharge again") //
						.timeleap(clock, 1, SECONDS) //
						.input(MAX_CELL_VOLTAGE, 3660) //
						.onAfterProcessImage(() -> sut.apply()) //
						.output(CHARGE_MAX_CURRENT, -2) //
						.output(DISCHARGE_MAX_CURRENT, 80)) //
				.next(new TestCase("Force-Discharge") //
						.timeleap(clock, 1, SECONDS) //
						.input(MAX_CELL_VOLTAGE, 3640) //
						.onAfterProcessImage(() -> sut.apply()) //
						.output(CHARGE_MAX_CURRENT, -2) //
						.output(DISCHARGE_MAX_CURRENT, 80)) //
				.next(new TestCase("Block Charge #1") //
						.timeleap(clock, 1, SECONDS) //
						.input(MAX_CELL_VOLTAGE, 3639) //
						.onAfterProcessImage(() -> sut.apply()) //
						.output(CHARGE_MAX_CURRENT, -1) //
						.output(DISCHARGE_MAX_CURRENT, 80)) //
				.next(new TestCase("Block Charge #1 still reduce by 1") //
						.timeleap(clock, 1, SECONDS) //
						.input(MAX_CELL_VOLTAGE, 3638) //
						.onAfterProcessImage(() -> sut.apply()) //
						.output(CHARGE_MAX_CURRENT, -1) //
						.output(DISCHARGE_MAX_CURRENT, 80)) //
				.next(new TestCase("Block Charge #1 still reduce by 1") //
						.timeleap(clock, 1, SECONDS) //
						.input(MAX_CELL_VOLTAGE, 3637) //
						.onAfterProcessImage(() -> sut.apply()) //
						.output(CHARGE_MAX_CURRENT, 0) //
						.output(DISCHARGE_MAX_CURRENT, 80)) //
				.next(new TestCase("Block Charge #2") //
						.timeleap(clock, 1, SECONDS) //
						.input(MAX_CELL_VOLTAGE, 3600) //
						.onAfterProcessImage(() -> sut.apply()) //
						.output(CHARGE_MAX_CURRENT, 0) //
						.output(DISCHARGE_MAX_CURRENT, 80)) //
				.next(new TestCase("Block Charge #3") //
						.timeleap(clock, 1, SECONDS) //
						.input(MAX_CELL_VOLTAGE, 3450) //
						.onAfterProcessImage(() -> sut.apply()) //
						.output(CHARGE_MAX_CURRENT, 0) //
						.output(DISCHARGE_MAX_CURRENT, 80)) //
				.next(new TestCase("Finish Force-Discharge") //
						.timeleap(clock, 1, SECONDS) //
						.input(MAX_CELL_VOLTAGE, 3449) //
						.onAfterProcessImage(() -> sut.apply()) //
						.output(CHARGE_MAX_CURRENT, 0) //
						.output(DISCHARGE_MAX_CURRENT, 80)) //
				.next(new TestCase() //
						.timeleap(clock, 1, SECONDS) //
						.input(MAX_CELL_VOLTAGE, 3400) //
						.onAfterProcessImage(() -> sut.apply()) //
						.output(CHARGE_MAX_CURRENT, 0) //
						.output(DISCHARGE_MAX_CURRENT, 80)) //
				.next(new TestCase("Allow Charge") //
						.timeleap(clock, 1, SECONDS) //
						.input(MAX_CELL_VOLTAGE, 3350) //
						.onAfterProcessImage(() -> sut.apply()) //
						.output(CHARGE_MAX_CURRENT, 1) //
						.output(DISCHARGE_MAX_CURRENT, 80)) //
		;
	}

}
