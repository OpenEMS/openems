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

public class TouOctopusHeatTest {

	// Octopus Heat constants
	private static final double HEAT_HIGHER_PRICE = 32.5;
	private static final double HEAT_STANDARD_PRICE = 24.8;
	private static final double HEAT_LOWER_PRICE = 12.3;
	private TouManualHelper heatHelper;

	private Clock clock;

	@Before
	public void setup() {
		this.clock = TestUtils.createDummyClock();

		// Setup Octopus Heat schedule
		final var schedule = JSCalendar.Tasks.<Double>create() //
				.add(t -> t // Lower price 02:00-06:00
						.setStart(LocalTime.of(2, 0)) //
						.setDuration(Duration.ofHours(4)) //
						.addRecurrenceRule(b -> b //
								.setFrequency(DAILY)) //
						.setPayload(HEAT_LOWER_PRICE) //
						.build()) //
				.add(t -> t // Lower price 12:00-16:00
						.setStart(LocalTime.of(12, 0)) //
						.setDuration(Duration.ofHours(4)) //
						.addRecurrenceRule(b -> b //
								.setFrequency(DAILY)) //
						.setPayload(HEAT_LOWER_PRICE) //
						.build()) //
				.add(t -> t // Higher price 18:00-21:00
						.setStart(LocalTime.of(18, 0)) //
						.setDuration(Duration.ofHours(3)) //
						.addRecurrenceRule(b -> b //
								.setFrequency(DAILY)) //
						.setPayload(HEAT_HIGHER_PRICE) //
						.build()) //
				.build();
		this.heatHelper = new TouManualHelper(this.clock, schedule, HEAT_STANDARD_PRICE);
	}

	// Octopus Heat Tests
	@Test
	public void testHeatStandardPriceOutsideSchedule() {
		var testTime = roundDownToQuarter(ZonedDateTime.now(this.clock).withHour(8));
		var price = this.heatHelper.getPrices().getAt(testTime);
		assertEquals(HEAT_STANDARD_PRICE, price, 0.001);
	}

	@Test
	public void testHeatStandardPriceOutsideScheduleWithMinutes() {
		var testTime = roundDownToQuarter(ZonedDateTime.now(this.clock).withHour(8).withMinute(15));
		var price = this.heatHelper.getPrices().getAt(testTime);
		assertEquals(HEAT_STANDARD_PRICE, price, 0.001);
	}

	@Test
	public void testHeatLowerPriceMorning() {
		var testTime = roundDownToQuarter(ZonedDateTime.now(this.clock).withHour(3));
		var price = this.heatHelper.getPrices().getAt(testTime);
		assertEquals(HEAT_LOWER_PRICE, price, 0.001);
	}

	@Test
	public void testHeatLowerPriceAfternoon() {
		var testTime = roundDownToQuarter(ZonedDateTime.now(this.clock).withHour(14));
		var price = this.heatHelper.getPrices().getAt(testTime);
		assertEquals(HEAT_LOWER_PRICE, price, 0.001);
	}

	@Test
	public void testHeatHigherPriceEvening() {
		var testTime = roundDownToQuarter(ZonedDateTime.now(this.clock).withHour(19));
		var price = this.heatHelper.getPrices().getAt(testTime);
		assertEquals(HEAT_HIGHER_PRICE, price, 0.001);
	}

	@Test
	public void testHeatBoundaryMorningLowerPriceStart() {
		var testTime = roundDownToQuarter(ZonedDateTime.now(this.clock).withHour(2));
		var price = this.heatHelper.getPrices().getAt(testTime);
		assertEquals(HEAT_LOWER_PRICE, price, 0.001);
	}

	@Test
	public void testHeatBoundaryMorningLowerPriceEnd() {
		var testTime = roundDownToQuarter(ZonedDateTime.now(this.clock).withHour(6));
		var price = this.heatHelper.getPrices().getAt(testTime);
		assertEquals(HEAT_STANDARD_PRICE, price, 0.001);
	}

	@Test
	public void testHeatBoundaryAfternoonLowerPriceStart() {
		var testTime = roundDownToQuarter(ZonedDateTime.now(this.clock).withHour(12));
		var price = this.heatHelper.getPrices().getAt(testTime);
		assertEquals(HEAT_LOWER_PRICE, price, 0.001);
	}

	@Test
	public void testHeatBoundaryAfternoonLowerPriceEnd() {
		var testTime = roundDownToQuarter(ZonedDateTime.now(this.clock).withHour(16));
		var price = this.heatHelper.getPrices().getAt(testTime);
		assertEquals(HEAT_STANDARD_PRICE, price, 0.001);
	}

	@Test
	public void testHeatBoundaryHigherPriceStart() {
		var testTime = roundDownToQuarter(ZonedDateTime.now(this.clock).withHour(18));
		var price = this.heatHelper.getPrices().getAt(testTime);
		assertEquals(HEAT_HIGHER_PRICE, price, 0.001);
	}

	@Test
	public void testHeatBoundaryHigherPriceEnd() {
		var testTime = roundDownToQuarter(ZonedDateTime.now(this.clock).withHour(21));
		var price = this.heatHelper.getPrices().getAt(testTime);
		assertEquals(HEAT_STANDARD_PRICE, price, 0.001);
	}

}
