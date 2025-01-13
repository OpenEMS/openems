package io.openems.edge.battery.pylontech.powercubem2;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.startstop.StartStopConfig;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.battery.pylontech.powercubem2.statemachine.StateMachine;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;

import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.Test;

/**
 * Checks that the battery wakes up correctly and progresses through the state
 * machine in response to commands.
 */
public class PylontechPowercubeM2BatteryImplTest {

	private static final String BATTERY_ID = "battery0";
	private static final String MODBUS_ID = "modbus0";

	private static final ChannelAddress STATE_MACHINE = new ChannelAddress(BATTERY_ID,
			PylontechPowercubeM2Battery.ChannelId.STATE_MACHINE.id());
	private static final ChannelAddress STATUS_CHANNEL = new ChannelAddress(BATTERY_ID,
			PylontechPowercubeM2Battery.ChannelId.BASIC_STATUS.id());
	
	private static final ChannelAddress N_PILES_CHANNEL = new ChannelAddress(BATTERY_ID,
			PylontechPowercubeM2Battery.ChannelId.NUMBER_OF_PILES_IN_PARALLEL.id());
	
	@Test
	public void wakeUpBattery() throws Exception {

		new ComponentTest(new PylontechPowercubeM2BatteryImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.activate(MyConfig.create() //
						.setId(BATTERY_ID) //
						.setModbusId(MODBUS_ID) //
						.setModbusUnitId(1) //
						.setStartStop(StartStopConfig.START) //
						.build())
				.next(new TestCase("Check State Machine = UNDEFINED to begin")
						.input(STATUS_CHANNEL, Status.SLEEP) // Ensure that it is in sleep to begin
						.output(STATE_MACHINE, StateMachine.State.UNDEFINED))
				.next(new TestCase("Check that it moves to GO_RUNNING")
						.output(STATE_MACHINE, StateMachine.State.GO_RUNNING).input(STATUS_CHANNEL, Status.CHARGE))
				.next(new TestCase("Stays in GO_RUNNING for one cycle.").output(STATE_MACHINE,
						StateMachine.State.GO_RUNNING))
				.next(new TestCase("Then moves into running state.").output(STATE_MACHINE, StateMachine.State.RUNNING));
	}

	@Test
	public void stopBattery() throws Exception {

		new ComponentTest(new PylontechPowercubeM2BatteryImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.activate(MyConfig.create() //
						.setId(BATTERY_ID) //
						.setModbusId(MODBUS_ID) //
						.setModbusUnitId(1) //
						.setStartStop(StartStopConfig.STOP) //
						.build())
				.next(new TestCase("Battery running")
						.input(STATUS_CHANNEL, Status.CHARGE)
						.input(STATE_MACHINE,
						StateMachine.State.RUNNING))
				.next(new TestCase("Stopping")
						.output(STATE_MACHINE, StateMachine.State.GO_STOPPED)
						.input(STATUS_CHANNEL, Status.SLEEP))
				.next(new TestCase("Waiting for STOPPED")
						.output(STATE_MACHINE, StateMachine.State.GO_STOPPED))
				.next(new TestCase("Stopped").output(STATE_MACHINE, StateMachine.State.STOPPED));
	}

	@Test
	public void faultTest() throws Exception { // TODO: Set faults for this test

		new ComponentTest(new PylontechPowercubeM2BatteryImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.activate(MyConfig.create() //
						.setId(BATTERY_ID) //
						.setModbusId(MODBUS_ID) //
						.setModbusUnitId(1) //
						.setStartStop(StartStopConfig.AUTO) //
						.build())
				.next(new TestCase("Battery running").input(STATUS_CHANNEL, Status.CHARGE)
						.input(STATE_MACHINE, StateMachine.State.RUNNING));
	}
	
	@Test
	public void nPilesTest() throws Exception { // Check that channels can be added dynamically
		new ComponentTest(new PylontechPowercubeM2BatteryImpl())  //
			.addReference("cm", new DummyConfigurationAdmin()) //
			.addReference("componentManager", new DummyComponentManager()) //
			.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
			.activate(MyConfig.create() //
					.setId(BATTERY_ID) //
					.setModbusId(MODBUS_ID) //
					.setModbusUnitId(1) //
					.setStartStop(StartStopConfig.AUTO) //
					.build())
			.next(new TestCase("Set n Piles")
					.input(N_PILES_CHANNEL, 2)
					.output(BATTERY_ID, "Pile1SystemErrorProtection", false)
					.output(BATTERY_ID, "Pile2SystemErrorProtection", false)
					.output(BATTERY_ID, "Pile1Current", null)
					.output(BATTERY_ID, "Pile2Current", null));
		
	}

}