package io.openems.common.jscalendar;

import static io.openems.common.jscalendar.JSCalendar.VOID_SERIALIZER;
import static io.openems.common.jscalendar.JSCalendar.RecurrenceFrequency.DAILY;
import static io.openems.common.jscalendar.JSCalendar.RecurrenceFrequency.MONTHLY;
import static io.openems.common.jscalendar.JSCalendar.RecurrenceFrequency.WEEKLY;
import static io.openems.common.jscalendar.JSCalendar.RecurrenceFrequency.YEARLY;
import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.common.utils.JsonUtils.prettyToString;
import static io.openems.common.utils.UuidUtils.getNilUuid;
import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jscalendar.JSCalendar.OneTasks;
import io.openems.common.jscalendar.JSCalendar.RecurrenceRule;
import io.openems.common.jscalendar.JSCalendar.Tasks;
import io.openems.common.jscalendar.JSCalendar.Tasks.OneTask;
import io.openems.common.jsonrpc.serialization.JsonElementPathActual;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.utils.JsonUtils;

//CHECKSTYLE:OFF
public class JSCalendarTest {
	// CHECKSTYLE:ON

	private static final JsonSerializer<JSCalendar.Task<StringPayload>> TASK_SERIALIZER = JSCalendar.Task
			.serializer(StringPayload.serializer());

	private static final LocalDateTime EPOCH = LocalDateTime.of(LocalDate.EPOCH, LocalTime.MIN);
	private static final ZonedDateTime NOW_2000 = ZonedDateTime.now(createDummyClock());

	private static void assertDayOfWeek(ImmutableList<ZonedDateTime> times, int index, String expectedToString,
			DayOfWeek expectedDayOfWeek) {
		assertDayOfWeek(times.get(index), expectedToString, expectedDayOfWeek);
	}

	private static void assertDayOfWeek(ZonedDateTime time, String expectedToString, DayOfWeek expectedDayOfWeek) {
		assertEquals(expectedToString, time.toString());
		assertEquals(expectedDayOfWeek, time.getDayOfWeek());
	}

	private static void assertOneTask(OneTask<?> oneTask, String expectedStartString, String expectedEndString,
			String expectedPayloadString) {
		assertEquals(expectedStartString, oneTask.start().toString());
		assertEquals(expectedEndString, oneTask.end().toString());
		assertEquals(expectedPayloadString, oneTask.payload().toString());
	}

	private static void assertOneTask(OneTask<?> oneTask, DayOfWeek expectedDayOfWeek, String expectedStartAndEndString,
			String expectedPayloadString) {
		assertOneTask(oneTask, expectedStartAndEndString, expectedStartAndEndString, expectedPayloadString);
		assertDayOfWeek(oneTask.start(), expectedStartAndEndString, expectedDayOfWeek);
	}

	@Test
	public void testTasks_fromStringOrEmpty() throws OpenemsNamedException {
		assertEquals(0, Tasks.fromStringOrEmpty(null).numberOfTasks());
		assertEquals(0, Tasks.fromStringOrEmpty("").numberOfTasks());
		assertEquals(0, Tasks.fromStringOrEmpty("  ").numberOfTasks());
		assertEquals(0, Tasks.fromStringOrEmpty("foo").numberOfTasks());

		assertEquals(new JsonArray(), Tasks.fromStringOrEmpty(null).toJson(VOID_SERIALIZER));
	}

	@Test
	public void testRecurrence_getNextOccurrence() throws OpenemsNamedException {
		{
			final var daily = RecurrenceRule.create() //
					.setFrequency(DAILY) //
					.build();
			assertDayOfWeek(daily.getNextOccurrence(EPOCH, NOW_2000), //
					"2020-01-01T00:00Z", WEDNESDAY);
			assertDayOfWeek(daily.getNextOccurrence(EPOCH, NOW_2000.plusNanos(1)), //
					"2020-01-02T00:00Z", THURSDAY);
			assertDayOfWeek(daily.getNextOccurrence(EPOCH.plusNanos(1), NOW_2000.plusNanos(1)), //
					"2020-01-01T00:00:00.000000001Z", WEDNESDAY);
		}
		{
			final var weekly = RecurrenceRule.create() //
					.setFrequency(WEEKLY) //
					.setUntil(NOW_2000.toLocalDate().plusMonths(1)) //
					.build();
			assertDayOfWeek(weekly.getNextOccurrence(EPOCH, NOW_2000), //
					"2020-01-01T00:00Z", WEDNESDAY);
			assertDayOfWeek(weekly.getNextOccurrence(EPOCH, NOW_2000.plusNanos(1)), //
					"2020-01-02T00:00Z", THURSDAY);

			// Test until
			var oneMonthLater = NOW_2000.plusMonths(1);
			assertDayOfWeek(weekly.getNextOccurrence(EPOCH, oneMonthLater), //
					"2020-02-01T00:00Z", SATURDAY);
			assertNull(weekly.getNextOccurrence(EPOCH, oneMonthLater.plusNanos(1)));

			// Test from is before taskStart
			assertDayOfWeek(weekly.getNextOccurrence(EPOCH, ZonedDateTime.of(EPOCH, NOW_2000.getZone()).minusDays(1)), //
					"1970-01-01T00:00Z", THURSDAY);
		}
		{
			final var weekly = RecurrenceRule.create() //
					.setFrequency(WEEKLY) //
					.addByDay(WEDNESDAY, THURSDAY) //
					.build();
			assertDayOfWeek(weekly.getNextOccurrence(EPOCH, NOW_2000), //
					"2020-01-01T00:00Z", WEDNESDAY);
			assertDayOfWeek(weekly.getNextOccurrence(EPOCH, NOW_2000.plusNanos(1)), //
					"2020-01-02T00:00Z", THURSDAY);
			assertDayOfWeek(weekly.getNextOccurrence(EPOCH, NOW_2000.plusDays(1)), //
					"2020-01-02T00:00Z", THURSDAY);
			assertDayOfWeek(weekly.getNextOccurrence(EPOCH, NOW_2000.plusDays(1).plusNanos(1)), //
					"2020-01-08T00:00Z", WEDNESDAY);
		}
		{
			final var yearly = RecurrenceRule.create() //
					.setFrequency(YEARLY) //
					.build();
			assertNull(yearly.getNextOccurrence(EPOCH, NOW_2000)); // not implemented
		}
	}

