package io.openems.backend.metadata.odoo.odoo;

import io.openems.backend.metadata.odoo.Field;

public class Domain {
	protected final String field;
	protected final String operator;
	protected final Object value;

	public Domain(String field, String operator, Object value) {
		this.field = field;
		this.operator = operator;
		this.value = value;
	}

	public Domain(Field field, String operator, Object value) {
		this(field.id(), operator, value);
	}
}
