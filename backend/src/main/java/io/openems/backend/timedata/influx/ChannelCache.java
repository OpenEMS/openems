package io.openems.backend.timedata.influx;

public class ChannelCache {
	private final long timestamp; // in milliseconds
	private final Object value;

	public ChannelCache(long timestamp, Object value) {
		super();
		this.timestamp = timestamp;
		this.value = value;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public Object getValue() {
		return value;
	}
}