	@Test
	public void testRecurrence_getOccurrencesBetween() throws OpenemsNamedException {
		final var sut = RecurrenceRule.create() //
				.setFrequency(WEEKLY) //
				.addByDay(MONDAY) //
				.addByDay(WEDNESDAY) //
				.addByDay(SATURDAY) //
				.build();
		final var taskStart = EPOCH.plusHours(7).plusMinutes(15);
		{
			final var times = sut.getOccurrencesBetween(taskStart, NOW_2000, NOW_2000.plusWeeks(2));
			assertEquals(6, times.size());
			assertDayOfWeek(times, 0, "2020-01-01T07:15Z", WEDNESDAY);
			assertDayOfWeek(times, 1, "2020-01-04T07:15Z", SATURDAY);
			assertDayOfWeek(times, 2, "2020-01-06T07:15Z", MONDAY);
			assertDayOfWeek(times, 3, "2020-01-08T07:15Z", WEDNESDAY);
			assertDayOfWeek(times, 4, "2020-01-11T07:15Z", SATURDAY);
			assertDayOfWeek(times, 5, "2020-01-13T07:15Z", MONDAY);
		}

		{
			// Test no result within limits
			final var times = sut.getOccurrencesBetween(taskStart, NOW_2000, NOW_2000.plusHours(1));
			assertEquals(1, times.size());
			assertDayOfWeek(times, 0, "2020-01-01T07:15Z", WEDNESDAY);
		}
	}

	@Test
	public void testTask_getOccurrencesBetween() throws OpenemsNamedException {
		{
			final var sut = JSCalendar.Task.<JsonObject>create() //
					.setStart("07:00:00") //
					.setDuration(Duration.ofDays(2)) //
					.addRecurrenceRule(b -> b //
							.setFrequency(WEEKLY) //
							.addByDay(TUESDAY, THURSDAY)) //
					.build();
			final var times = sut.getOccurrencesBetween(NOW_2000, NOW_2000.plusMonths(1));
			assertEquals(10, times.size());
			assertDayOfWeek(times, 0, "2019-12-31T07:00Z", TUESDAY); // starts before, but duration is between
			assertDayOfWeek(times, 1, "2020-01-02T07:00Z", THURSDAY);
			assertDayOfWeek(times, 2, "2020-01-07T07:00Z", TUESDAY);
			assertDayOfWeek(times, 3, "2020-01-09T07:00Z", THURSDAY);
			assertDayOfWeek(times, 4, "2020-01-14T07:00Z", TUESDAY);
			assertDayOfWeek(times, 5, "2020-01-16T07:00Z", THURSDAY);
			assertDayOfWeek(times, 6, "2020-01-21T07:00Z", TUESDAY);
			assertDayOfWeek(times, 7, "2020-01-23T07:00Z", THURSDAY);
			assertDayOfWeek(times, 8, "2020-01-28T07:00Z", TUESDAY);
			assertDayOfWeek(times, 9, "2020-01-30T07:00Z", THURSDAY);
		}

		{
			// Test without Duration
			final var sut = JSCalendar.Task.<JsonObject>create() //
					.setStart("07:00:00") //
					.addRecurrenceRule(b -> b //
							.setFrequency(WEEKLY) //
							.addByDay(TUESDAY, THURSDAY)) //
					.build();
			final var times = sut.getOccurrencesBetween(NOW_2000, NOW_2000.plusMonths(1));
			assertEquals(9, times.size());
		}

		{
			// Test "empty" result with next occurrence
			final var sut = JSCalendar.Task.<JsonObject>create() //
					.setStart("07:00:00") //
					.addRecurrenceRule(b -> b //
							.setFrequency(WEEKLY) //
							.addByDay(TUESDAY, THURSDAY)) //
					.build();
			final var times = sut.getOccurrencesBetween(NOW_2000, NOW_2000.plusDays(1));
			assertEquals(1, times.size());
		}

		{
			// Test "empty" result with no possible next occurrence
			final var sut = JSCalendar.Task.<JsonObject>create() //
					.setStart("07:00:00") //
					.addRecurrenceRule(b -> b //
							.setFrequency(WEEKLY) //
							.setUntil(NOW_2000.toLocalDate() /* same day */) //
							.addByDay(TUESDAY, THURSDAY)) //
					.build();
			final var times = sut.getOccurrencesBetween(NOW_2000, NOW_2000.plusDays(1));
			assertEquals(0, times.size());
		}
	}

	@Test
	public void testTasks_getActiveOneTask() throws OpenemsNamedException {
		var tasks = JSCalendar.Tasks.empty();
		assertNull(tasks.getActiveOneTask());
	}

