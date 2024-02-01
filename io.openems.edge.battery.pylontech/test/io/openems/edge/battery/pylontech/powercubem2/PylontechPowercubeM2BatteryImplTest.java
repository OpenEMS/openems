package io.openems.edge.battery.pylontech.powercubem2;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.startstop.StartStopConfig;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.battery.pylontech.powercubem2.statemachine.StateMachine;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.common.test.TimeLeapClock;

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
		final var clock = new TimeLeapClock(Instant.parse("2020-01-01T01:00:00.00Z"), ZoneOffset.UTC);

		new ComponentTest(new PylontechPowercubeM2BatteryImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
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
		final var clock = new TimeLeapClock(Instant.parse("2020-01-01T01:00:00.00Z"), ZoneOffset.UTC);

		new ComponentTest(new PylontechPowercubeM2BatteryImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
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

}