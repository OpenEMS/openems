package io.openems.edge.battery.pylontech.powercubem2;

import org.junit.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.battery.pylontech.powercubem2.statemachine.StateMachine;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.startstop.StartStopConfig;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;

/**
 * Checks that the battery wakes up correctly and progresses through the state
 * machine in response to commands.
 */
public class PylontechPowercubeM2BatteryImplTest {

	@Test
	public void wakeUpBattery() throws Exception {
		new ComponentTest(new PylontechPowercubeM2BatteryImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("battery0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.setStartStop(StartStopConfig.START) //
						.build())
				// Ensure that it is in sleep to begin
				.next(new TestCase("Check State Machine = UNDEFINED to begin") //
						.input(PylontechPowercubeM2Battery.ChannelId.BASIC_STATUS, Status.SLEEP) //
						.output(PylontechPowercubeM2Battery.ChannelId.STATE_MACHINE, StateMachine.State.UNDEFINED)) //
				.next(new TestCase("Check that it moves to GO_RUNNING") //
						.output(PylontechPowercubeM2Battery.ChannelId.STATE_MACHINE, StateMachine.State.GO_RUNNING) //
						.input(PylontechPowercubeM2Battery.ChannelId.BASIC_STATUS, Status.CHARGE)) //
				.next(new TestCase("Stays in GO_RUNNING for one cycle.") //
						.output(PylontechPowercubeM2Battery.ChannelId.STATE_MACHINE, StateMachine.State.GO_RUNNING))
				.next(new TestCase("Then moves into running state.") //
						.output(PylontechPowercubeM2Battery.ChannelId.STATE_MACHINE, StateMachine.State.RUNNING));
	}

	@Test
	public void stopBattery() throws Exception {
		new ComponentTest(new PylontechPowercubeM2BatteryImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("battery0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.setStartStop(StartStopConfig.STOP) //
						.build())
				.next(new TestCase("Battery running") //
						.input(PylontechPowercubeM2Battery.ChannelId.BASIC_STATUS, Status.CHARGE) //
						.input(PylontechPowercubeM2Battery.ChannelId.STATE_MACHINE, StateMachine.State.RUNNING))
				.next(new TestCase("Stopping") //
						.output(PylontechPowercubeM2Battery.ChannelId.STATE_MACHINE, StateMachine.State.GO_STOPPED) //
						.input(PylontechPowercubeM2Battery.ChannelId.BASIC_STATUS, Status.SLEEP)) //
				.next(new TestCase("Waiting for STOPPED") //
						.output(PylontechPowercubeM2Battery.ChannelId.STATE_MACHINE, StateMachine.State.GO_STOPPED)) //
				.next(new TestCase("Stopped") //
						.output(PylontechPowercubeM2Battery.ChannelId.STATE_MACHINE, StateMachine.State.STOPPED));
	}

	@Test
	public void faultTest() throws Exception { // TODO: Set faults for this test
		new ComponentTest(new PylontechPowercubeM2BatteryImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("battery0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.setStartStop(StartStopConfig.AUTO) //
						.build())
				.next(new TestCase("Battery running") //
						.input(PylontechPowercubeM2Battery.ChannelId.NUMBER_OF_PILES_IN_PARALLEL, Status.CHARGE) //
						.input(PylontechPowercubeM2Battery.ChannelId.STATE_MACHINE, StateMachine.State.RUNNING));
	}

	@Test
	public void nPilesTest() throws Exception { // Check that channels can be added dynamically
		new ComponentTest(new PylontechPowercubeM2BatteryImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("battery0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.setStartStop(StartStopConfig.AUTO) //
						.build())
				.next(new TestCase("Set n Piles") //
						.input(PylontechPowercubeM2Battery.ChannelId.NUMBER_OF_PILES_IN_PARALLEL, 2) //
						.output("battery0", "Pile1SystemErrorProtection", false) //
						.output("battery0", "Pile2SystemErrorProtection", false) //
						.output("battery0", "Pile1Current", null) //
						.output("battery0", "Pile2Current", null));
	}
}