package io.openems.backend.timedata.api;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.JsonElement;

import io.openems.common.types.ChannelAddress;

public class EdgeCache {

	private long timestamp = 0l;
	private final ConcurrentHashMap<ChannelAddress, JsonElement> channelValueCache = new ConcurrentHashMap<>();

	public synchronized final Optional<JsonElement> getChannelValue(ChannelAddress address) {
		return Optional.ofNullable(this.channelValueCache.get(address));
	}

	public synchronized final ConcurrentHashMap<ChannelAddress, JsonElement> getChannelCacheEntries() {
		return this.channelValueCache;
	}

	/**
	 * Adds the channel value to the cache
	 *
	 * @param channel the Channel-Address
	 * @param value the Value as a JsonElement
	 */
	public synchronized void putToChannelCache(ChannelAddress channel, JsonElement value) {
		this.channelValueCache.put(channel, value);
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public void clear() {
		this.channelValueCache.clear();
	}

}
