package io.openems.edge.battery.fenecon.commercial;

import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.fenecon.commercial.statemachine.StateMachine;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.startstop.StartStopConfig;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.common.test.TimeLeapClock;
import io.openems.edge.io.test.DummyInputOutput;

public class BatteryFeneconCommercialImplTest {

	private static final String BATTERY_ID = "battery0";
	private static final String MODBUS_ID = "modbus0";
	private static final String IO_ID = "io0";

	private static final ChannelAddress STATE_MACHINE = new ChannelAddress(BATTERY_ID,
			BatteryFeneconCommercial.ChannelId.STATE_MACHINE.id());
	private static final ChannelAddress RUNNING = new ChannelAddress(BATTERY_ID,
			BatteryFeneconCommercial.ChannelId.RUNNING.id());
	private static final ChannelAddress BATTERY_RELAY = new ChannelAddress(IO_ID, "InputOutput7");
	private static final ChannelAddress START_STOP = new ChannelAddress(BATTERY_ID,
			StartStoppable.ChannelId.START_STOP.id());
	private static final ChannelAddress BATTERY_SOC = new ChannelAddress(BATTERY_ID,
			BatteryFeneconCommercial.ChannelId.BATTERY_SOC.id());
	private static final ChannelAddress BATTERY_MAX_DISCHARGE_CURRENT = new ChannelAddress(BATTERY_ID,
			Battery.ChannelId.DISCHARGE_MAX_CURRENT.id());
	private static final ChannelAddress BATTERY_MAX_CHARGE_CURRENT = new ChannelAddress(BATTERY_ID,
			Battery.ChannelId.CHARGE_MAX_CURRENT.id());
	private static final ChannelAddress SOC = new ChannelAddress(BATTERY_ID, Battery.ChannelId.SOC.id());

	@Test
	public void startBattery() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2020-01-01T01:00:00.00Z"), ZoneOffset.UTC);
		new ComponentTest(new BatteryFeneconCommercialImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.addComponent(new DummyInputOutput(IO_ID))//
				.activate(MyConfig.create() //
						.setId(BATTERY_ID) //
						.setModbusId(MODBUS_ID) //
						.setModbusUnitId(1) //
						.setStartStop(StartStopConfig.START) //
						.setBatteryStartStopRelay("io0/InputOutput7")//
						.build())//
				.next(new TestCase("Battery Relay false, starting") //
						.input(BATTERY_RELAY, false)//
						.input(RUNNING, false)// Switched Off
						.output(STATE_MACHINE, StateMachine.State.UNDEFINED))//
				.next(new TestCase() //
						.output(STATE_MACHINE, StateMachine.State.GO_RUNNING)) //
				.next(new TestCase()//
						.output(STATE_MACHINE, StateMachine.State.GO_RUNNING))//
				.next(new TestCase()//
						.input(BATTERY_RELAY, true))//
				.next(new TestCase() //
						.output(STATE_MACHINE, StateMachine.State.GO_RUNNING))//
				.next(new TestCase()//
						.input(RUNNING, true)//
						.input(BATTERY_RELAY, false))//
				.next(new TestCase() //
						.output(STATE_MACHINE, StateMachine.State.GO_RUNNING))//
				.next(new TestCase("Battery Running")//
						.output(STATE_MACHINE, StateMachine.State.RUNNING))//
				.next(new TestCase("Battery Running")//
						.output(BATTERY_RELAY, false)//
						.output(RUNNING, true) //
						.output(STATE_MACHINE, StateMachine.State.RUNNING))//

		;
	}

	@Test
	public void stopBattery() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2020-01-01T01:00:00.00Z"), ZoneOffset.UTC);
		new ComponentTest(new BatteryFeneconCommercialImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.addComponent(new DummyInputOutput(IO_ID))//
				.activate(MyConfig.create() //
						.setId(BATTERY_ID) //
						.setModbusId(MODBUS_ID) //
						.setModbusUnitId(1) //
						.setStartStop(StartStopConfig.STOP) //
						.setBatteryStartStopRelay("io0/InputOutput7")//
						.build())//
				.next(new TestCase("Battery Running")//
						.input(BATTERY_RELAY, false)//
						.input(RUNNING, true) //
						.input(STATE_MACHINE, StateMachine.State.RUNNING))//
				.next(new TestCase("Stopping") //
						.input(START_STOP, StartStopConfig.STOP)) //
				.next(new TestCase()//
						.output(STATE_MACHINE, StateMachine.State.GO_STOPPED))//
				.next(new TestCase()//
						.input(BATTERY_RELAY, true)) //
				.next(new TestCase()//
						.output(STATE_MACHINE, StateMachine.State.STOPPED))//

		;
	}

	@Test
	public void socManipulationMin() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2020-01-01T01:00:00.00Z"), ZoneOffset.UTC);
		new ComponentTest(new BatteryFeneconCommercialImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.addComponent(new DummyInputOutput(IO_ID))//
				.activate(MyConfig.create() //
						.setId(BATTERY_ID) //
						.setModbusId(MODBUS_ID) //
						.setModbusUnitId(1) //
						.setStartStop(StartStopConfig.START) //
						.setBatteryStartStopRelay("io0/InputOutput7")//
						.build())//
				.next(new TestCase("Soc")//
						.input(RUNNING, true) //
						.input(BATTERY_SOC, 10) //
						.input(BATTERY_MAX_DISCHARGE_CURRENT, 0) //
						.input(BATTERY_MAX_CHARGE_CURRENT, 10000) //
						.output(SOC, 10));
	}

	@Test
	public void socManipulationMax() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2020-01-01T01:00:00.00Z"), ZoneOffset.UTC);
		new ComponentTest(new BatteryFeneconCommercialImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.addComponent(new DummyInputOutput(IO_ID))//
				.activate(MyConfig.create() //
						.setId(BATTERY_ID) //
						.setModbusId(MODBUS_ID) //
						.setModbusUnitId(1) //
						.setStartStop(StartStopConfig.START) //
						.setBatteryStartStopRelay("io0/InputOutput7")//
						.build())//
				.next(new TestCase("Soc")//
						.input(RUNNING, true) //
						.input(BATTERY_SOC, 98) //
						.input(BATTERY_MAX_DISCHARGE_CURRENT, 100000) //
						.input(BATTERY_MAX_CHARGE_CURRENT, 0) //
						.output(SOC, 100));
	}
}
