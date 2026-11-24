package io.openems.backend.common.edge;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

import io.openems.common.jsonrpc.notification.TimestampedDataNotification;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.DataTable;

public class EdgeCacheTest {

	private static final String CHANNEL1 = new ChannelAddress("foo", "bar1").toString();
	private static final String CHANNEL2 = new ChannelAddress("foo", "bar2").toString();
	private static final String CHANNEL3 = new ChannelAddress("foo", "bar3").toString();

	@Test
	public void test() {
		var cache = new EdgeCache();
		var timestamp = 0L;

		var data1 = buildData(timestamp, CHANNEL1, "value1");
		cache.updateCurrentData(data1);
		assertEquals("value1", cache.getChannelValue(CHANNEL1).getAsString());

		// older than cache
		var data2 = buildData(timestamp - 1, CHANNEL1, "ignore");
		cache.updateCurrentData(data2);
		assertEquals("value1", cache.getChannelValue(CHANNEL1).getAsString());

		// normal operation
		var data3 = buildData(timestamp += 2 * 60 * 1000, CHANNEL2, "value2");
		cache.updateCurrentData(data3);
		assertEquals("value1", cache.getChannelValue(CHANNEL1).getAsString());
		assertEquals("value2", cache.getChannelValue(CHANNEL2).getAsString());

		// exactly 15 minutes later, cache is still valid
		var data3b = buildData(timestamp += 15 * 60 * 1000, CHANNEL2, "value2");
		cache.updateCurrentData(data3b);
		assertEquals("value1", cache.getChannelValue(CHANNEL1).getAsString());
		assertEquals("value2", cache.getChannelValue(CHANNEL2).getAsString());

		// invalidate cache
		var data4 = buildData(timestamp += 15 * 60 * 1000 + 1, CHANNEL3, "value3");
		cache.updateCurrentData(data4);
		assertEquals(JsonNull.INSTANCE, cache.getChannelValue(CHANNEL1));
		assertEquals(JsonNull.INSTANCE, cache.getChannelValue(CHANNEL2));
		assertEquals("value3", cache.getChannelValue(CHANNEL3).getAsString());
	}

	private static TimestampedDataNotification buildData(long timestamp, String channel, String value) {
		var table = new DataTable();
		table.put(timestamp, channel, new JsonPrimitive(value));
		return new TimestampedDataNotification(table);
	}
}
