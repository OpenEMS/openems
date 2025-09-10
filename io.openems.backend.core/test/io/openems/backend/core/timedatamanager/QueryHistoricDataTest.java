package io.openems.backend.core.timedatamanager;

import static org.junit.Assert.assertEquals;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

import io.openems.backend.common.timedata.TimedataManager;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;

public class QueryHistoricDataTest {

	private TimedataManager timedataManager;

	private static final ChannelAddress CHANNEL_1 = new ChannelAddress("comp1", "Channel1");
	private static final ChannelAddress CHANNEL_2 = new ChannelAddress("comp1", "Channel2");
	private static final ChannelAddress CHANNEL_UNDEFINED = new ChannelAddress("some0", "RandomChannel");

	@Before
	public void before() {
		final var timedata0 = DummyTimedata.create() //
				.setId("timedata0") //
				.setQueryHistoricDataFromPredefinedData(Map.of(//
						ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")),
						Map.<ChannelAddress, JsonElement>of(CHANNEL_1, new JsonPrimitive(50)) //
				)) //
				.build();

		final var timedata1 = DummyTimedata.create() //
				.setId("timedata1") //
				.setQueryHistoricDataFromPredefinedData(Map.of(//
						ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")), Map.<ChannelAddress, JsonElement>of(//
								CHANNEL_1, new JsonPrimitive(100), //
								CHANNEL_2, new JsonPrimitive(200)) //
				)) //
				.build();

		var timedataManager = new TimedataManagerImpl();
		timedataManager.activate(MyConfig.create() //
				.setTimedataIds(timedata0.id(), timedata1.id()) //
				.build());
		timedataManager.addTimedata(timedata0);
		timedataManager.addTimedata(timedata1);

		this.timedataManager = timedataManager;
	}

	@Test
	public void testFirstResult() throws Exception {
		final var from = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
		final var to = ZonedDateTime.of(2000, 1, 1, 0, 5, 0, 0, ZoneId.of("UTC"));
		final var result = this.timedataManager.queryHistoricData("edge0", from, to, Set.of(CHANNEL_1),
				new Resolution(5, ChronoUnit.MINUTES));

		assertEquals(1, result.size());
		final var entry = result.get(result.firstKey());
		assertEquals(1, entry.size());
		assertEquals(new JsonPrimitive(50), entry.get(CHANNEL_1));
	}

	@Test(expected = OpenemsNamedException.class)
	public void testNoneAreAvailable() throws Exception {
		final var from = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
		final var to = ZonedDateTime.of(2000, 1, 1, 0, 5, 0, 0, ZoneId.of("UTC"));
		this.timedataManager.queryHistoricData("edge0", from, to, Set.of(CHANNEL_UNDEFINED),
				new Resolution(5, ChronoUnit.MINUTES));
	}

	@Test
	public void testFirstResultFillMissing() throws Exception {
		final var from = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
		final var to = ZonedDateTime.of(2000, 1, 1, 0, 10, 0, 0, ZoneId.of("UTC"));
		final var result = this.timedataManager.queryHistoricData("edge0", from, to, Set.of(CHANNEL_1),
				new Resolution(5, ChronoUnit.MINUTES));

		assertEquals(2, result.size());
		final var firstEntry = result.get(result.firstKey());
		assertEquals(1, firstEntry.size());
		assertEquals(new JsonPrimitive(50), firstEntry.get(CHANNEL_1));
		final var lastEntry = result.get(result.lastKey());
		assertEquals(1, lastEntry.size());
		assertEquals(JsonNull.INSTANCE, lastEntry.get(CHANNEL_1));
	}

	@Test
	public void testFirstPossibleResult() throws Exception {
		final var from = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
		final var to = ZonedDateTime.of(2000, 1, 1, 0, 5, 0, 0, ZoneId.of("UTC"));
		final var result = this.timedataManager.queryHistoricData("edge0", from, to, Set.of(CHANNEL_1, CHANNEL_2),
				new Resolution(5, ChronoUnit.MINUTES));

		assertEquals(1, result.size());
		final var entry = result.get(result.firstKey());
		assertEquals(2, entry.size());
		assertEquals(new JsonPrimitive(50), entry.get(CHANNEL_1));
		assertEquals(new JsonPrimitive(200), entry.get(CHANNEL_2));
	}

	@Test
	public void testOnlySecondAvailable() throws Exception {
		final var from = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
		final var to = ZonedDateTime.of(2000, 1, 1, 0, 5, 0, 0, ZoneId.of("UTC"));
		final var result = this.timedataManager.queryHistoricData("edge0", from, to, Set.of(CHANNEL_2),
				new Resolution(5, ChronoUnit.MINUTES));

		assertEquals(1, result.size());
		final var entry = result.get(result.firstKey());
		assertEquals(1, entry.size());
		assertEquals(new JsonPrimitive(200), entry.get(CHANNEL_2));
	}

	@Test
	public void testSomeChannelsAreAvailable() throws Exception {
		final var from = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
		final var to = ZonedDateTime.of(2000, 1, 1, 0, 5, 0, 0, ZoneId.of("UTC"));
		final var result = this.timedataManager.queryHistoricData("edge0", from, to,
				Set.of(CHANNEL_1, CHANNEL_UNDEFINED), new Resolution(5, ChronoUnit.MINUTES));

		assertEquals(1, result.size());
		final var entry = result.get(result.firstKey());
		assertEquals(2, entry.size());
		assertEquals(new JsonPrimitive(50), entry.get(CHANNEL_1));
		assertEquals(JsonNull.INSTANCE, entry.get(CHANNEL_UNDEFINED));
	}

}
