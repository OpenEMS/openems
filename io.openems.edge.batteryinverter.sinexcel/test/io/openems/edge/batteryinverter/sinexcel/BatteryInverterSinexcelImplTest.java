package io.openems.edge.batteryinverter.sinexcel;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.test.DummyBattery;
import io.openems.edge.batteryinverter.api.OffGridBatteryInverter;
import io.openems.edge.batteryinverter.api.OffGridBatteryInverter.TargetGridMode;
import io.openems.edge.batteryinverter.sinexcel.enums.CountryCode;
import io.openems.edge.batteryinverter.sinexcel.enums.EnableDisable;
import io.openems.edge.batteryinverter.sinexcel.statemachine.StateMachine.State;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.startstop.StartStopConfig;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.common.test.TimeLeapClock;

public class BatteryInverterSinexcelImplTest {

	private static final String BATTERY_INVERTER_ID = "batteryInverter0";
	private static final String BATTERY_ID = "battery0";
	private static final String MODBUS_ID = "modbus0";
	private static final ChannelAddress STATE_MACHINE = new ChannelAddress(BATTERY_INVERTER_ID, "StateMachine");
	private static final ChannelAddress SET_ON_GRID_MODE = new ChannelAddress(BATTERY_INVERTER_ID, "SetOnGridMode");
	private static final ChannelAddress SET_OFF_GRID_MODE = new ChannelAddress(BATTERY_INVERTER_ID, "SetOffGridMode");
	private static final ChannelAddress MAX_APPARENT_POWER = new ChannelAddress(BATTERY_INVERTER_ID, //
			"MaxApparentPower");

	private static final ChannelAddress INVERTER_STATE = new ChannelAddress(BATTERY_INVERTER_ID, //
			OffGridBatteryInverter.ChannelId.INVERTER_STATE.id());

	private static class MyComponentTest extends ComponentTest {

		private final Battery battery = new DummyBattery(BATTERY_ID);

		public MyComponentTest(OpenemsComponent sut) throws OpenemsException {
			super(sut);
		}

		@Override
		protected void handleEvent(String topic) throws Exception {
			if (topic.equals(EdgeEventConstants.TOPIC_CYCLE_BEFORE_WRITE)) {
				((BatteryInverterSinexcelImpl) this.getSut()).run(this.battery, 0, 0);
			}
			super.handleEvent(topic);
		}

	}

	@Test
	public void testStart() throws Exception {
		final var clock = new TimeLeapClock(Instant.ofEpochSecond(1577836800L) /* starts at 1. January 2020 00:00:00 */,
				ZoneOffset.UTC);
		new MyComponentTest(new BatteryInverterSinexcelImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.activate(MyConfig.create() //
						.setId(BATTERY_INVERTER_ID) //
						.setStartStopConfig(StartStopConfig.START) //
						.setModbusId(MODBUS_ID) //
						.setCountryCode(CountryCode.GERMANY)//
						.setEmergencyPower(EnableDisable.DISABLE)//
						.build()) //
				.next(new TestCase("first") //
						.input(MAX_APPARENT_POWER, 50_000) //
						.output(STATE_MACHINE, State.UNDEFINED)) //
				.next(new TestCase("second") //
						.timeleap(clock, 4, ChronoUnit.SECONDS) //
						.output(STATE_MACHINE, State.GO_RUNNING)) //
				.next(new TestCase("third") //
						.input(INVERTER_STATE, true)) //
				.next(new TestCase("Fourth") //
						.output(STATE_MACHINE, State.RUNNING))
				.next(new TestCase("Fifth") //
						.output(STATE_MACHINE, State.RUNNING))
				.next(new TestCase("sixth") //
						.input(INVERTER_STATE, false)) //
				.next(new TestCase("Fifth") //
						.output(STATE_MACHINE, State.UNDEFINED))
				.next(new TestCase("Fifth") //
						.output(STATE_MACHINE, State.GO_RUNNING));
	}

	@Test
	public void testOffGrid() throws Exception {
		final var clock = new TimeLeapClock(Instant.ofEpochSecond(1577836800L) /* starts at 1. January 2020 00:00:00 */,
				ZoneOffset.UTC);
		var sut = new BatteryInverterSinexcelImpl();
		new MyComponentTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.activate(MyConfig.create() //
						.setId(BATTERY_INVERTER_ID) //
						.setStartStopConfig(StartStopConfig.START) //
						.setModbusId(MODBUS_ID) //
						.setCountryCode(CountryCode.GERMANY)//
						.setEmergencyPower(EnableDisable.DISABLE)//
						.build()) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.UNDEFINED)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.GO_RUNNING)) //
				.next(new TestCase()//
						.input(SET_OFF_GRID_MODE, false) //
						.input(SET_ON_GRID_MODE, true)) //
				.next(new TestCase()//
						.input(INVERTER_STATE, true))
				.next(new TestCase() //
						.output(STATE_MACHINE, State.RUNNING)) //
				.next(new TestCase() //
						.onExecuteControllersCallbacks(() -> sut.setTargetGridMode(TargetGridMode.GO_OFF_GRID))) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.UNDEFINED)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.GO_RUNNING)) //
				.next(new TestCase()//
						.input(SET_OFF_GRID_MODE, true) //
						.input(SET_ON_GRID_MODE, false)) //
				.next(new TestCase()//
						.input(INVERTER_STATE, true))
				.next(new TestCase() //
						.output(STATE_MACHINE, State.RUNNING)) //

		;
	}

}
