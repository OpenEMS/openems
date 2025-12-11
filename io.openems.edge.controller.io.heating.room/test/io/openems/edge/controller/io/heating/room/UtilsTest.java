package io.openems.edge.controller.io.heating.room;

import static io.openems.common.test.TestUtils.createDummyClock;
import static org.junit.Assert.assertEquals;

import java.time.ZonedDateTime;

import org.junit.Test;

import io.openems.common.jscalendar.JSCalendar;

public class UtilsTest {

	@Test
	public void test() {
		var clock = createDummyClock();
		var schedule = JSCalendar.Tasks.fromStringOrEmpty(clock, """
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
				]""");

		var now = ZonedDateTime.now(clock);
		var ots = schedule.getOneTasksBetween(now, now.plusDays(1));
		assertEquals(3, ots.size());
		assertEquals("OneTask{start=2020-01-01T05:30Z, end=2020-01-01T08:00Z, duration=PT2H30M, payload=null}",
				ots.pollFirst().toString());
		assertEquals("OneTask{start=2020-01-01T14:00Z, end=2020-01-02T00:00Z, duration=PT10H, payload=null}",
				ots.pollFirst().toString());
		assertEquals("OneTask{start=2020-01-04T08:00Z, end=2020-01-05T00:00Z, duration=PT16H, payload=null}",
				ots.pollFirst().toString());
	}
}
