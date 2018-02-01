package io.openems.backend.timedata.influx;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

public class DeviceCache {
	private long timestamp = 0l;
	private final Map<String, Object> channelValueCache = new HashMap<>();

	public synchronized final Optional<Object> getChannelValueOpt(String address) {
		return Optional.ofNullable(this.channelValueCache.get(address));
	}

	public synchronized final Set<Entry<String, Object>> getChannelCacheEntries() {
		return this.channelValueCache.entrySet();
	}

	/**
	 * Adds the channel value to the cache
	 *
	 * @param channel
	 * @param timestamp
	 * @param value
	 */
	public synchronized void putToChannelCache(String address, Object value) {
		this.channelValueCache.put(address, value);
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
