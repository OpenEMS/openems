package io.openems.edge.core.appmanager.formly.builder;

import com.google.gson.JsonObject;

import io.openems.edge.core.appmanager.Nameable;

/**
 * A Builder for a Formly DateTime Picker.
 *
 * <pre>
 * {
 * 	"key": "key",
 * 	"type": "datetime",
 * 	"templateOptions": {
 * 		"label": "label",
 * 		"required": true
 * 	},
 * 	"expressionProperties": {
 * 		"templateOptions.required": "model.PROPERTY"
 * 	},
 * 	"hideExpression": "!model.PROPERTY",
 * 	"defaultValue": "defaultValue"
 * }
 * </pre>
 *
 */
public final class DateTimeBuilder extends FormlyBuilder<DateTimeBuilder> {

	public DateTimeBuilder(Nameable property) {
		super(property);
	}

	@Override
	public JsonObject build() {
		final var result = super.build();
		this.templateOptions.addProperty("presentation", "date-time");
		return result;
	}

	@Override
	protected String getType() {
		return "datetime";
	}

}