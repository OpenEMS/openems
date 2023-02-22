package io.openems.backend.common.edgewebsocket;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

public class EdgeCache {

	/**
	 * The Timestamp of the data in the Cache.
	 */
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
			var result = this.data.get(address);
			if (result == null) {
				return JsonNull.INSTANCE;
			}
			return result;
		}
	}

	/**
	 * Updates the Cache.
	 *
	 * @param incomingDatas the incoming data
	 */
	public void update(SortedMap<Long, Map<String, JsonElement>> incomingDatas) {
		for (Entry<Long, Map<String, JsonElement>> entry : incomingDatas.entrySet()) {
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
