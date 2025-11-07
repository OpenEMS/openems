package io.openems.edge.battery.pylontech.powercubem2;

import org.junit.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.battery.pylontech.powercubem2.statemachine.StateMachine;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStopConfig;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;

public class PylontechPowercubeM2GenericEssIntegrationTest {

	private static final ChannelAddress STATE_MACHINE = new ChannelAddress("battery0",
			PylontechPowercubeM2Battery.ChannelId.STATE_MACHINE.id());
	private static final ChannelAddress STATUS_CHANNEL = new ChannelAddress("battery0",
			PylontechPowercubeM2Battery.ChannelId.BASIC_STATUS.id());
	private static final ChannelAddress START_STOP = new ChannelAddress("battery0",
			StartStoppable.ChannelId.START_STOP.id());

	@Test
	public void startBatteryTest() throws Exception {
		var sut = new PylontechPowercubeM2BatteryImpl();
		var test = new ComponentTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("battery0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.setStartStop(StartStopConfig.AUTO) //
						.build())
				.next(new TestCase("Start in UNDEFINED state. The StartStop is UNDEFINED to begin with")
						.input(STATUS_CHANNEL, Status.IDLE) //
						.output(STATE_MACHINE, StateMachine.State.UNDEFINED) //
						.output(START_STOP, StartStop.UNDEFINED))
				.next(new TestCase("Check that nothing is changing without external input")
						.output(STATUS_CHANNEL, Status.IDLE) //
						.output(STATE_MACHINE, StateMachine.State.UNDEFINED) //
						.output(START_STOP, StartStop.UNDEFINED))

				.next(new TestCase("Try to start it"));

		sut.setStartStop(StartStop.START);

		test //
				.next(new TestCase("Nothing has changed (yet)") //
						.output(STATE_MACHINE, StateMachine.State.UNDEFINED) //
						.output(START_STOP, StartStop.UNDEFINED))
				.next(new TestCase("Wait one cycle")) //
				.next(new TestCase("Wait.") //
						.output(STATE_MACHINE, StateMachine.State.GO_RUNNING) //
						.output(START_STOP, StartStop.UNDEFINED) //
						.output(STATUS_CHANNEL, Status.IDLE))
				.next(new TestCase("Wait one cycle"))
				.next(new TestCase(
						"The battery is now RUNNING, and START_STOP has been set to START to mark battery as running.")
						.output(STATE_MACHINE, StateMachine.State.RUNNING) //
						.output(START_STOP, StartStop.START) //
						.output(STATUS_CHANNEL, Status.IDLE)) //
				.deactivate();
	}
}