	@Test
	public void testTasks_getOneTasksBetween() throws OpenemsNamedException {
		{
			var clock = createDummyClock();
			var tasks = JSCalendar.Tasks.<StringPayload>create() //
					.setClock(clock) //
					.add(t -> t //
							.setStart("12:15") //
							.addRecurrenceRule(b -> b //
									.setFrequency(DAILY)) //
							.setPayload(new StringPayload("ONE"))) //
					.add(t -> t //
							.setStart("12:00") //
							.setDuration(Duration.ofMinutes(30)) //
							.addRecurrenceRule(b -> b //
									.setFrequency(DAILY)) //
							.setPayload(new StringPayload("TWO"))) //
					.add(t -> t //
							.setStart("07:00") //
							.setDuration(Duration.ofHours(8)) //
							.addRecurrenceRule(b -> b //
									.setFrequency(WEEKLY) //
									.addByDay(TUESDAY, THURSDAY)) //
							.setPayload(new StringPayload("THREE"))) //
					.add(t -> t //
							.setStart("00:15") //
							.setDuration(Duration.ofMinutes(15)) //
							.addRecurrenceRule(b -> b //
									.setFrequency(DAILY)) //
							.setPayload(new StringPayload("FOUR"))) //
					.build();

			assertNull(tasks.getActiveOneTask());
			clock.leap(15, MINUTES); // 2020-01-01T00:15
			assertOneTask(tasks.getActiveOneTask(), "2020-01-01T00:15Z", "2020-01-01T00:30Z", "FOUR");
			assertOneTask(tasks.getLastActiveOneTask(), "2020-01-01T00:15Z", "2020-01-01T00:30Z", "FOUR");
			clock.leap(14, MINUTES); // "2020-01-01T00:29Z
			assertOneTask(tasks.getActiveOneTask(), "2020-01-01T00:15Z", "2020-01-01T00:30Z", "FOUR");
			clock.leap(1, MINUTES); // 2020-01-01T00:30Z
			assertNull(tasks.getActiveOneTask());
			clock.leap(11 * 60 + 29, MINUTES); // 2020-01-01T11:59Z
			assertNull(tasks.getActiveOneTask());
			clock.leap(1, MINUTES); // 2020-01-01T12:00Z
			assertOneTask(tasks.getActiveOneTask(), "2020-01-01T12:00Z", "2020-01-01T12:15Z", "TWO");
			clock.leap(14, MINUTES); // 2020-01-01T12:14Z
			assertOneTask(tasks.getActiveOneTask(), "2020-01-01T12:00Z", "2020-01-01T12:15Z", "TWO");
			clock.leap(1, MINUTES); // 2020-01-01T12:15Z
			assertOneTask(tasks.getActiveOneTask(), "2020-01-01T12:15Z", "2020-01-01T12:15Z", "ONE");
			assertOneTask(tasks.getActiveOneTask(), "2020-01-01T12:15Z", "2020-01-01T12:30Z", "TWO");

			var ots = tasks.getOneTasksBetween(NOW_2000, NOW_2000.plusDays(2));
			assertFalse(ots.isEmpty());
			assertEquals(10, ots.size());
			var iterator = ots.iterator();
			assertOneTask(iterator.next(), "2020-01-01T00:15Z", "2020-01-01T00:30Z", "FOUR");
			assertOneTask(iterator.next(), "2020-01-01T12:00Z", "2020-01-01T12:15Z", "TWO");
			assertOneTask(iterator.next(), "2020-01-01T12:15Z", "2020-01-01T12:15Z", "ONE");
			assertOneTask(iterator.next(), "2020-01-01T12:15Z", "2020-01-01T12:30Z", "TWO");
			assertOneTask(iterator.next(), "2020-01-02T00:15Z", "2020-01-02T00:30Z", "FOUR");
			assertOneTask(iterator.next(), "2020-01-02T07:00Z", "2020-01-02T12:00Z", "THREE");
			assertOneTask(iterator.next(), "2020-01-02T12:00Z", "2020-01-02T12:15Z", "TWO");
			assertOneTask(iterator.next(), "2020-01-02T12:15Z", "2020-01-02T12:15Z", "ONE");
			assertOneTask(iterator.next(), "2020-01-02T12:15Z", "2020-01-02T12:30Z", "TWO");
			assertOneTask(iterator.next(), "2020-01-02T12:30Z", "2020-01-02T15:00Z", "THREE");

			// Test getPayloadAt()
			assertNull(ots.getPayloadAt(ZonedDateTime.parse("2020-01-02T00:14:59Z")));
			assertEquals("FOUR", ots.getPayloadAt(ZonedDateTime.parse("2020-01-02T00:15Z")).toString());
			assertEquals("FOUR", ots.getPayloadAt(ZonedDateTime.parse("2020-01-02T00:29:59Z")).toString());
			assertNull(ots.getPayloadAt(ZonedDateTime.parse("2020-01-02T00:30Z")));

			// Test getPayloadsBetween()
			assertEquals(0L, ots.getBetween(//
					ZonedDateTime.parse("2020-01-02T00:14Z"), //
					ZonedDateTime.parse("2020-01-02T00:14:59Z")).count());
			{
				var ps = ots.getBetween(//
						ZonedDateTime.parse("2020-01-02T00:15Z"), //
						ZonedDateTime.parse("2020-01-02T07:00:00Z")).toList();
				assertEquals(1, ps.size());
				assertOneTask(ps.get(0), "2020-01-02T00:15Z", "2020-01-02T00:30Z", "FOUR");
			}
			{
				var ps = ots.getBetween(//
						ZonedDateTime.parse("2020-01-02T00:15Z"), //
						ZonedDateTime.parse("2020-01-02T07:00:01Z")).toList();
				assertEquals(2, ps.size());
				assertOneTask(ps.get(0), "2020-01-02T00:15Z", "2020-01-02T00:30Z", "FOUR");
				assertOneTask(ps.get(1), "2020-01-02T07:00Z", "2020-01-02T12:00Z", "THREE");
			}

			// Serializer
			var serializer = OneTasks.serializer(StringPayload.serializer());
			var json = serializer.serialize(ots);
			assertEquals(json, serializer.serialize(serializer.deserialize(json)));
		}
		{
			var clock = createDummyClock();
			var tasks = JSCalendar.Tasks.<StringPayload>create() //
					.setClock(clock) //
					.add(t -> t //
							.setStart("11:00") //
							.setDuration(Duration.ofHours(2)) //
							.addRecurrenceRule(b -> b //
									.setFrequency(WEEKLY) //
									.addByDay(WEDNESDAY)) //
							.setPayload(new StringPayload("ONE"))) //
					.add(t -> t //
							.setStart("12:00") //
							.setDuration(Duration.ofHours(30)) //
							.addRecurrenceRule(b -> b //
									.setFrequency(DAILY)) //
							.setPayload(new StringPayload("TWO"))) //
					.build();

			var ots = tasks.getOneTasksBetween(NOW_2000, NOW_2000.plusDays(2));
			assertEquals(3, ots.size());
			var iterator = ots.iterator();
			assertOneTask(iterator.next(), "2020-01-01T00:00Z", "2020-01-01T11:00Z", "TWO");
			assertOneTask(iterator.next(), "2020-01-01T11:00Z", "2020-01-01T13:00Z", "ONE");
			assertOneTask(iterator.next(), "2020-01-01T13:00Z", "2020-01-03T00:00Z", "TWO");
		}
		{
			var clock = createDummyClock();
			var tasks = JSCalendar.Tasks.<StringPayload>create() //
					.setClock(clock) //
					.add(t -> t //
							.setStart("11:00") //
							.setDuration(Duration.ofHours(2)) //
							.addRecurrenceRule(b -> b //
									.setFrequency(DAILY) //
									.setUntil(LocalDate.of(2020, 1, 2))) //
							.setPayload(new StringPayload("ONE"))) //
					.add(t -> t //
							.setStart(LocalDateTime.of(2020, 1, 2, 4, 0)) //
							.setPayload(new StringPayload("TWO"))) //
					.build();

			// 00:00
			assertNull(tasks.getActiveOneTask());
			// 10:59
			clock.leap(10 * 60 + 59, MINUTES);
			assertNull(tasks.getActiveOneTask());
			// 11:00
			clock.leap(1, MINUTES);
			assertOneTask(tasks.getActiveOneTask(), "2020-01-01T11:00Z", "2020-01-01T13:00Z", "ONE");
			// 12:59
			clock.leap(60 + 59, MINUTES);
			assertOneTask(tasks.getActiveOneTask(), "2020-01-01T11:00Z", "2020-01-01T13:00Z", "ONE");
			// 13:00
			clock.leap(1, MINUTES);
			assertNull(tasks.getActiveOneTask());
			// 12:59
			clock.leap(21 * 60 + 59, MINUTES);
			assertNull(tasks.getActiveOneTask());
			// 13:00
			clock.leap(1, MINUTES);
			assertOneTask(tasks.getActiveOneTask(), "2020-01-02T11:00Z", "2020-01-02T13:00Z", "ONE");
			// 13:00
			clock.leap(2 * 60, MINUTES);
			assertNull(tasks.getActiveOneTask());
			assertNull(tasks.getActiveOneTask()); // Task 2 sollte kommen
		}
	}

