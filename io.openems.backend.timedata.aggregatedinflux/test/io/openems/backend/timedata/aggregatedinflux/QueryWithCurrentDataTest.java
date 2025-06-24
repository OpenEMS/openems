package io.openems.backend.timedata.aggregatedinflux;

import static org.junit.Assert.assertEquals;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.SortedMap;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

import io.openems.backend.common.test.DummyEdgeManager;
import io.openems.common.test.TimeLeapClock;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;

public class QueryWithCurrentDataTest {

	private static final ZoneId UTC = ZoneId.of("UTC");

	@Test
	public void testQueryHistoricEnergyPerPeriodDayResUtc() throws Exception {
		this.testQueryHistoricEnergyPerPeriodDayResolutionTimezone(UTC);
	}

	@Test
	public void testQueryHistoricEnergyPerPeriodDayResEuropeBerlin() throws Exception {
		this.testQueryHistoricEnergyPerPeriodDayResolutionTimezone(ZoneId.of("Europe/Berlin"));
	}

	private void testQueryHistoricEnergyPerPeriodDayResolutionTimezone(final ZoneId zone) throws Exception {
		final var channel = new ChannelAddress("comp0", "Channel");

		final var edgeManager = new DummyEdgeManager(
				ImmutableMap.<String, SortedMap<ChannelAddress, JsonElement>>builder() //
						.put("edge0", ImmutableSortedMap.<ChannelAddress, JsonElement>naturalOrder() //
								.put(channel, new JsonPrimitive(300)) //
								.build()) //
						.build());
		final var queryWithCurrentData = new QueryWithCurrentData(null, edgeManager,
				new TimeLeapClock(ZonedDateTime.of(2025, 1, 4, 13, 33, 0, 0, UTC).toInstant(), UTC));

		final var rawData = ImmutableSortedMap.<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>>naturalOrder() //
				.put(ZonedDateTime.of(2024, 12, 31, 0, 0, 0, 0, zone),
						ImmutableSortedMap.<ChannelAddress, JsonElement>naturalOrder() //
								.put(channel, new JsonPrimitive(0)) //
								.build()) //
				.put(ZonedDateTime.of(2025, 1, 1, 0, 0, 0, 0, zone),
						ImmutableSortedMap.<ChannelAddress, JsonElement>naturalOrder() //
								.put(channel, new JsonPrimitive(100)) //
								.build()) //
				.put(ZonedDateTime.of(2025, 1, 2, 0, 0, 0, 0, zone),
						ImmutableSortedMap.<ChannelAddress, JsonElement>naturalOrder() //
								.put(channel, new JsonPrimitive(200)) //
								.build()) //
				.put(ZonedDateTime.of(2025, 1, 3, 0, 0, 0, 0, zone),
						ImmutableSortedMap.<ChannelAddress, JsonElement>naturalOrder() //
								.put(channel, new JsonPrimitive(250)) //
								.build()) //
				.build();

		final var result = queryWithCurrentData.queryHistoricEnergyPerPeriod("edge0", //
				ZonedDateTime.of(2025, 1, 1, 0, 0, 0, 0, zone), //
				ZonedDateTime.of(2025, 2, 1, 0, 0, 0, 0, zone), //
				Set.of(channel), new Resolution(1, ChronoUnit.DAYS), rawData);

		assertEquals(31, result.size());
		assertEquals(100, result.get(ZonedDateTime.of(2025, 1, 1, 0, 0, 0, 0, zone)).get(channel).getAsInt());
		assertEquals(100, result.get(ZonedDateTime.of(2025, 1, 2, 0, 0, 0, 0, zone)).get(channel).getAsInt());
		assertEquals(50, result.get(ZonedDateTime.of(2025, 1, 3, 0, 0, 0, 0, zone)).get(channel).getAsInt());
		assertEquals(50, result.get(ZonedDateTime.of(2025, 1, 4, 0, 0, 0, 0, zone)).get(channel).getAsInt());
		for (int i = 5; i <= 12; i++) {
			assertEquals(JsonNull.INSTANCE, result.get(ZonedDateTime.of(2025, 1, i, 0, 0, 0, 0, zone)).get(channel));
		}
	}

	@Test
	public void testQueryHistoricEnergyPerPeriodMonthResUtc() throws Exception {
		this.testQueryHistoricEnergyPerPeriodMonthResolutionTimezone(UTC);
	}

	@Test
	public void testQueryHistoricEnergyPerPeriodMonthResEuropeBerlin() throws Exception {
		this.testQueryHistoricEnergyPerPeriodMonthResolutionTimezone(ZoneId.of("Europe/Berlin"));
	}

