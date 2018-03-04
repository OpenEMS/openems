package io.openems.common.types;

public class TimestampedFieldValue {
	private long timestamp;
	private FieldValue<?> value;
	
	public TimestampedFieldValue(long timestamp, FieldValue<?> value) {
		this.timestamp = timestamp;
		this.value = value;
	}
	
	public long getTimestamp() {
		return timestamp;
	}
	
	public FieldValue<?> getValue() {
		return value;
	}
}
