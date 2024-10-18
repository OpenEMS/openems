package io.openems.edge.battery.fenecon.commercial;

import static io.openems.edge.battery.api.Battery.ChannelId.CHARGE_MAX_CURRENT;
import static io.openems.edge.battery.api.Battery.ChannelId.DISCHARGE_MAX_CURRENT;
import static io.openems.edge.battery.api.Battery.ChannelId.SOC;
import static io.openems.edge.battery.fenecon.commercial.BatteryFeneconCommercial.ChannelId.BATTERY_SOC;
import static io.openems.edge.battery.fenecon.commercial.BatteryFeneconCommercial.ChannelId.RUNNING;
import static io.openems.edge.battery.fenecon.commercial.BatteryFeneconCommercial.ChannelId.STATE_MACHINE;
import static io.openems.edge.common.startstop.StartStoppable.ChannelId.START_STOP;
import static io.openems.edge.common.test.TestUtils.createDummyClock;
import static io.openems.edge.io.test.DummyInputOutput.ChannelId.INPUT_OUTPUT7;

import org.junit.Test;

import io.openems.edge.battery.fenecon.commercial.statemachine.StateMachine;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.startstop.StartStopConfig;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.io.test.DummyInputOutput;

public class BatteryFeneconCommercialImplTest {

	@Test
	public void startBattery() throws Exception {
		final var clock = createDummyClock();
		new ComponentTest(new BatteryFeneconCommercialImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.addComponent(new DummyInputOutput("io0"))//
				.activate(MyConfig.create() //
						.setId("battery0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.setStartStop(StartStopConfig.START) //
						.setBatteryStartStopRelay("io0/InputOutput7")//
						.build())//
				.next(new TestCase("Battery Relay false, starting") //
						.input("io0", INPUT_OUTPUT7, false)//
						.input(RUNNING, false)// Switched Off
						.output(STATE_MACHINE, StateMachine.State.UNDEFINED))//
				.next(new TestCase() //
						.output(STATE_MACHINE, StateMachine.State.GO_RUNNING)) //
				.next(new TestCase()//
						.output(STATE_MACHINE, StateMachine.State.GO_RUNNING))//
				.next(new TestCase()//
						.input("io0", INPUT_OUTPUT7, true))//
				.next(new TestCase() //
						.output(STATE_MACHINE, StateMachine.State.GO_RUNNING))//
				.next(new TestCase()//
						.input(RUNNING, true)//
						.input("io0", INPUT_OUTPUT7, false))//
				.next(new TestCase() //
						.output(STATE_MACHINE, StateMachine.State.GO_RUNNING))//
				.next(new TestCase("Battery Running")//
						.output(STATE_MACHINE, StateMachine.State.RUNNING))//
				.next(new TestCase("Battery Running")//
						.output("io0", INPUT_OUTPUT7, false)//
						.output(RUNNING, true) //
						.output(STATE_MACHINE, StateMachine.State.RUNNING))//

		;
	}

	@Test
	public void stopBattery() throws Exception {
		final var clock = createDummyClock();
		new ComponentTest(new BatteryFeneconCommercialImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.addComponent(new DummyInputOutput("io0"))//
				.activate(MyConfig.create() //
						.setId("battery0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.setStartStop(StartStopConfig.STOP) //
						.setBatteryStartStopRelay("io0/InputOutput7")//
						.build())//
				.next(new TestCase("Battery Running")//
						.input("io0", INPUT_OUTPUT7, false)//
						.input(RUNNING, true) //
						.input(STATE_MACHINE, StateMachine.State.RUNNING))//
				.next(new TestCase("Stopping") //
						.input(START_STOP, StartStopConfig.STOP)) //
				.next(new TestCase()//
						.output(STATE_MACHINE, StateMachine.State.GO_STOPPED))//
				.next(new TestCase()//
						.input("io0", INPUT_OUTPUT7, true)) //
				.next(new TestCase()//
						.output(STATE_MACHINE, StateMachine.State.STOPPED))//

		;
	}

	@Test
	public void socManipulationMin() throws Exception {
		final var clock = createDummyClock();
		new ComponentTest(new BatteryFeneconCommercialImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.addComponent(new DummyInputOutput("io0"))//
				.activate(MyConfig.create() //
						.setId("battery0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.setStartStop(StartStopConfig.START) //
						.setBatteryStartStopRelay("io0/InputOutput7")//
						.build())//
				.next(new TestCase("Soc")//
						.input(RUNNING, true) //
						.input(BATTERY_SOC, 10) //
						.input(DISCHARGE_MAX_CURRENT, 0) //
						.input(CHARGE_MAX_CURRENT, 10000) //
						.output(SOC, 10));
	}

	@Test
	public void socManipulationMax() throws Exception {
		final var clock = createDummyClock();
		new ComponentTest(new BatteryFeneconCommercialImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.addComponent(new DummyInputOutput("io0"))//
				.activate(MyConfig.create() //
						.setId("battery0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.setStartStop(StartStopConfig.START) //
						.setBatteryStartStopRelay("io0/InputOutput7")//
						.build())//
				.next(new TestCase("Soc")//
						.input(RUNNING, true) //
						.input(BATTERY_SOC, 98) //
						.input(DISCHARGE_MAX_CURRENT, 100000) //
						.input(CHARGE_MAX_CURRENT, 0) //
						.output(SOC, 100));
	}
}