	private void testQueryHistoricEnergyPerPeriodMonthResolutionTimezone(final ZoneId zone) throws Exception {
		final var channel = new ChannelAddress("comp0", "Channel");

		final var edgeManager = new DummyEdgeManager(
				ImmutableMap.<String, SortedMap<ChannelAddress, JsonElement>>builder() //
						.put("edge0", ImmutableSortedMap.<ChannelAddress, JsonElement>naturalOrder() //
								.put(channel, new JsonPrimitive(300)) //
								.build()) //
						.build());
		final var queryWithCurrentData = new QueryWithCurrentData(null, edgeManager,
				new TimeLeapClock(ZonedDateTime.of(2025, 3, 16, 13, 33, 0, 0, UTC).toInstant(), UTC));

		final var rawData = ImmutableSortedMap.<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>>naturalOrder() //
				.put(ZonedDateTime.of(2024, 12, 31, 0, 0, 0, 0, zone),
						ImmutableSortedMap.<ChannelAddress, JsonElement>naturalOrder() //
								.put(channel, new JsonPrimitive(0)) //
								.build()) //
				.put(ZonedDateTime.of(2025, 1, 1, 0, 0, 0, 0, zone),
						ImmutableSortedMap.<ChannelAddress, JsonElement>naturalOrder() //
								.put(channel, new JsonPrimitive(100)) //
								.build()) //
				.put(ZonedDateTime.of(2025, 2, 1, 0, 0, 0, 0, zone),
						ImmutableSortedMap.<ChannelAddress, JsonElement>naturalOrder() //
								.put(channel, new JsonPrimitive(200)) //
								.build()) //
				.put(ZonedDateTime.of(2025, 3, 1, 0, 0, 0, 0, zone),
						ImmutableSortedMap.<ChannelAddress, JsonElement>naturalOrder() //
								.put(channel, new JsonPrimitive(250)) //
								.build()) //
				.build();

		final var result = queryWithCurrentData.queryHistoricEnergyPerPeriod("edge0", //
				ZonedDateTime.of(2025, 1, 1, 0, 0, 0, 0, zone), //
				ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, zone), //
				Set.of(channel), new Resolution(1, ChronoUnit.MONTHS), rawData);

		assertEquals(12, result.size());
		assertEquals(100, result.get(ZonedDateTime.of(2025, 1, 1, 0, 0, 0, 0, zone)).get(channel).getAsInt());
		assertEquals(100, result.get(ZonedDateTime.of(2025, 2, 1, 0, 0, 0, 0, zone)).get(channel).getAsInt());
		assertEquals(100, result.get(ZonedDateTime.of(2025, 3, 1, 0, 0, 0, 0, zone)).get(channel).getAsInt());
		for (int i = 4; i <= 12; i++) {
			assertEquals(JsonNull.INSTANCE, result.get(ZonedDateTime.of(2025, i, 1, 0, 0, 0, 0, zone)).get(channel));
		}
	}

	@Test
	public void testQueryHistoricEnergyPerPeriodYearResUtc() throws Exception {
		this.testQueryHistoricEnergyPerPeriodYearResolutionTimezone(UTC);
	}

	@Test
	public void testQueryHistoricEnergyPerPeriodYearResEuropeBerlin() throws Exception {
		this.testQueryHistoricEnergyPerPeriodYearResolutionTimezone(ZoneId.of("Europe/Berlin"));
	}

	private void testQueryHistoricEnergyPerPeriodYearResolutionTimezone(final ZoneId zone) throws Exception {
		final var channel = new ChannelAddress("comp0", "Channel");

		final var edgeManager = new DummyEdgeManager(
				ImmutableMap.<String, SortedMap<ChannelAddress, JsonElement>>builder() //
						.put("edge0", ImmutableSortedMap.<ChannelAddress, JsonElement>naturalOrder() //
								.put(channel, new JsonPrimitive(300)) //
								.build()) //
						.build());
		final var queryWithCurrentData = new QueryWithCurrentData(null, edgeManager,
				new TimeLeapClock(ZonedDateTime.of(2026, 3, 16, 13, 33, 0, 0, UTC).toInstant(), UTC));

		final var rawData = ImmutableSortedMap.<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>>naturalOrder() //
				.put(ZonedDateTime.of(2024, 12, 31, 0, 0, 0, 0, zone),
						ImmutableSortedMap.<ChannelAddress, JsonElement>naturalOrder() //
								.put(channel, new JsonPrimitive(0)) //
								.build()) //
				.put(ZonedDateTime.of(2025, 1, 1, 0, 0, 0, 0, zone),
						ImmutableSortedMap.<ChannelAddress, JsonElement>naturalOrder() //
								.put(channel, new JsonPrimitive(100)) //
								.build()) //
				.build();

		final var result = queryWithCurrentData.queryHistoricEnergyPerPeriod("edge0", //
				ZonedDateTime.of(2025, 1, 1, 0, 0, 0, 0, zone), //
				ZonedDateTime.of(2027, 1, 1, 0, 0, 0, 0, zone), //
				Set.of(channel), new Resolution(1, ChronoUnit.YEARS), rawData);

		assertEquals(2, result.size());
		assertEquals(100, result.get(ZonedDateTime.of(2025, 1, 1, 0, 0, 0, 0, zone)).get(channel).getAsInt());
		assertEquals(200, result.get(ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, zone)).get(channel).getAsInt());
	}

}
