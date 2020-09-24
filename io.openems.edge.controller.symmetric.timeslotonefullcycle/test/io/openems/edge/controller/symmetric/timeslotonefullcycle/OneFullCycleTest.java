package io.openems.edge.controller.symmetric.timeslotonefullcycle;

import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.common.test.TimeLeapClock;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;

public class OneFullCycleTest {

	private static final String CTRL_ID = "ctrl0";
	private static final String ESS_ID = "ess0";
	private static final ChannelAddress CTRL_STATE_MACHINE = new ChannelAddress(CTRL_ID, "StateMachine");
	private static final ChannelAddress ESS_ACTIVE_POWER = new ChannelAddress(ESS_ID, "ActivePower");

	@Test
	public void test() throws Exception {
		final long start = 1600943428L;
		final TimeLeapClock clock = new TimeLeapClock(Instant.ofEpochSecond(start), ZoneOffset.UTC);
		new ControllerTest(new OneFullCycle()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("ess", new DummyManagedSymmetricEss(ESS_ID)) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEssId(ESS_ID) //
						.setSchedule(JsonUtils.buildJsonArray()//
								.add(JsonUtils.buildJsonObject()//
										.addProperty("startTimestamp", start + 500) //
										.addProperty("duration", 900) //
										.addProperty("activePowerSetPoint", 10_000) //
										.build() //
								).build().toString() //
						).build()) //
				.next(new TestCase("No active setpoint") //
						.input(ESS_ACTIVE_POWER, 1000) //
						.output(CTRL_STATE_MACHINE, true))//
		;
	}
}