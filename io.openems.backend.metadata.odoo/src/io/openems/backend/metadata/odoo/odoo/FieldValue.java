package io.openems.backend.metadata.odoo.odoo;

import static io.openems.common.utils.StringUtils.toShortString;

import com.google.gson.JsonElement;

import io.openems.backend.metadata.odoo.Field;

public class FieldValue<T> {
	private final Field field;
	private final T value;

	public FieldValue(Field field, T value) {
		this.field = field;
		this.value = value;
	}

	public Field getField() {
		return this.field;
	}

	public T getValue() {
		return this.value;
	}

	@Override
	public String toString() {
		var string = switch (this.value) {
		case JsonElement je //
			-> toShortString(je, 100);
		case String s //
			-> toShortString(s, 100);
		default //
			-> this.value.toString();
		};
		string = string.replace("\n", "");
		return this.field.id() + ":" + string;
	}
}
