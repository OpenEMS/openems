package io.openems.edge.controller.evcs;

import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.common.utils.JsonUtils.prettyToString;
import static io.openems.common.utils.UuidUtils.getNilUuid;
import static io.openems.edge.common.test.TestUtils.createDummyClock;
import static io.openems.edge.controller.evcs.JSCalendar.RecurrenceFrequency.WEEKLY;
import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static java.util.function.Function.identity;
import static org.junit.Assert.assertEquals;

import java.time.ZonedDateTime;

import org.junit.Test;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;

//CHECKSTYLE:OFF
public class JSCalendarTest {
	// CHECKSTYLE:ON

	@Test
	public void testWeekday() throws OpenemsNamedException {
		var clock = createDummyClock();
		var sut = new JSCalendar.Task.Builder<JsonObject>(getNilUuid(), ZonedDateTime.now(clock)) //
				.setStart("2024-06-17T07:00:00") //
				.addRecurrenceRule(b -> b //
						.setFrequency(WEEKLY) //
						.addByDay(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)) //
				.setPayload(buildJsonObject() //
						.addProperty("sessionEnergy", 10000) //
						.build()) //
				.build();
		assertEquals("""
				{
				  "@type": "Task",
				  "uid": "00000000-0000-0000-0000-000000000000",
				  "updated": "2020-01-01T00:00:00Z",
				  "start": "2024-06-17T07:00:00",
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
				  "payload": {
				    "sessionEnergy": 10000
				  }
				}""", prettyToString(sut.toJson(identity())));

		var next = sut.getNextOccurence(ZonedDateTime.now(clock));
		assertEquals("2024-06-17T07:00Z", next.toString());
		next = sut.getNextOccurence(next.plusSeconds(1));
		assertEquals("2024-06-18T07:00Z", next.toString());
		next = sut.getNextOccurence(next.plusSeconds(1));
		assertEquals("2024-06-19T07:00Z", next.toString());
		next = sut.getNextOccurence(next.plusSeconds(1));
		assertEquals("2024-06-20T07:00Z", next.toString());
		next = sut.getNextOccurence(next.plusSeconds(1));
		assertEquals("2024-06-21T07:00Z", next.toString());
		next = sut.getNextOccurence(next.plusSeconds(1)); // next week
		assertEquals("2024-06-24T07:00Z", next.toString());
		next = sut.getNextOccurence(next);
		assertEquals("2024-06-24T07:00Z", next.toString()); // same

		// Parse JSON
		var fromJson = JSCalendar.Task.fromJson(sut.toJson(identity()), identity());
		assertEquals(sut.toJson(identity()), fromJson.toJson(identity()));
	}

	@Test
	public void testWeekend() throws OpenemsNamedException {
		var clock = createDummyClock();
		var sut = new JSCalendar.Task.Builder<JsonObject>(getNilUuid(), ZonedDateTime.now(clock)) //
				.setStart("2024-06-17T00:00:00") //
				.addRecurrenceRule(b -> b //
						.setFrequency(WEEKLY) //
						.addByDay(SATURDAY, SUNDAY)) //
				.setPayload(buildJsonObject() //
						.addProperty("sessionEnergy", 10001) //
						.build()) //
				.build();
		assertEquals("""
				{
				  "@type": "Task",
				  "uid": "00000000-0000-0000-0000-000000000000",
				  "updated": "2020-01-01T00:00:00Z",
				  "start": "2024-06-17T00:00:00",
				  "recurrenceRules": [
				    {
				      "frequency": "weekly",
				      "byDay": [
				        "sa",
				        "su"
				      ]
				    }
				  ],
				  "payload": {
				    "sessionEnergy": 10001
				  }
				}""", prettyToString(sut.toJson(identity())));

		var next = sut.getNextOccurence(ZonedDateTime.now(clock));
		assertEquals("2024-06-22T00:00Z", next.toString());
		next = sut.getNextOccurence(next.plusSeconds(1));
		assertEquals("2024-06-23T00:00Z", next.toString());
		next = sut.getNextOccurence(next.plusSeconds(1));
		assertEquals("2024-06-29T00:00Z", next.toString());
		next = sut.getNextOccurence(next.plusSeconds(1));
		assertEquals("2024-06-30T00:00Z", next.toString());
		next = sut.getNextOccurence(next.plusSeconds(1));
		assertEquals("2024-07-06T00:00Z", next.toString());

		// Parse JSON
		var fromJson = JSCalendar.Task.fromJson(sut.toJson(identity()), identity());
		assertEquals(sut.toJson(identity()), fromJson.toJson(identity()));
	}

}
