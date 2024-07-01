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
		return this.field;
	}

	public T getValue() {
		return this.value;
	}

	@Override
	public String toString() {
		String string;
		if (this.value instanceof JsonElement) {
			string = StringUtils.toShortString((JsonElement) this.value, 100);
		} else if (this.value instanceof String) {
			string = StringUtils.toShortString((String) this.value, 100);
		} else {
			string = this.value.toString();
		}
		string = string.replace("\n", "");
		return this.field.id() + ":" + string;
	}
}
