package io.openems.edge.predictor.profileclusteringmodel.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Clock;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.timedata.test.DummyTimedata;

public class PredictionDataServiceTest {

	@Test
	public void testFetchSeriesForWindow_ShouldReturnCorrectSeries() throws Exception {
		var timedata = new DummyTimedata("timedata0");
		var clock = Clock.fixed(ZonedDateTime.parse("2025-07-10T00:00:00+02:00").toInstant(),
				ZoneId.of("Europe/Berlin"));
		var channelAddress = new ChannelAddress("_sum", "testChannel");

		// Add data for 10 days, but with missing values every other 15min
		for (int day = 0; day < 10; day++) {
			var dayStart = ZonedDateTime.now(clock).minusDays(day).truncatedTo(ChronoUnit.DAYS);
			for (int min = 0; min < 24 * 60; min += 15) {
				// Skip some values to simulate missing data
				if (min % 30 != 0) {
					timedata.add(dayStart.plusMinutes(min), channelAddress, day * 10 + min);
				}
			}
		}

		var sut = new PredictionDataService(timedata, () -> clock, channelAddress);

		var series = sut.fetchSeriesForWindow(7);

		// Should have 7 (days) * 96 (quarters) entries
		assertEquals(7 * (24 * 4), series.getValues().size());

		// First timestamp should be exactly 7 days ago, truncated to day
		var expectedStart = ZonedDateTime.now(clock).minusDays(7).truncatedTo(ChronoUnit.DAYS);
		assertEquals(expectedStart, series.getIndex().getFirst());

		// Check that there are missing (NaN) values
		long nanCount = series.getValues().stream().filter(v -> v.isNaN()).count();
		assertTrue(nanCount > 0);

		// Ensure timestamps are continuous with 15 minutes between each
		var timestamps = series.getIndex();
		for (int i = 1; i < timestamps.size(); i++) {
			assertEquals(15, ChronoUnit.MINUTES.between(timestamps.get(i - 1), timestamps.get(i)));
		}
	}

	@Test
	public void testFetchSeriesForToday_ShouldReturnDataForToday() throws Exception {
		var timedata = new DummyTimedata("timedata0");
		var clock = Clock.fixed(//
				ZonedDateTime.parse("2025-07-10T12:00:00+02:00").toInstant(), //
				ZoneId.of("Europe/Berlin"));
		var channelAddress = new ChannelAddress("_sum", "testChannel");

		// Add values for today from 00:00 to 11:45 every 15 minutes
		var todayStart = ZonedDateTime.now(clock).truncatedTo(ChronoUnit.DAYS);
		for (int min = 0; min < 12 * 60; min += 15) {
			timedata.add(todayStart.plusMinutes(min), channelAddress, min);
		}

		var sut = new PredictionDataService(timedata, () -> clock, channelAddress);
		var series = sut.fetchSeriesForToday();

		// Should have 48 entries (12 hours * 4 quarters)
		assertEquals(48, series.getValues().size());

		// First timestamp should be at 00:00
		assertEquals(todayStart, series.getIndex().getFirst());

		// Ensure there are no NaN values
		long nanCount = series.getValues().stream().filter(v -> v.isNaN()).count();
		assertEquals(0, nanCount);

		// Ensure timestamps are evenly spaced by 15 minutes
		var timestamps = series.getIndex();
		for (int i = 1; i < timestamps.size(); i++) {
			assertEquals(15, ChronoUnit.MINUTES.between(timestamps.get(i - 1), timestamps.get(i)));
		}
	}
}
