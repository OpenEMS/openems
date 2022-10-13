package io.openems.backend.common.edgewebsocket;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.function.BiConsumer;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

import io.openems.common.types.ChannelAddress;

public class EdgeCache {

	/**
	 * The Timestamp of the data in the Cache.
	 */
	private long cacheTimestamp = 0L;

	/**
	 * The Timestamp when the Cache was last applied to the incoming data.
	 */
	private long lastAppliedTimestamp = 0L;

	private final HashMap<ChannelAddress, JsonElement> cacheData = new HashMap<>();

	/**
	 * Gets the channel value from cache.
	 *
	 * @param address the {@link ChannelAddress} of the channel
	 * @return the value; empty if it is not in cache
	 */
	public final JsonElement getChannelValue(ChannelAddress address) {
		synchronized (this) {
			var result = this.cacheData.get(address);
			if (result == null) {
				return JsonNull.INSTANCE;
			} else {
				return result;
			}
		}
	}

	/**
	 * Updates the 'incoming data' with the data from the cache.
	 *
	 * @param incomingDatas  the incoming data
	 * @param onInvalidCache callback on invalid cache. Can be used for a log
	 *                       message.
	 */
	public void complementDataFromCache(SortedMap<Long, Map<ChannelAddress, JsonElement>> incomingDatas,
			BiConsumer<Instant, Instant> onInvalidCache) {
		for (Entry<Long, Map<ChannelAddress, JsonElement>> entry : incomingDatas.entrySet()) {
			var incomingTimestamp = entry.getKey();
			var incomingData = entry.getValue();

			// Check if cache should be applied
			if (incomingTimestamp < this.cacheTimestamp) {
				// Incoming data is older than cache -> do not apply cache

			} else {
				// Incoming data is more recent than cache

				if (incomingTimestamp > this.cacheTimestamp + 5 * 60 * 1000) {
					// Cache is not anymore valid (elder than 5 minutes)
					if (this.cacheTimestamp != 0L) {
						onInvalidCache.accept(Instant.ofEpochMilli(incomingTimestamp),
								Instant.ofEpochMilli(this.cacheTimestamp));
					}
					synchronized (this) {
						// Clear Cache
						this.cacheData.clear();
					}

				} else if (incomingTimestamp < this.lastAppliedTimestamp + 2 * 60 * 1000) {
					// Apply Cache only once every two minutes to throttle writes

				} else {
					// Apply Cache

					// cache is valid (not elder than 5 minutes)
					this.lastAppliedTimestamp = incomingTimestamp;
					synchronized (this) {
						this.cacheData.entrySet().stream() //
								.forEach(e -> {
									// check if there is a current value for this timestamp + channel
									// if not -> add cache data to write data
									incomingData.putIfAbsent(e.getKey(), e.getValue());
								});
					}
				}

				// update cache
				this.cacheTimestamp = incomingTimestamp;
				synchronized (this) {
					this.cacheData.putAll(incomingData);
				}
			}
		}
	}

}
