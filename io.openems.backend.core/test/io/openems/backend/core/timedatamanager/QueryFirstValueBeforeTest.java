package io.openems.backend.core.timedatamanager;

import static org.junit.Assert.assertEquals;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

import io.openems.backend.common.timedata.TimedataManager;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;

public class QueryFirstValueBeforeTest {

	private TimedataManager timedataManager;

	private static final ChannelAddress CHANNEL_1 = new ChannelAddress("comp1", "Channel1");
	private static final ChannelAddress CHANNEL_2 = new ChannelAddress("comp1", "Channel2");
	private static final ChannelAddress CHANNEL_UNDEFINED = new ChannelAddress("some0", "RandomChannel");

	@Before
	public void before() {

		final var timedata0 = DummyTimedata.create() //
				.setId("timedata0") //
				.setQueryFirstValueBeforeFromPredefinedData(Map.of(//
						ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")),
						Map.<ChannelAddress, JsonElement>of(CHANNEL_1, new JsonPrimitive(50)) //
				)) //
				.build();

		final var timedata1 = DummyTimedata.create() //
				.setId("timedata1") //
				.setQueryFirstValueBeforeFromPredefinedData(Map.of(//
						ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")), Map.<ChannelAddress, JsonElement>of(//
								CHANNEL_1, new JsonPrimitive(100), //
								CHANNEL_2, new JsonPrimitive(200)) //
				)) //
				.build();

		final var timedataManager = new TimedataManagerImpl();
		timedataManager.activate(MyConfig.create() //
				.setTimedataIds(timedata0.id(), timedata1.id()) //
				.build());
		timedataManager.addTimedata(timedata0);
		timedataManager.addTimedata(timedata1);

		this.timedataManager = timedataManager;
	}

	@Test
	public void testFirstResult() throws Exception {
		final var date = ZonedDateTime.of(2000, 1, 2, 0, 0, 0, 0, ZoneId.of("UTC"));
		final var result = this.timedataManager.queryFirstValueBefore("edge0", date, Set.of(CHANNEL_1));

		assertEquals(1, result.size());
		assertEquals(new JsonPrimitive(50), result.get(CHANNEL_1));
	}

	@Test(expected = OpenemsNamedException.class)
	public void testNoneAreAvailable() throws Exception {
		final var date = ZonedDateTime.of(2000, 1, 2, 0, 0, 0, 0, ZoneId.of("UTC"));
		this.timedataManager.queryFirstValueBefore("edge0", date, Set.of(CHANNEL_UNDEFINED));
	}

	@Test
	public void testFirstPossibleResult() throws Exception {
		final var date = ZonedDateTime.of(2000, 1, 2, 0, 0, 0, 0, ZoneId.of("UTC"));
		final var result = this.timedataManager.queryFirstValueBefore("edge0", date, Set.of(CHANNEL_1, CHANNEL_2));

		assertEquals(2, result.size());
		assertEquals(new JsonPrimitive(50), result.get(CHANNEL_1));
		assertEquals(new JsonPrimitive(200), result.get(CHANNEL_2));
	}

	@Test
	public void testOnlySecondAvailable() throws Exception {
		final var date = ZonedDateTime.of(2000, 1, 2, 0, 0, 0, 0, ZoneId.of("UTC"));
		final var result = this.timedataManager.queryFirstValueBefore("edge0", date, Set.of(CHANNEL_2));

		assertEquals(1, result.size());
		assertEquals(new JsonPrimitive(200), result.get(CHANNEL_2));
	}

	@Test
	public void testSomeChannelsAreAvailable() throws Exception {
		final var date = ZonedDateTime.of(2000, 1, 2, 0, 0, 0, 0, ZoneId.of("UTC"));
		final var result = this.timedataManager.queryFirstValueBefore("edge0", date,
				Set.of(CHANNEL_1, CHANNEL_UNDEFINED));

		assertEquals(2, result.size());
		assertEquals(new JsonPrimitive(50), result.get(CHANNEL_1));
		assertEquals(JsonNull.INSTANCE, result.get(CHANNEL_UNDEFINED));
	}

}
