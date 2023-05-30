package io.openems.edge.scheduler.daily;

import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.TimeLeapClock;
import io.openems.edge.controller.test.DummyController;
import io.openems.edge.scheduler.api.Scheduler;

public class DailySchedulerImplTest {

	private static final String SCHEDULER_ID = "scheduler0";

	private static final String CTRL0_ID = "ctrl0";
	private static final String CTRL1_ID = "ctrl1";
	private static final String CTRL2_ID = "ctrl2";
	private static final String CTRL3_ID = "ctrl3";
	private static final String CTRL4_ID = "ctrl4";

	@Test
	public void test() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2020-01-01T00:00:00.00Z"), ZoneOffset.UTC);
		final DailyScheduler sut = new DailySchedulerImpl();
		new ComponentTest(sut) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addComponent(new DummyController(CTRL0_ID)) //
				.addComponent(new DummyController(CTRL1_ID)) //
				.addComponent(new DummyController(CTRL2_ID)) //
				.addComponent(new DummyController(CTRL3_ID)) //
				.addComponent(new DummyController(CTRL4_ID)) //
				.activate(MyConfig.create() //
						.setId(SCHEDULER_ID) //
						.setAlwaysRunBeforeControllerIds(CTRL2_ID).setControllerScheduleJson(JsonUtils.buildJsonArray() //
								.add(JsonUtils.buildJsonObject() //
										.addProperty("time", "08:00:00") //
										.add("controllers", JsonUtils.buildJsonArray() //
												.add(CTRL0_ID) //
												.build()) //
										.build()) //
								.add(JsonUtils.buildJsonObject() //
										.addProperty("time", "13:45:00") //
										.add("controllers", JsonUtils.buildJsonArray() //
												.add(CTRL4_ID) //
												.build()) //
										.build()) //
								.build().toString())
						.setAlwaysRunAfterControllerIds(CTRL3_ID, CTRL1_ID) //
						.build()) //
				.next(new TestCase("00:00") //
						.onBeforeControllersCallbacks(() -> assertEquals(//
								Arrays.asList(CTRL2_ID, CTRL4_ID, CTRL3_ID, CTRL1_ID), //
								getControllerIds(sut)))) //
				.next(new TestCase("12:00") //
						.timeleap(clock, 12, ChronoUnit.HOURS) //
						.onBeforeControllersCallbacks(() -> assertEquals(//
								Arrays.asList(CTRL2_ID, CTRL0_ID, CTRL3_ID, CTRL1_ID), //
								getControllerIds(sut))))
				.next(new TestCase("14:00") //
						.timeleap(clock, 12, ChronoUnit.HOURS) //
						.onBeforeControllersCallbacks(() -> assertEquals(//
								Arrays.asList(CTRL2_ID, CTRL4_ID, CTRL3_ID, CTRL1_ID), //
								getControllerIds(sut))));
	}

	private static List<String> getControllerIds(Scheduler scheduler) throws OpenemsNamedException {
		return scheduler.getControllers().stream() //
				.toList();
	}

}