	@Test
	public void testTasks_getOneTasksBetween_Monthly() throws OpenemsNamedException {
		var clock = createDummyClock();
		{
			var tasks = JSCalendar.Tasks.<Void>create() //
					.setClock(clock) //
					.add(t -> t //
							.setStart("20:00") //
							.addRecurrenceRule(b -> b //
									.setFrequency(MONTHLY))) //
					.build();

			var ots = tasks.getOneTasksBetween(NOW_2000, NOW_2000.plusMonths(13));
			assertTrue(ots.isEmpty());
		}
		{
			// Every first Sunday of the month at 8pm
			var tasks = JSCalendar.Tasks.<StringPayload>create() //
					.setClock(clock) //
					.add(t -> t //
							.setStart("20:00") //
							.addRecurrenceRule(b -> b //
									.setFrequency(MONTHLY) //
									.addByDay(SUNDAY)) //
							.setPayload(new StringPayload("ONE"))) //
					.build();

			var ots = tasks.getOneTasksBetween(NOW_2000, NOW_2000.plusMonths(13));
			assertEquals(13, ots.size());
			var iterator = ots.iterator();
			assertOneTask(iterator.next(), SUNDAY, "2020-01-05T20:00Z", "ONE");
			assertOneTask(iterator.next(), SUNDAY, "2020-02-02T20:00Z", "ONE");
			assertOneTask(iterator.next(), SUNDAY, "2020-03-01T20:00Z", "ONE");
			assertOneTask(iterator.next(), SUNDAY, "2020-04-05T20:00Z", "ONE");
			assertOneTask(iterator.next(), SUNDAY, "2020-05-03T20:00Z", "ONE");
			assertOneTask(iterator.next(), SUNDAY, "2020-06-07T20:00Z", "ONE");
			assertOneTask(iterator.next(), SUNDAY, "2020-07-05T20:00Z", "ONE");
			assertOneTask(iterator.next(), SUNDAY, "2020-08-02T20:00Z", "ONE");
			assertOneTask(iterator.next(), SUNDAY, "2020-09-06T20:00Z", "ONE");
			assertOneTask(iterator.next(), SUNDAY, "2020-10-04T20:00Z", "ONE");
			assertOneTask(iterator.next(), SUNDAY, "2020-11-01T20:00Z", "ONE");
			assertOneTask(iterator.next(), SUNDAY, "2020-12-06T20:00Z", "ONE");
			assertOneTask(iterator.next(), SUNDAY, "2021-01-03T20:00Z", "ONE");
		}
		{
			// ONE on every first Sunday and Tuesday of the month
			// TWO on every first Thursday
			var tasks = JSCalendar.Tasks.<StringPayload>create() //
					.setClock(clock) //
					.add(t -> t //
							.setStart("20:00") //
							.addRecurrenceRule(b -> b //
									.setFrequency(MONTHLY) //
									.addByDay(SUNDAY, TUESDAY)) //
							.setPayload(new StringPayload("ONE"))) //
					.add(t -> t //
							.setStart("20:00") //
							.addRecurrenceRule(b -> b //
									.setFrequency(MONTHLY) //
									.addByDay(THURSDAY)) //
							.setPayload(new StringPayload("TWO"))) //
					.build();

			var ots = tasks.getOneTasksBetween(NOW_2000, NOW_2000.plusMonths(2));
			assertEquals(6, ots.size());
			var iterator = ots.iterator();
			assertOneTask(iterator.next(), THURSDAY, "2020-01-02T20:00Z", "TWO");
			assertOneTask(iterator.next(), SUNDAY, "2020-01-05T20:00Z", "ONE");
			assertOneTask(iterator.next(), TUESDAY, "2020-01-07T20:00Z", "ONE");
			assertOneTask(iterator.next(), SUNDAY, "2020-02-02T20:00Z", "ONE");
			assertOneTask(iterator.next(), TUESDAY, "2020-02-04T20:00Z", "ONE");
			assertOneTask(iterator.next(), THURSDAY, "2020-02-06T20:00Z", "TWO");
		}
	}

