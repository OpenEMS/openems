package io.openems.backend.metadata.odoo.odoo;

import com.google.gson.JsonElement;

import io.openems.backend.metadata.odoo.Field;
import io.openems.common.utils.StringUtils;

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
		String string;
		if (value instanceof JsonElement) {
			string = StringUtils.toShortString((JsonElement) value, 100);
		} else if (value instanceof String) {
			string = StringUtils.toShortString((String) value, 100);
		} else {
			string = this.value.toString();
		}
		string = string.replaceAll("\n", "");
		return this.field.id() + ":" + string;
	}
}
