package io.openems.backend.metadata.odoo;

public class FieldValue<T> {
	private final Field field;
	private final T value;

	public FieldValue(Field field, T value) {
		this.field = field;
		this.value = value;
	}

	public Field getField() {
		return field;
	}

	public T getValue() {
		return value;
	}

	@Override
	public String toString() {
		return "[" + this.field.n() + ":" + value + "]";
	}
}
