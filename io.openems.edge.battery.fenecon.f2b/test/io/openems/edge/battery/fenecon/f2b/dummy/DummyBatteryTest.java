package io.openems.edge.battery.fenecon.f2b.dummy;

import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.Test;

import io.openems.common.test.TimeLeapClock;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.battery.fenecon.f2b.dummy.statemachine.StateMachine;
import io.openems.edge.common.startstop.StartStopConfig;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;

public class DummyBatteryTest {
	private static final String BATTERY_ID = "battery0";

	private static final ChannelAddress STATE_MACHINE = new ChannelAddress(BATTERY_ID,
			BatteryFeneconF2bDummy.ChannelId.STATE_MACHINE.id());

	@Test
	public void startBattery() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2020-01-01T01:00:00.00Z"), ZoneOffset.UTC);
		var battery = new BatteryFeneconF2bDummyImpl();
		new ComponentTest(battery) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.activate(MyConfig.create() //
						.setId(BATTERY_ID) //
						.setStartStop(StartStopConfig.START) //
						.build())//
				.next(new TestCase()//
						.output(STATE_MACHINE, StateMachine.State.UNDEFINED))//
				.next(new TestCase()//
						.output(STATE_MACHINE, StateMachine.State.GO_STOPPED))//
				.next(new TestCase()//
						.output(STATE_MACHINE, StateMachine.State.STOPPED))//
				.next(new TestCase()//
						.output(STATE_MACHINE, StateMachine.State.GO_RUNNING))//
				.next(new TestCase()//
						.onAfterProcessImage(() -> battery.setHvContactorUnlocked(true)))//
				.next(new TestCase()//
						.output(STATE_MACHINE, StateMachine.State.RUNNING))//
		;
	}
}