package io.openems.edge.batteryinverter.sinexcel;

import static io.openems.edge.batteryinverter.api.OffGridBatteryInverter.ChannelId.INVERTER_STATE;
import static io.openems.edge.batteryinverter.api.SymmetricBatteryInverter.ChannelId.MAX_APPARENT_POWER;
import static io.openems.edge.batteryinverter.sinexcel.BatteryInverterSinexcel.ChannelId.SET_OFF_GRID_MODE;
import static io.openems.edge.batteryinverter.sinexcel.BatteryInverterSinexcel.ChannelId.SET_ON_GRID_MODE;
import static io.openems.edge.batteryinverter.sinexcel.BatteryInverterSinexcel.ChannelId.STATE_MACHINE;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.test.TimeLeapClock;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.test.DummyBattery;
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

public class BatteryInverterSinexcelImplTest {

	private static class MyComponentTest extends ComponentTest {

		private final Battery battery = new DummyBattery("battery0");

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
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("batteryInverter0") //
						.setStartStopConfig(StartStopConfig.START) //
						.setModbusId("modbus0") //
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
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("batteryInverter0") //
						.setStartStopConfig(StartStopConfig.START) //
						.setModbusId("modbus0") //
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
