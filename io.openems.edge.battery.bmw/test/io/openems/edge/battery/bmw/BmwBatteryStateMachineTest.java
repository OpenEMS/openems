package io.openems.edge.battery.bmw;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.battery.bmw.enums.BmsState;
import io.openems.edge.battery.bmw.statemachine.StateMachine.State;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.startstop.StartStopConfig;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;

public class BmwBatteryStateMachineTest {

	private final static String BATTERY_ID = "bms0";
	private final static String MODBUS_ID = "modbus0";
	private final static StartStopConfig START = StartStopConfig.START;
	private final static StartStopConfig STOP = StartStopConfig.STOP;

	private static final ChannelAddress STATE_MACHINE = new ChannelAddress(BATTERY_ID, "StateMachine");
	private static final ChannelAddress BMS_STATE = new ChannelAddress(BATTERY_ID, "BmsState");

	@Test
	public void testStart() throws Exception {
		ComponentTest test = new ComponentTest(new BmwBatteryImpl());

		test.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //

				.activate(MyConfig.create() //
						.setModbusId(MODBUS_ID) //
						.setId(BATTERY_ID) //
						.setStartStop(START) //
						.setModbusId(MODBUS_ID) //
						.build()) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.UNDEFINED)) //

				.next(new TestCase() //
						.output(STATE_MACHINE, State.GO_RUNNING)) //

				.next(new TestCase() //
						.input(BMS_STATE, BmsState.OPERATION)) //

				.next(new TestCase() //
						.output(STATE_MACHINE, State.RUNNING)) //
		;

	}

	@Test
	public void testStop() throws Exception {
		ComponentTest test = new ComponentTest(new BmwBatteryImpl());

		test.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //

				.activate(MyConfig.create() //
						.setModbusId(MODBUS_ID) //
						.setId(BATTERY_ID) //
						.setStartStop(STOP) //
						.setModbusId(MODBUS_ID) //
						.build()) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.UNDEFINED)) //

				.next(new TestCase() //
						.output(STATE_MACHINE, State.GO_STOPPED)) //

				.next(new TestCase() //
						.input(BMS_STATE, BmsState.OFF)) //

				.next(new TestCase() //
						.output(STATE_MACHINE, State.STOPPED)) //
		;

	}

}
