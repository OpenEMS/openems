package io.openems.backend.common.edge;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

import io.openems.common.jsonrpc.notification.AggregatedDataNotification;
import io.openems.common.jsonrpc.notification.TimestampedDataNotification;
import io.openems.common.types.Tuple2;

public class EdgeCache {

	private final ChannelDataCache current = new ChannelDataCache();
	private final ChannelDataCache aggregated = new ChannelDataCache();

	public record TimestampedValue(long timestamp, JsonElement value) {

	}

	private static class ChannelDataCache {
		private long timestamp = 0L;
		private final HashMap<String, JsonElement> data = new HashMap<>();

		/**
		 * Gets the channel value from cache.
		 *
		 * @param address the Channel-Address of the channel
		 * @return the value; {@link JsonNull} if it is not in cache
		 */
		public final JsonElement getChannelValue(String address) {
			synchronized (this) {
				return this.data.get(address);
			}
		}

		/**
		 * Gets the channel value with last update timestamp from cache.
		 *
		 * @param address the Channel-Address of the channel
		 * @return the {@link TimestampedValue} or null if it is not in cache
		 */
		public final TimestampedValue getChannelValueWithTimestamp(String address) {
			synchronized (this) {
				final var value = this.data.get(address);
				if (value == null) {
					return null;
				}
				return new TimestampedValue(this.timestamp, value);
			}
		}

		/**
		 * Updates the Cache.
		 *
		 * @param incomingDatas the incoming data, ordered by timestamp ascending
		 */
		public void update(Map<Long, Map<String, JsonElement>> incomingDatas) {
			for (var entry : incomingDatas.entrySet()) {
				var incomingTimestamp = entry.getKey();
				var incomingData = entry.getValue();

				// Check if cache should be applied
				if (incomingTimestamp < this.timestamp) {
					// Incoming data is older than cache -> do not apply cache

				} else {
					// Incoming data is more recent than cache

					if (incomingTimestamp > this.timestamp + 15 * 60 * 1000) {
						// Cache is not anymore valid (elder than 15 minutes) -> clear Cache
						synchronized (this) {
							this.data.clear();
						}
					}

					// update cache
					this.timestamp = incomingTimestamp;
					synchronized (this) {
						this.data.putAll(incomingData);
					}
				}
			}
		}
	}

	/**
	 * Gets the channel value from cache.
	 *
	 * @param address the Channel-Address of the channel
	 * @return the value; {@link JsonNull} if it is not in cache
	 */
	public final JsonElement getChannelValue(String address) {
		final var result = this.current.getChannelValue(address);
		if (result != null) {
			return result;
		}
		final var aggregatedResult = this.aggregated.getChannelValue(address);
		if (aggregatedResult != null) {
			return aggregatedResult;
		}
		return JsonNull.INSTANCE;
	}

	/**
	 * Gets the channel value with last update timestamp from cache.
	 * 
	 * @param address the Channel-Address of the channel
	 * @return the {@link TimestampedValue} or null if it is not in cache
	 */
	public final TimestampedValue getChannelValueWithTimestamp(String address) {
		final var result = this.current.getChannelValueWithTimestamp(address);
		if (result != null) {
			return result;
		}
		final var aggregatedResult = this.aggregated.getChannelValueWithTimestamp(address);
		if (aggregatedResult != null) {
			return aggregatedResult;
		}
		return null;
	}

	/**
	 * Gets the channel values from cache.
	 *
	 * @param addresses the Channel-Addresses of the channels
	 * @return a) Map of Channel-Address to values ({@link JsonNull} if not in
	 *         cache); b) Set of Channel-Addresses that are only available as
	 *         aggregated data
	 */
	public final Tuple2<Map<String, JsonElement>, Set<String>> getChannelValues(Set<String> addresses) {
		final var result = new TreeMap<String, JsonElement>();
		final var aggregatedChannelValues = new TreeSet<String>();
		for (var address : addresses) {
			final var value = this.current.getChannelValue(address);
			if (value != null) {
				result.put(address, value);
				continue;
			}
			final var aggregatedValue = this.aggregated.getChannelValue(address);
			if (aggregatedValue != null) {
				result.put(address, aggregatedValue);
				aggregatedChannelValues.add(address);
				continue;
			}
			result.put(address, JsonNull.INSTANCE);
		}
		return new Tuple2<>(result, aggregatedChannelValues);
	}

	/**
	 * Updates the Cache.
	 *
	 * @param notification the incoming data
	 */
	public void updateCurrentData(TimestampedDataNotification notification) {
		this.current.update(notification.getData().rowMap());
	}

	/**
	 * Updates the aggregated data cache.
	 *
	 * @param notification the incoming data
	 */
	public void updateAggregatedData(AggregatedDataNotification notification) {
		this.aggregated.update(notification.getData().rowMap());
	}

}
