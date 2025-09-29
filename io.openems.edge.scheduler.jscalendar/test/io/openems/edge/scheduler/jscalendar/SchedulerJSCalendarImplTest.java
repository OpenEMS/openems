package io.openems.edge.scheduler.jscalendar;

import static io.openems.common.jscalendar.JSCalendar.RecurrenceFrequency.WEEKLY;
import static io.openems.common.test.TestUtils.createDummyClock;
import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.time.Duration;
import java.time.LocalTime;
import java.time.ZonedDateTime;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jscalendar.JSCalendar;
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
		final var tasks = ImmutableList.of(//
				// TODO overlapping tasks are not yet handled properly
				// Suggested solution: split Tasks in such a case
				// JSCalendar.Task.<Payload>create() //
				// .setStart(LocalTime.of(11, 00)) //
				// .setDuration(Duration.ofHours(1)) //
				// .addRecurrenceRule(b -> b //
				// .setFrequency(DAILY)) //
				// .setPayload(new Payload(new String[] { "ctrl3" })) //
				// .build(), //
				JSCalendar.Task.<Payload>create() //
						.setStart(LocalTime.of(8, 30)) //
						.setDuration(Duration.ofHours(10)) //
						.addRecurrenceRule(b -> b //
								.setFrequency(WEEKLY) //
								.addByDay(TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)) //
						.setPayload(new Payload(new String[] { "ctrl0", "ctrl1" })) //
						.build(), //
				JSCalendar.Task.<Payload>create() //
						.setStart(LocalTime.of(7, 30)) //
						.setDuration(Duration.ofHours(14)) //
						.addRecurrenceRule(b -> b //
								.setFrequency(WEEKLY) //
								.addByDay(SATURDAY, SUNDAY)) //
						.setPayload(new Payload(new String[] { "ctrl2" })) //
						.build());

		final var clock = createDummyClock(); // Starts on a WEDNESDAY
		final SchedulerJSCalendarImpl sut = new SchedulerJSCalendarImpl();
		new ComponentTest(sut) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addComponent(new DummyController("ctrl0")) //
				.addComponent(new DummyController("ctrl1")) //
				.addComponent(new DummyController("ctrl2")) //
				.addComponent(new DummyController("ctrl3")) //
				.addComponent(new DummyController("ctrl4")) //
				.addComponent(new DummyController("ctrl5")) //
				.addComponent(new DummyController("ctrl6")) //
				.activate(MyConfig.create() //
						.setId("scheduler0") //
						.setAlwaysRunBeforeControllerIds("ctrl4") //
						.setJSCalendar(JSCalendar.Tasks.serializer(Payload.serializer()) //
								.serialize(tasks) //
								.toString()) //
						.setAlwaysRunAfterControllerIds("ctrl5", "ctrl6") //
						.build()) //
				.next(new TestCase("00:00") // No active task
						.onBeforeControllersCallbacks(() -> assertControllerIds(sut, "ctrl4", "ctrl5", "ctrl6"))) //
				.next(new TestCase("08:29") // No active task
						.timeleap(clock, 8 * 60 + 29, MINUTES) //
						.onBeforeControllersCallbacks(() -> assertControllerIds(sut, "ctrl4", "ctrl5", "ctrl6"))) //
				.next(new TestCase("08:30") //
						.timeleap(clock, 1, MINUTES) //
						.onBeforeControllersCallbacks(
								() -> assertControllerIds(sut, "ctrl4", "ctrl0", "ctrl1", "ctrl5", "ctrl6"))) //
				.next(new TestCase("18:29") //
						.timeleap(clock, 9 * 60 + 59, MINUTES) //
						.onBeforeControllersCallbacks(
								() -> assertControllerIds(sut, "ctrl4", "ctrl0", "ctrl1", "ctrl5", "ctrl6"))) //
				.next(new TestCase("18:30") //
						.timeleap(clock, 1, MINUTES) //
						.onBeforeControllersCallbacks(() -> assertControllerIds(sut, "ctrl4", "ctrl5", "ctrl6"))) //
				.next(new TestCase("00:00") // THURSDAY
						.timeleap(clock, 5 * 60 + 30, MINUTES) //
						.onBeforeControllersCallbacks(() -> assertEquals(LocalTime.of(0, 0), LocalTime.now(clock)))) //
				.next(new TestCase("00:00") // SATURDAY
						.timeleap(clock, 2, DAYS) //
						.onBeforeControllersCallbacks(
								() -> assertEquals(SATURDAY, ZonedDateTime.now(clock).getDayOfWeek()))) //
				.next(new TestCase("07:29") //
						.timeleap(clock, 7 * 60 + 29, MINUTES) //
						.onBeforeControllersCallbacks(() -> assertControllerIds(sut, "ctrl4", "ctrl5", "ctrl6"))) //
				.next(new TestCase("07:30") //
						.timeleap(clock, 1, MINUTES) //
						.onBeforeControllersCallbacks(
								() -> assertControllerIds(sut, "ctrl4", "ctrl2", "ctrl5", "ctrl6"))) //
				.next(new TestCase("21:29") //
						.timeleap(clock, 13 * 60 + 59, MINUTES) //
						.onBeforeControllersCallbacks(
								() -> assertControllerIds(sut, "ctrl4", "ctrl2", "ctrl5", "ctrl6"))) //
				.next(new TestCase("21:31") //
						.timeleap(clock, 1, MINUTES) //
						.onBeforeControllersCallbacks(() -> assertControllerIds(sut, "ctrl4", "ctrl5", "ctrl6"))) //
				.deactivate();
	}

	private static void assertControllerIds(Scheduler scheduler, String... controllerIds) throws OpenemsNamedException {
		assertArrayEquals(controllerIds, scheduler.getControllers().stream() //
				.toArray(String[]::new));
	}
}
