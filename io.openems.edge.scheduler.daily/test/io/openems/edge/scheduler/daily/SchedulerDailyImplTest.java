package io.openems.edge.scheduler.daily;

import static io.openems.common.utils.JsonUtils.buildJsonArray;
import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.edge.common.test.TestUtils.createDummyClock;
import static java.time.temporal.ChronoUnit.HOURS;
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.DummyController;
import io.openems.edge.scheduler.api.Scheduler;

public class SchedulerDailyImplTest {

	@Test
	public void test() throws Exception {
		final var clock = createDummyClock();
		final SchedulerDaily sut = new SchedulerDailyImpl();
		new ComponentTest(sut) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addComponent(new DummyController("ctrl0")) //
				.addComponent(new DummyController("ctrl1")) //
				.addComponent(new DummyController("ctrl2")) //
				.addComponent(new DummyController("ctrl3")) //
				.addComponent(new DummyController("ctrl4")) //
				.activate(MyConfig.create() //
						.setId("scheduler0") //
						.setAlwaysRunBeforeControllerIds("ctrl2") //
						.setControllerScheduleJson(buildJsonArray() //
								.add(buildJsonObject() //
										.addProperty("time", "08:00:00") //
										.add("controllers", buildJsonArray() //
												.add("ctrl0") //
												.build()) //
										.build()) //
								.add(buildJsonObject() //
										.addProperty("time", "13:45:00") //
										.add("controllers", buildJsonArray() //
												.add("ctrl4") //
												.build()) //
										.build()) //
								.build().toString())
						.setAlwaysRunAfterControllerIds("ctrl3", "ctrl1") //
						.build()) //
				.next(new TestCase("00:00") //
						.onBeforeControllersCallbacks(() -> assertEquals(//
								List.of("ctrl2", "ctrl4", "ctrl3", "ctrl1"), //
								getControllerIds(sut)))) //
				.next(new TestCase("12:00") //
						.timeleap(clock, 12, HOURS) //
						.onBeforeControllersCallbacks(() -> assertEquals(//
								List.of("ctrl2", "ctrl0", "ctrl3", "ctrl1"), //
								getControllerIds(sut))))
				.next(new TestCase("14:00") //
						.timeleap(clock, 12, HOURS) //
						.onBeforeControllersCallbacks(() -> assertEquals(//
								List.of("ctrl2", "ctrl4", "ctrl3", "ctrl1"), //
								getControllerIds(sut))));
	}

	private static List<String> getControllerIds(Scheduler scheduler) throws OpenemsNamedException {
		return scheduler.getControllers().stream() //
				.toList();
	}

}
