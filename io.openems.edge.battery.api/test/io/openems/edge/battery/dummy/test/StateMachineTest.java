package io.openems.edge.battery.dummy.test;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.battery.dummy.DummyBattery;
import io.openems.edge.battery.dummy.DummyBatteryImpl;
import io.openems.edge.battery.statemachine.StateMachine;
import io.openems.edge.common.startstop.StartStopConfig;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.common.test.TimeLeapClock;

public class StateMachineTest {
	private static final String BATTERY_ID = "battery0";

	private static final ChannelAddress STATE_MACHINE = new ChannelAddress(BATTERY_ID,
			DummyBattery.ChannelId.STATE_MACHINE.id());

	@Test
	public void startBattery() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2020-01-01T01:00:00.00Z"), ZoneOffset.UTC);
		new ComponentTest(new DummyBatteryImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.activate(MyConfig.create() //
						.setId(BATTERY_ID) //
						.setStartStop(StartStopConfig.START) //
						.setBatteryStartStopTime(10)//
						.build())//
				.next(new TestCase()//
						.output(STATE_MACHINE, StateMachine.State.UNDEFINED))//
				.next(new TestCase()//
						.output(STATE_MACHINE, StateMachine.State.GO_RUNNING))//
				.next(new TestCase()//
						.timeleap(clock, 11, ChronoUnit.SECONDS))//
				.next(new TestCase()//
						.output(STATE_MACHINE, StateMachine.State.RUNNING))//
		;
	}
}