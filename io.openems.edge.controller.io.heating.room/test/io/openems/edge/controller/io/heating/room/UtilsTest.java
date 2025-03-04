package io.openems.edge.controller.io.heating.room;

import static io.openems.edge.controller.io.heating.room.Utils.getNextHighPeriod;
import static org.junit.Assert.assertEquals;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.Test;

import io.openems.common.jscalendar.JSCalendar;

public class UtilsTest {

	@Test
	public void test() {
		var schedule = JSCalendar.Task.fromStringOrEmpty("""
				[
				   {
				      "@type":"Task",
				      "start":"05:30:00",
				      "duration":"PT2H30M",
				      "recurrenceRules":[
				         {
				            "frequency":"weekly",
				            "byDay":[
				               "mo",
				               "tu",
				               "we",
				               "th",
				               "fr"
				            ]
				         }
				      ]
				   },
				   {
				      "@type":"Task",
				      "start":"14:00:00",
				      "duration":"PT10H",
				      "recurrenceRules":[
				         {
				            "frequency":"weekly",
				            "byDay":[
				               "mo",
				               "tu",
				               "we",
				               "th",
				               "fr"
				            ]
				         }
				      ]
				   },
				   {
				      "@type":"Task",
				      "start":"08:00:00",
				      "duration":"PT16H",
				      "recurrenceRules":[
				         {
				            "frequency":"weekly",
				            "byDay":[
				               "sa",
				               "su"
				            ]
				         }
				      ]
				   }
				]""", j -> j);

		assertEquals("HighPeriod[from=2025-01-06T04:30:00Z, to=2025-01-06T07:00:00Z]", getNextHighPeriod(//
				ZonedDateTime.of(2025, 1, 6, 5, 29, 0, 0, ZoneId.of("Europe/Berlin")), schedule).toString());
		assertEquals("HighPeriod[from=2025-01-06T04:30:00Z, to=2025-01-06T07:00:00Z]", getNextHighPeriod(//
				ZonedDateTime.of(2025, 1, 6, 5, 31, 0, 0, ZoneId.of("Europe/Berlin")), schedule).toString());
		assertEquals("HighPeriod[from=2025-01-06T13:00:00Z, to=2025-01-06T23:00:00Z]", getNextHighPeriod(//
				ZonedDateTime.of(2025, 1, 6, 9, 0, 0, 0, ZoneId.of("Europe/Berlin")), schedule).toString());
	}
}
