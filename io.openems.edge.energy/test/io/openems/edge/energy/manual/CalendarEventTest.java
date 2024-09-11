package io.openems.edge.energy.manual;

import static io.openems.edge.energy.manual.RecurrenceFrequency.YEARLY;
import static org.junit.Assert.assertEquals;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.Test;

import io.openems.common.utils.JsonUtils;

public class CalendarEventTest {

	// private final ZonedDateTime DAY0 = ZonedDateTime.of(2024, 9, 9, 0, 0, 0, 0,
	// ZoneId.of("UTC"));
	// private final ZonedDateTime DAY1 = ZonedDateTime.of(2024, 9, 12, 0, 0, 0, 0,
	// ZoneId.of("UTC"));
	private static final ZonedDateTime QUARTER0 = ZonedDateTime.of(2024, 9, 8, 13, 15, 0, 0, ZoneId.of("UTC"));
	// private final ZonedDateTime QUARTER1 = ZonedDateTime.of(2024, 9, 9, 13, 15,
	// 0, 0, ZoneId.of("UTC"));
	// private final ZonedDateTime QUARTER2 = ZonedDateTime.of(2024, 9, 10, 13, 15,
	// 0, 0, ZoneId.of("UTC"));
	// private final ZonedDateTime QUARTER3 = ZonedDateTime.of(2024, 9, 11, 13, 15,
	// 0, 0, ZoneId.of("UTC"));
	// private final ZonedDateTime QUARTER4 = ZonedDateTime.of(2024, 9, 12, 13, 15,
	// 0, 0, ZoneId.of("UTC"));
	// private final ZonedDateTime QUARTER5 = ZonedDateTime.of(2024, 9, 13, 13, 15,
	// 0, 0, ZoneId.of("UTC"));

	@Test
	// Test for https://www.rfc-editor.org/rfc/rfc8984.html#section-6.1
	public void test6_1_SimpleEvent() {
		assertEquals("""
				{
				  "@type": "Event",
				  "uid": "a8df6573-0474-496d-8496-033ad45d7fea",
				  "updated": "2020-01-02T18:23:04Z",
				  "title": "Some event",
				  "start": "2020-01-15T13:00:00",
				  "timeZone": "America/New_York",
				  "duration": "PT1H"
				}""",
				JsonUtils.prettyToString(
						new CalendarEvent.Builder("a8df6573-0474-496d-8496-033ad45d7fea", "2020-01-02T18:23:04Z") //
								.setTitle("Some event") //
								.setStart("2020-01-15T13:00:00") //
								.setTimeZone("America/New_York") //
								.setDuration("PT1H") //
								.build().toJsonObject()));
	}

	@Test
	// Test for https://www.rfc-editor.org/rfc/rfc8984.html#section-6.4
	public void test6_4_AllDayEvent() {
		assertEquals("""
				{
				  "@type": "Event",
				  "uid": "a8df6573-0474-496d-8496-033ad45d7feb",
				  "updated": "2020-01-02T18:23:05Z",
				  "title": "April Fool's Day",
				  "showWithoutTime": true,
				  "start": "1900-04-01T00:00:00",
				  "duration": "P1D",
				  "recurrenceRules": [{
				    "@type": "RecurrenceRule",
				    "frequency": "yearly"
				  }]
				}""",
				JsonUtils.prettyToString(
						new CalendarEvent.Builder("a8df6573-0474-496d-8496-033ad45d7feb", "2020-01-02T18:23:05Z") //
								.setTitle("April Fool's Day") //
								.setStart("1900-04-01T00:00:00") //
								.setDuration("P1D") //
								.setRecurrenceRules(RecurrenceRule.create() //
										.frequency(YEARLY) //
										.build()) //
								.build().toJsonObject()));
	}

	@Test
	public void test() {
//		var weekends0 = Recur.create(Frequency.DAILY) //
//				.setCount(10) //
//				.setByDay(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) //
//				.build();
//
//		var event = CalendarEvent.create() //
//				.setSummary("Weekdays") //
//				.setStart(QUARTER0) //
//				.setRecur(Recur.create(Frequency.DAILY) //
//						.setCount(10) //
//						.setByDay(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) //
//						.build()) //
//				.build(); //
//
//		var weekends1 = JsonUtils.buildJsonObject() //
//				.addProperty("freq", Frequency.DAILY.name()) //
//				.addProperty("count", 10) //
//				.add("byday", JsonUtils.buildJsonArray() //
//						.add(DayOfWeek.SATURDAY.name()) //
//						.add(DayOfWeek.SUNDAY.name()) //
//						.build()) //
//				.build();
//
//		System.out.println(weekends1);
//		System.out.println(weekends0.toJsonObject());
//		System.out.println(event);

		// event.getIndividualEvents(DAY0, DAY1);

		// Should be: QUARTER1 to QUARTER4

	}

}
