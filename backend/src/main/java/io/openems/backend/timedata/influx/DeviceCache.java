package io.openems.backend.timedata.influx;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

public class DeviceCache {
	private final Map<String, ChannelCache> channelCacheMap = new HashMap<>();

	public synchronized final Optional<ChannelCache> getChannelCacheOpt(String address) {
		return Optional.ofNullable(this.channelCacheMap.get(address));
	}

	public synchronized final Set<Entry<String, ChannelCache>> getChannelCacheEntries() {
		return this.channelCacheMap.entrySet();
	}

	/**
	 * Adds the channel value to the cache, if there is no younger entry alread existing
	 *
	 * @param channel
	 * @param timestamp
	 * @param value
	 */
	public synchronized void putToChannelCache(String channel, long timestamp, Object value) {
		ChannelCache channelCache = this.channelCacheMap.get(channel);
		if (channelCache == null) {
			// create new
			this.channelCacheMap.put(channel, new ChannelCache(timestamp, value));
		} else {
			// was existing. check timestamp
			if (channelCache.getTimestamp() < timestamp) {
				// replace
				this.channelCacheMap.put(channel, new ChannelCache(timestamp, value));
			}
		}
	}
}
