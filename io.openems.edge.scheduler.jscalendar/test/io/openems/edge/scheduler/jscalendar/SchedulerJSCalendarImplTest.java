package io.openems.edge.scheduler.jscalendar;

import static io.openems.common.test.TestUtils.createDummyClock;
import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static java.time.temporal.ChronoUnit.HOURS;
import static org.junit.Assert.assertEquals;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.util.List;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jscalendar.JSCalendar;
import io.openems.common.jscalendar.JSCalendar.RecurrenceFrequency;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.DummyController;
import io.openems.edge.scheduler.api.Scheduler;
import io.openems.edge.scheduler.jscalendar.Utils.Payload;

//CHECKSTYLE:OFF
public class SchedulerJSCalendarImplTest {
	// CHECKSTYLE:ON

	@Test
	public void test() throws Exception {
		final var ctrls = new String[] { "ctrl0", "ctrl1" };
		final var scheduleConfig = ImmutableList.of(//
				JSCalendar.Task.<Payload>create() //
						.setStart(LocalTime.of(8, 30)) //
						.setDuration(Duration.ofHours(10)).addRecurrenceRule(b -> b //
								.setFrequency(RecurrenceFrequency.WEEKLY) //
								.addByDay(TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)) //
						.setPayload(new Payload(ctrls)) //
						.build(), //
				JSCalendar.Task.<Payload>create() //
						.setStart(LocalTime.of(7, 30)) //
						.setDuration(Duration.ofHours(14)).addRecurrenceRule(b -> b //
								.setFrequency(RecurrenceFrequency.WEEKLY) //
								.addByDay(DayOfWeek.SATURDAY) //
								.addByDay(DayOfWeek.SUNDAY)) //
						.setPayload(new Payload(ctrls)) //
						.build());

		final var clock = createDummyClock();
		final SchedulerJSCalendarImpl sut = new SchedulerJSCalendarImpl();
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
						.setJSCalendar(JSCalendar.Tasks.serializer(Payload.serializer()) //
								.serialize(scheduleConfig) //
								.toString()) //
						.setAlwaysRunAfterControllerIds("ctrl3", "ctrl4") //
						.build()) //
				.next(new TestCase("02:00") //
						.onBeforeControllersCallbacks(() -> assertEquals(//
								List.of("ctrl2", "ctrl3", "ctrl4"), //
								getControllerIds(sut)))) //
				.next(new TestCase("12:00") //
						.timeleap(clock, 12, HOURS) //
						.onBeforeControllersCallbacks(() -> assertEquals(//
								List.of("ctrl2", "ctrl0", "ctrl1", "ctrl3", "ctrl4"), //
								getControllerIds(sut))));
	}

	private static List<String> getControllerIds(Scheduler scheduler) throws OpenemsNamedException {
		return scheduler.getControllers().stream() //
				.toList();
	}

}
