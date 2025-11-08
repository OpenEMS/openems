package io.openems.edge.timeofusetariff.manual;

import static io.openems.common.jscalendar.JSCalendar.RecurrenceFrequency.DAILY;
import static io.openems.common.utils.DateUtils.roundDownToQuarter;
import static org.junit.Assert.assertEquals;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalTime;
import java.time.ZonedDateTime;

import org.junit.Before;
import org.junit.Test;

import io.openems.common.jscalendar.JSCalendar;
import io.openems.common.test.TestUtils;
import io.openems.edge.timeofusetariff.api.TouManualHelper;

public class TouOctopusGoTest {

	// Octopus Go constants
	private static final double GO_STANDARD_PRICE = 24.8;
	private static final double GO_LOW_PRICE = 14.5;
	private TouManualHelper goHelper;

	private Clock clock;

	@Before
	public void setup() {
		this.clock = TestUtils.createDummyClock();

		// Setup Octopus Go schedule
		final var schedule = JSCalendar.Tasks.<Double>create() //
				.setClock(this.clock) //
				.add(t -> t //
						.setStart(LocalTime.of(0, 0)) //
						.setDuration(Duration.ofHours(5)) //
						.addRecurrenceRule(b -> b.setFrequency(DAILY)) //
						.setPayload(GO_LOW_PRICE) //
						.build()) //
				.build();
		this.goHelper = new TouManualHelper(this.clock, schedule, GO_STANDARD_PRICE);
	}

	// Octopus Go Tests
	@Test
	public void testGoStandardPriceOutsideSchedule() {
		var testTime = roundDownToQuarter(ZonedDateTime.now(this.clock).withHour(10));
		var price = this.goHelper.getPrices().getAt(testTime);
		assertEquals(GO_STANDARD_PRICE, price, 0.001);
	}

	@Test
	public void testGoLowPriceDuringSchedule() {
		var testTime = roundDownToQuarter(ZonedDateTime.now(this.clock).withHour(2));
		var price = this.goHelper.getPrices().getAt(testTime);
		assertEquals(GO_LOW_PRICE, price, 0.001);
	}

	@Test
	public void testGoBoundaryStartOfLowPricePeriod() {
		var testTime = roundDownToQuarter(ZonedDateTime.now(this.clock).withHour(0));
		var price = this.goHelper.getPrices().getAt(testTime);
		assertEquals(GO_LOW_PRICE, price, 0.001);
	}

	@Test
	public void testGoBoundaryEndOfLowPricePeriod() {
		var testTime = roundDownToQuarter(ZonedDateTime.now(this.clock).withHour(5));
		var price = this.goHelper.getPrices().getAt(testTime);
		assertEquals(GO_STANDARD_PRICE, price, 0.001);
	}

}
