package io.openems.common.jscalendar;

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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jscalendar.JSCalendar.RecurrenceRule;
import io.openems.common.jscalendar.JSCalendar.Tasks;
import io.openems.common.jscalendar.JSCalendar.Tasks.OneTask;
import io.openems.common.jsonrpc.serialization.JsonSerializer;

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

	@Test
	public void testTasks_fromStringOrEmpty() throws OpenemsNamedException {
		assertEquals(0, Tasks.fromStringOrEmpty(null).numberOfTasks());
		assertEquals(0, Tasks.fromStringOrEmpty("").numberOfTasks());
		assertEquals(0, Tasks.fromStringOrEmpty("  ").numberOfTasks());
		assertEquals(0, Tasks.fromStringOrEmpty("foo").numberOfTasks());
	}

	@Test
	public void testRecurrence_getNextOccurence() throws OpenemsNamedException {
		{
			final var daily = RecurrenceRule.create() //
					.setFrequency(DAILY) //
					.build();
			assertDayOfWeek(daily.getNextOccurence(EPOCH, NOW_2000), //
					"2020-01-01T00:00Z", WEDNESDAY);
			assertDayOfWeek(daily.getNextOccurence(EPOCH, NOW_2000.plusNanos(1)), //
					"2020-01-02T00:00Z", THURSDAY);
			assertDayOfWeek(daily.getNextOccurence(EPOCH.plusNanos(1), NOW_2000.plusNanos(1)), //
					"2020-01-01T00:00:00.000000001Z", WEDNESDAY);
		}
		{
			final var weekly = RecurrenceRule.create() //
					.setFrequency(WEEKLY) //
					.setUntil(NOW_2000.toLocalDate().plusMonths(1)) //
					.build();
			assertDayOfWeek(weekly.getNextOccurence(EPOCH, NOW_2000), //
					"2020-01-01T00:00Z", WEDNESDAY);
			assertDayOfWeek(weekly.getNextOccurence(EPOCH, NOW_2000.plusNanos(1)), //
					"2020-01-02T00:00Z", THURSDAY);

			// Test until
			var oneMonthLater = NOW_2000.plusMonths(1);
			assertDayOfWeek(weekly.getNextOccurence(EPOCH, oneMonthLater), //
					"2020-02-01T00:00Z", SATURDAY);
			assertNull(weekly.getNextOccurence(EPOCH, oneMonthLater.plusNanos(1)));

			// Test from is before taskStart
			assertDayOfWeek(weekly.getNextOccurence(EPOCH, ZonedDateTime.of(EPOCH, NOW_2000.getZone()).minusDays(1)), //
					"1970-01-01T00:00Z", THURSDAY);
		}
		{
			final var weekly = RecurrenceRule.create() //
					.setFrequency(WEEKLY) //
					.addByDay(WEDNESDAY, THURSDAY) //
					.build();
			assertDayOfWeek(weekly.getNextOccurence(EPOCH, NOW_2000), //
					"2020-01-01T00:00Z", WEDNESDAY);
			assertDayOfWeek(weekly.getNextOccurence(EPOCH, NOW_2000.plusNanos(1)), //
					"2020-01-02T00:00Z", THURSDAY);
			assertDayOfWeek(weekly.getNextOccurence(EPOCH, NOW_2000.plusDays(1)), //
					"2020-01-02T00:00Z", THURSDAY);
			assertDayOfWeek(weekly.getNextOccurence(EPOCH, NOW_2000.plusDays(1).plusNanos(1)), //
					"2020-01-08T00:00Z", WEDNESDAY);
		}
		{
			final var monthly = RecurrenceRule.create() //
					.setFrequency(MONTHLY) //
					.build();
			assertNull(monthly.getNextOccurence(EPOCH, NOW_2000)); // not implemented
		}
		{
			final var yearly = RecurrenceRule.create() //
					.setFrequency(YEARLY) //
					.build();
			assertNull(yearly.getNextOccurence(EPOCH, NOW_2000)); // not implemented
		}
	}

	@Test
	public void testRecurrence_getOccurencesBetween() throws OpenemsNamedException {
		final var sut = RecurrenceRule.create() //
				.setFrequency(WEEKLY) //
				.addByDay(MONDAY) //
				.addByDay(WEDNESDAY) //
				.addByDay(SATURDAY) //
				.build();
		final var taskStart = EPOCH.plusHours(7).plusMinutes(15);
		{
			final var times = sut.getOccurencesBetween(taskStart, NOW_2000, NOW_2000.plusWeeks(2));
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
			final var times = sut.getOccurencesBetween(taskStart, NOW_2000, NOW_2000.plusHours(1));
			assertEquals(1, times.size());
			assertDayOfWeek(times, 0, "2020-01-01T07:15Z", WEDNESDAY);
		}
	}

	@Test
	public void testTask_getOccurencesBetween() throws OpenemsNamedException {
		{
			final var sut = JSCalendar.Task.<JsonObject>create() //
					.setStart("07:00:00") //
					.setDuration(Duration.ofDays(2)) //
					.addRecurrenceRule(b -> b //
							.setFrequency(WEEKLY) //
							.addByDay(TUESDAY, THURSDAY)) //
					.build();
			final var times = sut.getOccurencesBetween(NOW_2000, NOW_2000.plusMonths(1));
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
			final var times = sut.getOccurencesBetween(NOW_2000, NOW_2000.plusMonths(1));
			assertEquals(9, times.size());
		}

		{
			// Test "empty" result with next occurence
			final var sut = JSCalendar.Task.<JsonObject>create() //
					.setStart("07:00:00") //
					.addRecurrenceRule(b -> b //
							.setFrequency(WEEKLY) //
							.addByDay(TUESDAY, THURSDAY)) //
					.build();
			final var times = sut.getOccurencesBetween(NOW_2000, NOW_2000.plusDays(1));
			assertEquals(1, times.size());
		}

		{
			// Test "empty" result with no possible next occurence
			final var sut = JSCalendar.Task.<JsonObject>create() //
					.setStart("07:00:00") //
					.addRecurrenceRule(b -> b //
							.setFrequency(WEEKLY) //
							.setUntil(NOW_2000.toLocalDate() /* same day */) //
							.addByDay(TUESDAY, THURSDAY)) //
					.build();
			final var times = sut.getOccurencesBetween(NOW_2000, NOW_2000.plusDays(1));
			assertEquals(0, times.size());
		}
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
							.setPayload(new StringPayload("ONE")) //
							.build()) //
					.add(t -> t //
							.setStart("12:00") //
							.setDuration(Duration.ofMinutes(30)) //
							.addRecurrenceRule(b -> b //
									.setFrequency(DAILY)) //
							.setPayload(new StringPayload("TWO")) //
							.build()) //
					.add(t -> t //
							.setStart("07:00") //
							.setDuration(Duration.ofHours(8)) //
							.addRecurrenceRule(b -> b //
									.setFrequency(WEEKLY) //
									.addByDay(TUESDAY, THURSDAY)) //
							.setPayload(new StringPayload("THREE")) //
							.build()) //
					.add(t -> t //
							.setStart("00:15") //
							.setDuration(Duration.ofMinutes(15)) //
							.addRecurrenceRule(b -> b //
									.setFrequency(DAILY)) //
							.setPayload(new StringPayload("FOUR")) //
							.build()) //
					.build();

			assertNull(tasks.getActiveOneTask());
			clock.leap(15, MINUTES);
			assertOneTask(tasks.getActiveOneTask(), "2020-01-01T00:15Z", "2020-01-01T00:30Z", "FOUR");
			assertOneTask(tasks.getLastActiveOneTask(), "2020-01-01T00:15Z", "2020-01-01T00:30Z", "FOUR");

			var ots = tasks.getOneTasksBetween(NOW_2000, NOW_2000.plusDays(2));
			assertEquals(10, ots.size());
			assertOneTask(ots.pollFirst(), "2020-01-01T00:15Z", "2020-01-01T00:30Z", "FOUR");
			assertOneTask(ots.pollFirst(), "2020-01-01T12:00Z", "2020-01-01T12:15Z", "TWO");
			assertOneTask(ots.pollFirst(), "2020-01-01T12:15Z", "2020-01-01T12:15Z", "ONE");
			assertOneTask(ots.pollFirst(), "2020-01-01T12:15Z", "2020-01-01T12:30Z", "TWO");
			assertOneTask(ots.pollFirst(), "2020-01-02T00:15Z", "2020-01-02T00:30Z", "FOUR");
			assertOneTask(ots.pollFirst(), "2020-01-02T07:00Z", "2020-01-02T12:00Z", "THREE");
			assertOneTask(ots.pollFirst(), "2020-01-02T12:00Z", "2020-01-02T12:15Z", "TWO");
			assertOneTask(ots.pollFirst(), "2020-01-02T12:15Z", "2020-01-02T12:15Z", "ONE");
			assertOneTask(ots.pollFirst(), "2020-01-02T12:15Z", "2020-01-02T12:30Z", "TWO");
			assertOneTask(ots.pollFirst(), "2020-01-02T12:30Z", "2020-01-02T15:00Z", "THREE");
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
							.setPayload(new StringPayload("ONE")) //
							.build()) //
					.add(t -> t //
							.setStart("12:00") //
							.setDuration(Duration.ofHours(30)) //
							.addRecurrenceRule(b -> b //
									.setFrequency(DAILY)) //
							.setPayload(new StringPayload("TWO")) //
							.build()) //
					.build();

			var ots = tasks.getOneTasksBetween(NOW_2000, NOW_2000.plusDays(2));
			assertEquals(3, ots.size());
			assertOneTask(ots.pollFirst(), "2020-01-01T00:00Z", "2020-01-01T11:00Z", "TWO");
			assertOneTask(ots.pollFirst(), "2020-01-01T11:00Z", "2020-01-01T13:00Z", "ONE");
			assertOneTask(ots.pollFirst(), "2020-01-01T13:00Z", "2020-01-03T00:00Z", "TWO");
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
							.setPayload(new StringPayload("ONE")) //
							.build()) //
					.add(t -> t //
							.setStart(LocalDateTime.of(2020, 1, 2, 4, 0)) //
							.setPayload(new StringPayload("TWO")) //
							.build()) //
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
			System.out.println(ZonedDateTime.now(clock));
			assertNull(tasks.getActiveOneTask());
			// 13:00
			clock.leap(1, MINUTES);
			System.out.println(ZonedDateTime.now(clock));
			assertOneTask(tasks.getActiveOneTask(), "2020-01-02T11:00Z", "2020-01-02T13:00Z", "ONE");
			// 13:00
			clock.leap(2 * 60, MINUTES);
			System.out.println(ZonedDateTime.now(clock));
			assertNull(tasks.getActiveOneTask());
			assertNull(tasks.getActiveOneTask()); // Task 2 sollte kommen
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
		var times = sut.getOccurencesBetween(now, now.plusWeeks(1));
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
		var times = sut.getOccurencesBetween(now, now.plusMonths(2));
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
		var smartConfig = JSCalendar.Tasks.<JsonObject>create() //
				.add(t -> t //
						.setStart("1970-01-01T07:30:00") //
						.addRecurrenceRule(b -> b //
								.setFrequency(WEEKLY) //
								.addByDay(TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)) //
						.setPayload(buildJsonObject() //
								.addProperty("sessionEnergyMinimum", 10000) //
								.build()) //
						.build()) //
				.add(t -> t //
						.setStart("1970-01-01T07:30:00") //
						.addRecurrenceRule(b -> b //
								.setFrequency(WEEKLY) //
								.addByDay(MONDAY)) //
						.setPayload(buildJsonObject() //
								.addProperty("sessionEnergyMinimum", 60000) //
								.build()) //
						.build()) //
				.build();

		var firstTime = ZonedDateTime.parse("2025-03-23T22:45+01:00[Europe/Berlin]");
		var lastTime = ZonedDateTime.parse("2025-04-01T23:00+01:00[Europe/Berlin]");
		var ots = smartConfig.getOneTasksBetween(firstTime, lastTime);

		assertEquals(7, ots.size());
		var first = ots.pollFirst();
		assertOneTask(first, //
				"2025-03-24T07:30+01:00[Europe/Berlin]", "2025-03-24T07:30+01:00[Europe/Berlin]",
				"{\"sessionEnergyMinimum\":60000}");
		assertOneTask(ots.pollFirst(), //
				"2025-03-25T07:30+01:00[Europe/Berlin]", "2025-03-25T07:30+01:00[Europe/Berlin]",
				"{\"sessionEnergyMinimum\":10000}");
		assertOneTask(ots.pollFirst(), //
				"2025-03-26T07:30+01:00[Europe/Berlin]", "2025-03-26T07:30+01:00[Europe/Berlin]",
				"{\"sessionEnergyMinimum\":10000}");
		assertOneTask(ots.pollFirst(), //
				"2025-03-27T07:30+01:00[Europe/Berlin]", "2025-03-27T07:30+01:00[Europe/Berlin]",
				"{\"sessionEnergyMinimum\":10000}");
		assertOneTask(ots.pollFirst(), //
				"2025-03-28T07:30+01:00[Europe/Berlin]", "2025-03-28T07:30+01:00[Europe/Berlin]",
				"{\"sessionEnergyMinimum\":10000}");
		assertOneTask(ots.pollFirst(), //
				"2025-03-31T07:30+02:00[Europe/Berlin]", "2025-03-31T07:30+02:00[Europe/Berlin]",
				"{\"sessionEnergyMinimum\":60000}");
		assertOneTask(ots.pollFirst(), //
				"2025-04-01T07:30+02:00[Europe/Berlin]", "2025-04-01T07:30+02:00[Europe/Berlin]",
				"{\"sessionEnergyMinimum\":10000}");

		assertEquals(
				"OneTask{start=2025-03-24T07:30+01:00[Europe/Berlin], end=2025-03-24T07:30+01:00[Europe/Berlin], duration=null, payload={\"sessionEnergyMinimum\":60000}}",
				first.toString());
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
		var times = sut.getOccurencesBetween(now, now.plusWeeks(1));
		assertEquals(3, times.size());
		assertDayOfWeek(times, 0, "2020-01-01T07:00Z", WEDNESDAY);
		assertDayOfWeek(times, 1, "2020-01-02T07:00Z", THURSDAY);
		assertDayOfWeek(times, 2, "2020-01-03T07:00Z", FRIDAY);

		// Should not find any further occurrence after 'until'
		times = sut.getOccurencesBetween(now.plusWeeks(1), now.plusWeeks(2));
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
		var times = sut.getOccurencesBetween(now, now.plusWeeks(1));
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
