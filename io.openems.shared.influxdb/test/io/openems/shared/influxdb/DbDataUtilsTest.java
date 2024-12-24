package io.openems.shared.influxdb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Consumer;

import org.junit.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;

public class DbDataUtilsTest {

	private static final ZoneId ZONE = ZoneId.of("UTC");

	@Test
	public void testNormalizeTable() {
		final var dummyChannel = new ChannelAddress("cmp0", "chn0");
		final SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> data = new TreeMap<>();
		data.put(ZonedDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZONE), newMap(map -> {
			map.put(dummyChannel, new JsonPrimitive(10));
		}));
		data.put(ZonedDateTime.of(2020, 1, 3, 0, 0, 0, 0, ZONE), newMap(map -> {
			map.put(dummyChannel, new JsonPrimitive(30));
		}));
		data.put(ZonedDateTime.of(2020, 1, 5, 0, 0, 0, 0, ZONE), newMap(map -> {
			map.put(dummyChannel, new JsonPrimitive(50));
		}));

		final var normalizedTable = DbDataUtils.normalizeTable(data, Set.of(dummyChannel),
				new Resolution(1, ChronoUnit.DAYS), ZonedDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZONE),
				ZonedDateTime.of(2020, 1, 6, 0, 0, 0, 0, ZONE));

		assertEquals(5, normalizedTable.size());
		for (int i = 0; i < 5; i++) {
			final var value = normalizedTable.get(ZonedDateTime.of(2020, 1, 1 + i, 0, 0, 0, 0, ZONE));
			if (i % 2 == 1) {
				assertTrue(value.get(dummyChannel).isJsonNull());
			} else {
				assertFalse(value.get(dummyChannel).isJsonNull());
			}
		}
	}

	@Test
	public void testCalculateLastMinusFirst() {
		final var dummyChannel = new ChannelAddress("cmp0", "chn0");
		final SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> data = new TreeMap<>();
		data.put(ZonedDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZONE), newMap(map -> {
			map.put(dummyChannel, new JsonPrimitive(10));
		}));
		data.put(ZonedDateTime.of(2020, 1, 2, 0, 0, 0, 0, ZONE), newMap(map -> {
			map.put(dummyChannel, new JsonPrimitive(20));
		}));
		data.put(ZonedDateTime.of(2020, 1, 3, 0, 0, 0, 0, ZONE), newMap(map -> {
			map.put(dummyChannel, new JsonPrimitive(30));
		}));
		data.put(ZonedDateTime.of(2020, 1, 4, 0, 0, 0, 0, ZONE), newMap(map -> {
			map.put(dummyChannel, new JsonPrimitive(40));
		}));
		data.put(ZonedDateTime.of(2020, 1, 5, 0, 0, 0, 0, ZONE), newMap(map -> {
			map.put(dummyChannel, new JsonPrimitive(50));
		}));

		final var lastMinusFirstResult = DbDataUtils.calculateLastMinusFirst(data,
				ZonedDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZONE));

		assertEquals(5, lastMinusFirstResult.size());
		for (var entry : lastMinusFirstResult.entrySet()) {
			assertEquals(1, entry.getValue().size());
			final var value = entry.getValue().get(dummyChannel);
			assertEquals(10, value.getAsDouble(), 0);
		}
	}

	private static <K, V> TreeMap<K, V> newMap(Consumer<TreeMap<K, V>> consumer) {
		var map = new TreeMap<K, V>();
		consumer.accept(map);
		return map;
	}

}
