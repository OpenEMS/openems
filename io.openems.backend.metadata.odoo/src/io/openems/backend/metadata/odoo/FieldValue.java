package io.openems.backend.metadata.odoo;

public class FieldValue {
	private final Field field;
	private final String value;

	public FieldValue(Field field, String value) {
		this.field = field;
		this.value = value;
	}

	public Field getField() {
		return field;
	}

	public String getValue() {
		return value;
	}

	@Override
	public String toString() {
		return "[" + this.field.n() + ":" + value + "]";
	}
}
