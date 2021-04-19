package io.openems.edge.battery.bmw;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.bmw.BmwBattery.BMWChannelId;
import io.openems.edge.battery.bmw.enums.BatteryState;
import io.openems.edge.battery.bmw.enums.BmsState;
import io.openems.edge.battery.bmw.enums.State;
import io.openems.edge.battery.protection.BatteryProtection;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.common.test.TimeLeapClock;

public class BmwBatteryImplTest {

	private static final String BATTERY_ID = "battery0";
	private static final String MODBUS_ID = "modbus0";

	private static final ChannelAddress STATE_MACHINE = new ChannelAddress(BATTERY_ID, BMWChannelId.STATE_MACHINE.id());
	private static final ChannelAddress BMS_STATE = new ChannelAddress(BATTERY_ID, BMWChannelId.BMS_STATE.id());

	private static final ChannelAddress BP_CHARGE_BMS = new ChannelAddress(BATTERY_ID,
			BatteryProtection.ChannelId.BP_CHARGE_BMS.id());
	private static final ChannelAddress BP_DISCHARGE_BMS = new ChannelAddress(BATTERY_ID,
			BatteryProtection.ChannelId.BP_DISCHARGE_BMS.id());
	private static final ChannelAddress MIN_CELL_VOLTAGE = new ChannelAddress(BATTERY_ID,
			Battery.ChannelId.MIN_CELL_VOLTAGE.id());
	private static final ChannelAddress MAX_CELL_VOLTAGE = new ChannelAddress(BATTERY_ID,
			Battery.ChannelId.MAX_CELL_VOLTAGE.id());
	private static final ChannelAddress CHARGE_MAX_CURRENT = new ChannelAddress(BATTERY_ID,
			Battery.ChannelId.CHARGE_MAX_CURRENT.id());

	@Test
	public void test() throws Exception {
		final TimeLeapClock clock = new TimeLeapClock(Instant.parse("2020-01-01T01:00:00.00Z"), ZoneOffset.UTC);
		new ComponentTest(new BmwBatteryImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("manager", new DummyComponentManager(clock)) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.activate(MyConfig.create() //
						.setId(BATTERY_ID) //
						.setModbusId(MODBUS_ID) //
						.setBatteryState(BatteryState.DEFAULT) //
						.setErrorDelay(0) //
						.setMaxStartAttempts(0) //
						.setMaxStartTime(0) //
						.setPendingTolerance(0) //
						.setStartUnsuccessfulDelay(0) //
						.build()) //
				.next(new TestCase("Start Battery") //
						.input(BMS_STATE, BmsState.OPERATION) //
						.output(STATE_MACHINE, State.RUNNING)) //

				.next(new TestCase("Initialize BMS data") //
						.input(BP_CHARGE_BMS, 135) //
						.input(BP_DISCHARGE_BMS, 135) //
						.input(MAX_CELL_VOLTAGE, 3900) //
						.input(MIN_CELL_VOLTAGE, 3900)) //
				.next(new TestCase()) //

				.next(new TestCase("Start Increase-Ramp") //
						.timeleap(clock, 60, ChronoUnit.SECONDS) //
						.output(CHARGE_MAX_CURRENT, 6)) //
				.next(new TestCase() //
						.timeleap(clock, 60, ChronoUnit.SECONDS) //
						.output(CHARGE_MAX_CURRENT, 12)) //
				.next(new TestCase() //
						.timeleap(clock, 60, ChronoUnit.SECONDS) //
						.output(CHARGE_MAX_CURRENT, 18)) //
				.next(new TestCase() //
						.timeleap(clock, 60, ChronoUnit.MINUTES) //
						.output(CHARGE_MAX_CURRENT, 135)) //

				.next(new TestCase("Charge Limit") //
						.input(MAX_CELL_VOLTAGE, 4050)) //
				.next(new TestCase() //
						.output(CHARGE_MAX_CURRENT, 102)) //
				.next(new TestCase() //
						.input(MAX_CELL_VOLTAGE, 4167)) //
				.next(new TestCase() //
						.output(CHARGE_MAX_CURRENT, 5)) //
				.next(new TestCase() //
						.input(MAX_CELL_VOLTAGE, 4181)) //
				.next(new TestCase() //
						.output(CHARGE_MAX_CURRENT, 1)) //
				.next(new TestCase() //
						.input(MAX_CELL_VOLTAGE, 4182)) //
				.next(new TestCase() //
						.output(CHARGE_MAX_CURRENT, 0)) //

				.next(new TestCase("Wait for reset") //
						.input(MAX_CELL_VOLTAGE, 3906)) //
				.next(new TestCase() //
						.output(CHARGE_MAX_CURRENT, 0)) //
				.next(new TestCase() //
						.input(MAX_CELL_VOLTAGE, 3905)) //
				.next(new TestCase() //
						.timeleap(clock, 10, ChronoUnit.SECONDS) //
						.output(CHARGE_MAX_CURRENT, 1)) //
				.next(new TestCase() //
						.timeleap(clock, 60, ChronoUnit.MINUTES) //
						.output(CHARGE_MAX_CURRENT, 135)) //
		;
	}
}
