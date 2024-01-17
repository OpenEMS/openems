package io.openems.edge.battery.pylontech.powercubem2;

import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.pylontech.powercubem2.PylontechPowercubeM2Battery.Status;
import io.openems.edge.battery.pylontech.powercubem2.statemachine.StateMachine;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStopConfig;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.common.test.TimeLeapClock;

public class PylontechPowercubeM2GenericEssIntegrationTest {
	
	private static final String BATTERY_ID = "battery0";
	private static final String MODBUS_ID = "modbus0";
	
	private static final ChannelAddress STATE_MACHINE = new ChannelAddress(BATTERY_ID,
			PylontechPowercubeM2Battery.ChannelId.STATE_MACHINE.id());
	private static final ChannelAddress STATUS_CHANNEL = new ChannelAddress(BATTERY_ID,
			PylontechPowercubeM2Battery.ChannelId.SYSTEM_STATUS.id());
	private static final ChannelAddress START_STOP = new ChannelAddress(BATTERY_ID,
			StartStoppable.ChannelId.START_STOP.id());
	
	
	@Test
	public void startBatteryTest() throws Exception {
		
		final var clock = new TimeLeapClock(Instant.parse("2020-01-01T01:00:00.00Z"), ZoneOffset.UTC);
		
		PylontechPowercubeM2BatteryImpl sut = new PylontechPowercubeM2BatteryImpl();
		
		ComponentTest test = new ComponentTest(sut) //
			.addReference("cm", new DummyConfigurationAdmin()) //
			.addReference("componentManager", new DummyComponentManager(clock)) //
			.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
			.activate(MyConfig.create() //
					.setId(BATTERY_ID) //
					.setModbusId(MODBUS_ID) //
					.setModbusUnitId(1) //
					.setStartStop(StartStopConfig.AUTO) //
					.build())
			.next(new TestCase("Start in UNDEFINED state. The StartStop is UNDEFINED to begin with")
					.input(STATUS_CHANNEL, Status.IDLE)
					.output(STATE_MACHINE, StateMachine.State.UNDEFINED)
					.output(START_STOP, StartStop.UNDEFINED))
			.next(new TestCase("Check that nothing is changing without external input")
					.output(STATUS_CHANNEL, Status.IDLE)
					.output(STATE_MACHINE, StateMachine.State.UNDEFINED)
					.output(START_STOP, StartStop.UNDEFINED))
			.next(new TestCase("Check that nothing is changing without external input")
					.output(STATUS_CHANNEL, Status.IDLE)
					.output(STATE_MACHINE, StateMachine.State.UNDEFINED)
					.output(START_STOP, StartStop.UNDEFINED))
				
			.next(new TestCase("Try to start it")
				);
		
		sut.setStartStop(StartStop.START);
		
		test.next(new TestCase("Nothing has changed (yet)")
					.output(STATE_MACHINE, StateMachine.State.UNDEFINED)
					.output(START_STOP, StartStop.UNDEFINED))
			.next(new TestCase("Wait one cycle"))
			.next(new TestCase("Have we broken anything?")
				.output(STATE_MACHINE, StateMachine.State.GO_RUNNING)
				.output(START_STOP, StartStop.UNDEFINED)
				.output(STATUS_CHANNEL, Status.IDLE))
			.next(new TestCase("Wait one cycle"))
			.next(new TestCase("The battery is now RUNNING, and START_STOP has been set to START to mark battery as running.")
					.output(STATE_MACHINE, StateMachine.State.RUNNING)
					.output(START_STOP, StartStop.START)
					.output(STATUS_CHANNEL, Status.IDLE));
	}
}