	@Test
	public void testTasks_getOneTasks_monthly_on_nthOfPeriod() {
		var clock = createDummyClock();
		{
			// Every third Sunday of the month at 8pm
			var tasks = JSCalendar.Tasks.<StringPayload>create() //
					.setClock(clock) //
					.add(t -> t //
							.setStart("20:00") //
							.addRecurrenceRule(b -> b //
									.setFrequency(MONTHLY) //
									.addByDay(new JSCalendar.NDay(SUNDAY, 3))) //
							.setPayload(new StringPayload("ONE"))) //
					.build();

			var ots = tasks.getOneTasksBetween(NOW_2000, NOW_2000.plusMonths(13));
			assertEquals(13, ots.size());
			var iterator = ots.iterator();
			assertOneTask(iterator.next(), SUNDAY, "2020-01-19T20:00Z", "ONE");
			assertOneTask(iterator.next(), SUNDAY, "2020-02-16T20:00Z", "ONE");
			assertOneTask(iterator.next(), SUNDAY, "2020-03-15T20:00Z", "ONE");
			assertOneTask(iterator.next(), SUNDAY, "2020-04-19T20:00Z", "ONE");
			assertOneTask(iterator.next(), SUNDAY, "2020-05-17T20:00Z", "ONE");
			assertOneTask(iterator.next(), SUNDAY, "2020-06-21T20:00Z", "ONE");
			assertOneTask(iterator.next(), SUNDAY, "2020-07-19T20:00Z", "ONE");
			assertOneTask(iterator.next(), SUNDAY, "2020-08-16T20:00Z", "ONE");
			assertOneTask(iterator.next(), SUNDAY, "2020-09-20T20:00Z", "ONE");
			assertOneTask(iterator.next(), SUNDAY, "2020-10-18T20:00Z", "ONE");
			assertOneTask(iterator.next(), SUNDAY, "2020-11-15T20:00Z", "ONE");
			assertOneTask(iterator.next(), SUNDAY, "2020-12-20T20:00Z", "ONE");
			assertOneTask(iterator.next(), SUNDAY, "2021-01-17T20:00Z", "ONE");
		}
		{
			// Every second Sunday of the month at 10am
			var tasks = JSCalendar.Tasks.<StringPayload>create() //
					.setClock(clock) //
					.add(t -> t //
							.setStart("10:00") //
							.addRecurrenceRule(b -> b //
									.setFrequency(MONTHLY) //
									.addByDay(new JSCalendar.NDay(SUNDAY, 2))) //
							.setPayload(new StringPayload("ONE"))) //
					.build();

			var ots = tasks.getOneTasksBetween(NOW_2000, NOW_2000.plusMonths(13));
			assertEquals(13, ots.size());
			var iterator = ots.iterator();
			assertOneTask(iterator.next(), SUNDAY, "2020-01-12T10:00Z", "ONE");
			assertOneTask(iterator.next(), SUNDAY, "2020-02-09T10:00Z", "ONE");
			assertOneTask(iterator.next(), SUNDAY, "2020-03-08T10:00Z", "ONE");
			assertOneTask(iterator.next(), SUNDAY, "2020-04-12T10:00Z", "ONE");
			assertOneTask(iterator.next(), SUNDAY, "2020-05-10T10:00Z", "ONE");
			assertOneTask(iterator.next(), SUNDAY, "2020-06-14T10:00Z", "ONE");
			assertOneTask(iterator.next(), SUNDAY, "2020-07-12T10:00Z", "ONE");
			assertOneTask(iterator.next(), SUNDAY, "2020-08-09T10:00Z", "ONE");
			assertOneTask(iterator.next(), SUNDAY, "2020-09-13T10:00Z", "ONE");
			assertOneTask(iterator.next(), SUNDAY, "2020-10-11T10:00Z", "ONE");
			assertOneTask(iterator.next(), SUNDAY, "2020-11-08T10:00Z", "ONE");
			assertOneTask(iterator.next(), SUNDAY, "2020-12-13T10:00Z", "ONE");
			assertOneTask(iterator.next(), SUNDAY, "2021-01-10T10:00Z", "ONE");
		}
		{
			// Test various scenarios
			// Second sunday at 10am, first tuesday at 3 and 6pm
			// and first monday at 12am
			var tasks = JSCalendar.Tasks.<StringPayload>create() //
					.setClock(clock) //
					.add(t -> t //
							.setStart("10:00") //
							.addRecurrenceRule(b -> b //
									.setFrequency(MONTHLY) //
									.addByDay(new JSCalendar.NDay(SUNDAY, 2))) //
							.setPayload(new StringPayload("ONE"))) //
					.add(t -> t //
							.setStart("15:00") //
							.addRecurrenceRule(b -> b //
									.setFrequency(MONTHLY) //
									.addByDay(new JSCalendar.NDay(TUESDAY, 1))) //
							.setPayload(new StringPayload("TWO"))) //
					.add(t -> t //
							.setStart("18:00") //
							.addRecurrenceRule(b -> b //
									.setFrequency(MONTHLY) //
									.addByDay(new JSCalendar.NDay(TUESDAY, 1))) //
							.setPayload(new StringPayload("THREE"))) //
					.add(t -> t //
							.setStart("12:00") //
							.addRecurrenceRule(b -> b //
									.setFrequency(MONTHLY) //
									.addByDay(new JSCalendar.NDay(MONDAY, 1))) //
							.setPayload(new StringPayload("FOUR"))) //
					.build();

			var ots = tasks.getOneTasksBetween(NOW_2000, NOW_2000.plusMonths(13));
			assertEquals(52, ots.size());
			var iterator = ots.iterator();
			assertOneTask(iterator.next(), MONDAY, "2020-01-06T12:00Z", "FOUR");
			assertOneTask(iterator.next(), TUESDAY, "2020-01-07T15:00Z", "TWO");
			assertOneTask(iterator.next(), TUESDAY, "2020-01-07T18:00Z", "THREE");
			assertOneTask(iterator.next(), SUNDAY, "2020-01-12T10:00Z", "ONE");

			assertOneTask(iterator.next(), MONDAY, "2020-02-03T12:00Z", "FOUR");
			assertOneTask(iterator.next(), TUESDAY, "2020-02-04T15:00Z", "TWO");
			assertOneTask(iterator.next(), TUESDAY, "2020-02-04T18:00Z", "THREE");
			assertOneTask(iterator.next(), SUNDAY, "2020-02-09T10:00Z", "ONE");

			assertOneTask(iterator.next(), MONDAY, "2020-03-02T12:00Z", "FOUR");
			assertOneTask(iterator.next(), TUESDAY, "2020-03-03T15:00Z", "TWO");
			assertOneTask(iterator.next(), TUESDAY, "2020-03-03T18:00Z", "THREE");
			assertOneTask(iterator.next(), SUNDAY, "2020-03-08T10:00Z", "ONE");

			assertOneTask(iterator.next(), MONDAY, "2020-04-06T12:00Z", "FOUR");
			assertOneTask(iterator.next(), TUESDAY, "2020-04-07T15:00Z", "TWO");
			assertOneTask(iterator.next(), TUESDAY, "2020-04-07T18:00Z", "THREE");
			assertOneTask(iterator.next(), SUNDAY, "2020-04-12T10:00Z", "ONE");

			assertOneTask(iterator.next(), MONDAY, "2020-05-04T12:00Z", "FOUR");
			assertOneTask(iterator.next(), TUESDAY, "2020-05-05T15:00Z", "TWO");
			assertOneTask(iterator.next(), TUESDAY, "2020-05-05T18:00Z", "THREE");
			assertOneTask(iterator.next(), SUNDAY, "2020-05-10T10:00Z", "ONE");

			assertOneTask(iterator.next(), MONDAY, "2020-06-01T12:00Z", "FOUR");
			assertOneTask(iterator.next(), TUESDAY, "2020-06-02T15:00Z", "TWO");
			assertOneTask(iterator.next(), TUESDAY, "2020-06-02T18:00Z", "THREE");
			assertOneTask(iterator.next(), SUNDAY, "2020-06-14T10:00Z", "ONE");

			assertOneTask(iterator.next(), MONDAY, "2020-07-06T12:00Z", "FOUR");
			assertOneTask(iterator.next(), TUESDAY, "2020-07-07T15:00Z", "TWO");
			assertOneTask(iterator.next(), TUESDAY, "2020-07-07T18:00Z", "THREE");
			assertOneTask(iterator.next(), SUNDAY, "2020-07-12T10:00Z", "ONE");

			assertOneTask(iterator.next(), MONDAY, "2020-08-03T12:00Z", "FOUR");
			assertOneTask(iterator.next(), TUESDAY, "2020-08-04T15:00Z", "TWO");
			assertOneTask(iterator.next(), TUESDAY, "2020-08-04T18:00Z", "THREE");
			assertOneTask(iterator.next(), SUNDAY, "2020-08-09T10:00Z", "ONE");

			assertOneTask(iterator.next(), TUESDAY, "2020-09-01T15:00Z", "TWO");
			assertOneTask(iterator.next(), TUESDAY, "2020-09-01T18:00Z", "THREE");
			assertOneTask(iterator.next(), MONDAY, "2020-09-07T12:00Z", "FOUR");
			assertOneTask(iterator.next(), SUNDAY, "2020-09-13T10:00Z", "ONE");

			assertOneTask(iterator.next(), MONDAY, "2020-10-05T12:00Z", "FOUR");
			assertOneTask(iterator.next(), TUESDAY, "2020-10-06T15:00Z", "TWO");
			assertOneTask(iterator.next(), TUESDAY, "2020-10-06T18:00Z", "THREE");
			assertOneTask(iterator.next(), SUNDAY, "2020-10-11T10:00Z", "ONE");

			assertOneTask(iterator.next(), MONDAY, "2020-11-02T12:00Z", "FOUR");
			assertOneTask(iterator.next(), TUESDAY, "2020-11-03T15:00Z", "TWO");
			assertOneTask(iterator.next(), TUESDAY, "2020-11-03T18:00Z", "THREE");
			assertOneTask(iterator.next(), SUNDAY, "2020-11-08T10:00Z", "ONE");

			// month starts on a tuesday -> first monday is on 7th
			assertOneTask(iterator.next(), TUESDAY, "2020-12-01T15:00Z", "TWO");
			assertOneTask(iterator.next(), TUESDAY, "2020-12-01T18:00Z", "THREE");
			assertOneTask(iterator.next(), MONDAY, "2020-12-07T12:00Z", "FOUR");
			assertOneTask(iterator.next(), SUNDAY, "2020-12-13T10:00Z", "ONE");

			assertOneTask(iterator.next(), MONDAY, "2021-01-04T12:00Z", "FOUR");
			assertOneTask(iterator.next(), TUESDAY, "2021-01-05T15:00Z", "TWO");
			assertOneTask(iterator.next(), TUESDAY, "2021-01-05T18:00Z", "THREE");
			assertOneTask(iterator.next(), SUNDAY, "2021-01-10T10:00Z", "ONE");
		}
	}

