package io.openems.impl.persistence.influxdb;

public abstract class FieldValue<T> {
	public final String field;
	public final T value;

	public FieldValue(String field, T value) {
		this.field = field;
		this.value = value;
	}
}
