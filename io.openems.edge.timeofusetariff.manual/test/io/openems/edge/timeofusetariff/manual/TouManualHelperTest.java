package io.openems.edge.timeofusetariff.manual;

import static io.openems.common.jscalendar.JSCalendar.RecurrenceFrequency.DAILY;

import java.time.Duration;
import java.time.LocalTime;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

import io.openems.common.jscalendar.JSCalendar;
import io.openems.common.test.TestUtils;

public class TouManualHelperTest {

	@Test
	public void test() {
		final var clock = TestUtils.createDummyClock();
		final var schedule = ImmutableList.of(JSCalendar.Task.<Double>create() //
				.setStart(LocalTime.of(0, 0)) //
				.setDuration(Duration.ofHours(5)) //
				.addRecurrenceRule(b -> b //
						.setFrequency(DAILY)) //
				.setPayload(0.20) //
				.build());

		var sut = new TouManualHelper(clock, schedule, 0.3);
		System.out.println(sut.getPrices());
	}

}