	@Test
	public void testParseSingleTask() throws OpenemsNamedException {
		var sut = JSCalendar.Tasks.fromStringOrEmpty("""
				[
				   {
				      "@type":"Task",
				      "start":"2025-06-18T15:00:00",
				      "duration":"PT12H"
				   }
				]""");
		assertEquals(1, sut.numberOfTasks());

		assertEquals("PT12H", JsonUtils.getAsString(sut.toJson(VOID_SERIALIZER).get(0), "duration"));
	}

	@Test
	public void testDailyParse() throws OpenemsNamedException {
		var sut = JSCalendar.Tasks.fromStringOrEmpty("""
				[
				   {
				      "@type":"Task",
				      "start":"19:00:00",
				      "duration":"PT12H",
				      "recurrenceRules":[
				         {
				            "frequency":"daily"
				         }
				      ]
				   }
				]""");
		assertEquals(1, sut.numberOfTasks());
	}

	@Test
	public void testMonthlyParse() throws OpenemsNamedException {
		// Every first sunday of the month at 8pm
		// TODO Implement "interval" to support "every third month" etc.
		var sut = JSCalendar.Tasks.fromStringOrEmpty("""
				[
				   {
				      "@type":"Task",
				      "start":"2026-01-01T20:00:00",
				      "recurrenceRules":[
				         {
				            "frequency":"monthly",
				            "interval": 1,
				            "byDay": [
				                {"day":"mo", "nthOfPeriod": 2},
				    			{"day":"su", "nthOfPeriod": 1}
				  			]
				         }
				      ]
				   }
				]""");
		assertEquals(1, sut.numberOfTasks());
	}

	@Test
	public void testJsonDeserializeAndSerialize() throws OpenemsNamedException {
		var sut = JSCalendar.Tasks.fromStringOrEmpty("""
				[
				   {
				      "@type":"Task",
				      "start":"2026-01-01T20:00:00",
				      "recurrenceRules":[
				         { "frequency":"monthly",
				         "byDay":[
				         {"day":"mo","nthOfPeriod":2}
				         ]
				         }
				      ]
				   },
				   {
				      "@type":"Task",
				      "start":"2026-01-01T20:00:00",
				      "recurrenceRules":[
				         { "frequency":"weekly",
				         "byDay":["mo"]
				         }
				      ]
				   }
				]""");

		var json = sut.toJson(VOID_SERIALIZER);

		// Monthly
		var monthlyElement = json.get(0).getAsJsonObject().getAsJsonArray("recurrenceRules").get(0).getAsJsonObject()
				.getAsJsonArray("byDay").get(0);

		var monthlyPath = new JsonElementPathActual.JsonElementPathActualNonNull(monthlyElement);
		var nDayMonthly = RecurrenceRule.deserializeByDayElement(monthlyPath);
		var serializedMonthly = RecurrenceRule.nDaySerializer().serialize(nDayMonthly).toString();

		// Weekly
		var weeklyElement = json.get(1).getAsJsonObject().getAsJsonArray("recurrenceRules").get(0).getAsJsonObject()
				.getAsJsonArray("byDay").get(0);

		var weeklyPath = new JsonElementPathActual.JsonElementPathActualNonNull(weeklyElement);
		var nDayWeekly = RecurrenceRule.deserializeByDayElement(weeklyPath);
		var serializedWeekly = RecurrenceRule.nDaySerializer().serialize(nDayWeekly).toString();

		assertEquals("{\"day\":\"mo\",\"nthOfPeriod\":2}", serializedMonthly);
		assertEquals("\"mo\"", serializedWeekly);
	}

