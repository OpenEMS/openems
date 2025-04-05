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

import com.google.common.collect.ImmutableList;

import io.openems.common.jscalendar.JSCalendar;
import io.openems.common.test.TestUtils;

public class TouManualHelperTest {

	private static final double STANDARD_PRICE = 24.8;
	private static final double LOW_PRICE = 14.5;
	private TouManualHelper sut;
	private Clock clock;

	@Before
	public void setup() {
		this.clock = TestUtils.createDummyClock();
		final var schedule = ImmutableList.of(JSCalendar.Task.<Double>create() //
				.setStart(LocalTime.of(0, 0)) //
				.setDuration(Duration.ofHours(5)) //
				.addRecurrenceRule(b -> b //
						.setFrequency(DAILY)) //
				.setPayload(LOW_PRICE) //
				.build());

		this.sut = new TouManualHelper(this.clock, schedule, STANDARD_PRICE);
	}

	@Test
	public void testStandardPriceOutsideSchedule() {
		var testTime = roundDownToQuarter(ZonedDateTime.now(this.clock).withHour(10));
		var price = this.sut.getPrices().getAt(testTime);
		assertEquals(STANDARD_PRICE, price, 0.001);
	}

	@Test
	public void testLowPriceDuringSchedule() {
		var testTime = roundDownToQuarter(ZonedDateTime.now(this.clock).withHour(2));
		var price = this.sut.getPrices().getAt(testTime);
		assertEquals(LOW_PRICE, price, 0.001);
	}

	@Test
	public void testBoundaryStartOfLowPricePeriod() {
		var testTime = roundDownToQuarter(ZonedDateTime.now(this.clock).withHour(0));
		var price = this.sut.getPrices().getAt(testTime);
		assertEquals(LOW_PRICE, price, 0.001);
	}

	@Test
	public void testBoundaryEndOfLowPricePeriod() {
		var testTime = roundDownToQuarter(ZonedDateTime.now(this.clock).withHour(5));
		var price = this.sut.getPrices().getAt(testTime);
		assertEquals(STANDARD_PRICE, price, 0.001);
	}

}
