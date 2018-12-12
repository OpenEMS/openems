package io.openems.backend.timedata.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.JsonElement;

import io.openems.common.types.ChannelAddress;

import java.util.Optional;
import java.util.Set;

public class EdgeCache {

	private long timestamp = 0l;
	private final Map<ChannelAddress, JsonElement> channelValueCache = new HashMap<>();

	public synchronized final Optional<JsonElement> getChannelValue(ChannelAddress address) {
		return Optional.ofNullable(this.channelValueCache.get(address));
	}

	public synchronized final Set<Entry<ChannelAddress, JsonElement>> getChannelCacheEntries() {
		return this.channelValueCache.entrySet();
	}

	/**
	 * Adds the channel value to the cache
	 *
	 * @param address
	 * @param value
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