	@Test
	public void testFallbackParse() throws OpenemsNamedException {
		var sut = JSCalendar.Tasks.fromStringOrEmpty("""
				[
				   {
				      "@type":"Task",
				      "start":"08:00:00",
				      "duration":"PT12H",
				      "recurrenceRules":[
				         {
				            "frequency":"daily"
				         }
				      ],
				      "openems.io:payload":{
				         "value": "BAR"
				      }
				   },
				   {
				      "@type":"Task",
				      "openems.io:payload":{
				         "value": "FOO"
				      }
				   }
				]""", StringPayload.serializer());
		assertEquals(2, sut.numberOfTasks());
	}

	@Test
	public void testSingle() throws OpenemsNamedException {
		var sut = JSCalendar.Task.<StringPayload>create() //
				.setUid(getNilUuid()) //
				.setStart("2024-06-17T00:00:00") //
				.setPayload(new StringPayload("Hello World")) //
				.build();
		var json = TASK_SERIALIZER.serialize(sut);
		assertEquals("""
				{
				  "@type": "Task",
				  "uid": "00000000-0000-0000-0000-000000000000",
				  "start": "2024-06-17T00:00:00",
				  "duration": "PT0S",
				  "openems.io:payload": {
				    "value": "Hello World"
				  }
				}""", prettyToString(json));
	}

	@Test
	public void testWeekday() throws OpenemsNamedException {
		var clock = createDummyClock();
		var sut = JSCalendar.Task.<StringPayload>create() //
				.setUid(getNilUuid()) //
				.setStart("07:00:00") //
				.addRecurrenceRule(b -> b //
						.setFrequency(WEEKLY) //
						.addByDay(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)) //
				.setPayload(new StringPayload("Hello World")) //
				.build();
		var json = TASK_SERIALIZER.serialize(sut);
		assertEquals("""
				{
				  "@type": "Task",
				  "uid": "00000000-0000-0000-0000-000000000000",
				  "start": "07:00:00",
				  "duration": "PT0S",
				  "recurrenceRules": [
				    {
				      "frequency": "weekly",
				      "byDay": [
				        "mo",
				        "tu",
				        "we",
				        "th",
				        "fr"
				      ]
				    }
				  ],
				  "openems.io:payload": {
				    "value": "Hello World"
				  }
				}""", prettyToString(json));

		var now = ZonedDateTime.now(clock);
		var times = sut.getOccurrencesBetween(now, now.plusWeeks(1));
		assertEquals(5, times.size());
		assertDayOfWeek(times, 0, "2020-01-01T07:00Z", WEDNESDAY);
		assertDayOfWeek(times, 1, "2020-01-02T07:00Z", THURSDAY);
		assertDayOfWeek(times, 2, "2020-01-03T07:00Z", FRIDAY);
		assertDayOfWeek(times, 3, "2020-01-06T07:00Z", MONDAY);
		assertDayOfWeek(times, 4, "2020-01-07T07:00Z", TUESDAY);

		// Parse JSON
		assertEquals(sut, TASK_SERIALIZER.deserialize(json));
	}

	@Test
	public void testWeekend() throws OpenemsNamedException {
		var clock = createDummyClock();
		var sut = JSCalendar.Task.<StringPayload>create() //
				.setStart("2020-01-02T00:00:00") //
				.setUid(getNilUuid()) //
				.addRecurrenceRule(b -> b //
						.setFrequency(WEEKLY) //
						.setUntil(LocalDate.of(2020, 2, 1)) //
						.addByDay(SATURDAY, SUNDAY)) //
				.setPayload(new StringPayload("Hello World")) //
				.build();
		var json = TASK_SERIALIZER.serialize(sut);
		assertEquals("""
				{
				  "@type": "Task",
				  "uid": "00000000-0000-0000-0000-000000000000",
				  "start": "2020-01-02T00:00:00",
				  "duration": "PT0S",
				  "recurrenceRules": [
				    {
				      "frequency": "weekly",
				      "until": "2020-02-01",
				      "byDay": [
				        "sa",
				        "su"
				      ]
				    }
				  ],
				  "openems.io:payload": {
				    "value": "Hello World"
				  }
				}""", prettyToString(json));

		var now = ZonedDateTime.now(clock);
		var times = sut.getOccurrencesBetween(now, now.plusMonths(2));
		assertEquals(9, times.size());
		assertDayOfWeek(times, 0, "2020-01-04T00:00Z", SATURDAY);
		assertDayOfWeek(times, 1, "2020-01-05T00:00Z", SUNDAY);
		assertDayOfWeek(times, 2, "2020-01-11T00:00Z", SATURDAY);
		assertDayOfWeek(times, 3, "2020-01-12T00:00Z", SUNDAY);
		assertDayOfWeek(times, 4, "2020-01-18T00:00Z", SATURDAY);
		assertDayOfWeek(times, 5, "2020-01-19T00:00Z", SUNDAY);
		assertDayOfWeek(times, 6, "2020-01-25T00:00Z", SATURDAY);
		assertDayOfWeek(times, 7, "2020-01-26T00:00Z", SUNDAY);
		assertDayOfWeek(times, 8, "2020-02-01T00:00Z", SATURDAY);

		// Parse JSON
		assertEquals(sut, TASK_SERIALIZER.deserialize(json));
	}

	@Test
	public void testTasks_getOneTasksBetween2() throws OpenemsNamedException {
		var tasks = JSCalendar.Tasks.<JsonObject>create() //
				.add(t -> t //
						.setStart("1970-01-01T07:30:00") //
						.addRecurrenceRule(b -> b //
								.setFrequency(WEEKLY) //
								.addByDay(TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)) //
						.setPayload(buildJsonObject() //
								.addProperty("sessionEnergyMinimum", 10000) //
								.build())) //
				.add(t -> t //
						.setStart("1970-01-01T07:30:00") //
						.addRecurrenceRule(b -> b //
								.setFrequency(WEEKLY) //
								.addByDay(MONDAY)) //
						.setPayload(buildJsonObject() //
								.addProperty("sessionEnergyMinimum", 60000) //
								.build())) //
				.build();

		var firstTime = ZonedDateTime.parse("2025-03-23T22:45+01:00[Europe/Berlin]");
		var lastTime = ZonedDateTime.parse("2025-04-01T23:00+01:00[Europe/Berlin]");
		var ots = tasks.getOneTasksBetween(firstTime, lastTime);

		assertEquals(7, ots.size());
		var iterator = ots.iterator();
		var firstOt = iterator.next();
		assertOneTask(firstOt, //
				"2025-03-24T07:30+01:00[Europe/Berlin]", "2025-03-24T07:30+01:00[Europe/Berlin]",
				"{\"sessionEnergyMinimum\":60000}");
		assertOneTask(iterator.next(), //
				"2025-03-25T07:30+01:00[Europe/Berlin]", "2025-03-25T07:30+01:00[Europe/Berlin]",
				"{\"sessionEnergyMinimum\":10000}");
		assertOneTask(iterator.next(), //
				"2025-03-26T07:30+01:00[Europe/Berlin]", "2025-03-26T07:30+01:00[Europe/Berlin]",
				"{\"sessionEnergyMinimum\":10000}");
		assertOneTask(iterator.next(), //
				"2025-03-27T07:30+01:00[Europe/Berlin]", "2025-03-27T07:30+01:00[Europe/Berlin]",
				"{\"sessionEnergyMinimum\":10000}");
		assertOneTask(iterator.next(), //
				"2025-03-28T07:30+01:00[Europe/Berlin]", "2025-03-28T07:30+01:00[Europe/Berlin]",
				"{\"sessionEnergyMinimum\":10000}");
		assertOneTask(iterator.next(), //
				"2025-03-31T07:30+02:00[Europe/Berlin]", "2025-03-31T07:30+02:00[Europe/Berlin]",
				"{\"sessionEnergyMinimum\":60000}");
		assertOneTask(iterator.next(), //
				"2025-04-01T07:30+02:00[Europe/Berlin]", "2025-04-01T07:30+02:00[Europe/Berlin]",
				"{\"sessionEnergyMinimum\":10000}");

		assertEquals(
				"OneTask{start=2025-03-24T07:30+01:00[Europe/Berlin], end=2025-03-24T07:30+01:00[Europe/Berlin], duration=PT0S, payload={\"sessionEnergyMinimum\":60000}}",
				firstOt.toString());
	}

	@Test
	public void testTasks_withXTask() throws OpenemsNamedException {
		final var clock = createDummyClock();
		final var uid0 = randomUUID();
		final var tasks0 = JSCalendar.Tasks.<StringPayload>create() //
				.setClock(clock) //
				.add(t -> t //
						.setUid(uid0) //
						.setPayload(new StringPayload("FOO"))) //
				.build();
		assertEquals(1, tasks0.tasks.size());

		// Add
		clock.leap(1, MINUTES);
		final var tasks1 = tasks0.withAddedTask(JSCalendar.Task.<StringPayload>create()//
				.setPayload(new StringPayload("BAR")) //
				.build());
		assertEquals(2, tasks1.tasks.size());
		assertEquals("FOO", tasks1.tasks.get(0).payload().value);
		final var addedTask = tasks1.tasks.get(1);
		assertEquals("BAR", addedTask.payload().value);
		assertEquals("2020-01-01T00:01Z", addedTask.updated().toString());

		// Update
		assertThrowsExactly(IllegalArgumentException.class,
				() -> tasks1.withUpdatedTask(JSCalendar.Task.<StringPayload>create()//
						.setUid(randomUUID()) // New UID
						.setPayload(new StringPayload("BAR")) //
						.build()));
		final var tasks2 = tasks1.withUpdatedTask(JSCalendar.Task.<StringPayload>create()//
				.setUid(uid0) // New UID
				.setPayload(new StringPayload("FOO2")) //
				.build());
		assertEquals(2, tasks2.tasks.size());
		assertEquals("FOO", tasks1.tasks.get(0).payload().value);
		assertEquals("FOO2", tasks2.tasks.get(0).payload().value);

		// Remove
		assertThrowsExactly(IllegalArgumentException.class, //
				() -> tasks1.withRemovedTask(randomUUID())); // New UID
		final var tasks3 = tasks2.withRemovedTask(uid0);
		assertEquals(1, tasks3.tasks.size());
		assertEquals("BAR", tasks3.tasks.get(0).payload().value);
	}

	@Test
	public void testDailyWithUntil() throws OpenemsNamedException {
		var clock = createDummyClock();
		var sut = JSCalendar.Task.<JsonObject>create()//
				.setStart("07:00:00") //
				.addRecurrenceRule(b -> b.setFrequency(DAILY) //
						.setUntil(LocalDate.of(2020, 1, 3)))
				.build();

		var now = ZonedDateTime.now(clock);
		var times = sut.getOccurrencesBetween(now, now.plusWeeks(1));
		assertEquals(3, times.size());
		assertDayOfWeek(times, 0, "2020-01-01T07:00Z", WEDNESDAY);
		assertDayOfWeek(times, 1, "2020-01-02T07:00Z", THURSDAY);
		assertDayOfWeek(times, 2, "2020-01-03T07:00Z", FRIDAY);

		// Should not find any further occurrence after 'until'
		times = sut.getOccurrencesBetween(now.plusWeeks(1), now.plusWeeks(2));
		assertEquals(0, times.size());
	}

	@Test
	public void testUntilDateBeforeStart() throws OpenemsNamedException {
		var clock = createDummyClock();
		var sut = JSCalendar.Task.<JsonObject>create() //
				.setStart("07:00:00") //
				.addRecurrenceRule(b -> b //
						.setFrequency(DAILY) //
						.setUntil(LocalDate.of(2019, 12, 31))) // Before 2020-01-01
				.build();

		// Start is after until => no occurrence at all
		var now = ZonedDateTime.now(clock);
		var times = sut.getOccurrencesBetween(now, now.plusWeeks(1));
		assertEquals(0, times.size());
	}

	private static record StringPayload(String value) {

		@Override
		public final String toString() {
			return this.value;
		}

		/**
		 * Returns a {@link JsonSerializer} for a {@link StringPayload}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<StringPayload> serializer() {
			return jsonObjectSerializer(StringPayload.class, json -> {
				return new StringPayload(//
						json.getString("value") //
				);
			}, obj -> {
				return buildJsonObject() //
						.addProperty("value", obj.value) //
						.build();
			});
		}
	}
}
