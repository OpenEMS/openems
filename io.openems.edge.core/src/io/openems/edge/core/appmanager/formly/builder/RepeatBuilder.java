package io.openems.edge.core.appmanager.formly.builder;

import com.google.gson.JsonObject;

import io.openems.edge.core.appmanager.Nameable;

/**
 * A Builder for a Formly Checkbox.
 *
 * <pre>
 * {
 * 	"key": "key",
 * 	"type": "repeat",
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
public final class RepeatBuilder extends FormlyBuilder<RepeatBuilder> {

	private JsonObject fieldArray;

	public RepeatBuilder(Nameable property) {
		super(property);
	}

	public RepeatBuilder setAddText(String addText) {
		if (addText != null && !addText.isBlank()) {
			this.templateOptions.addProperty("addText", addText);
		} else if (this.templateOptions.has("addText")) {
			this.templateOptions.remove("addText");
		}
		return this;
	}

	public RepeatBuilder setFieldArray(JsonObject object) {
		this.fieldArray = object;
		return this;
	}

	@Override
	protected String getType() {
		return "repeat";
	}

	@Override
	public JsonObject build() {
		if (this.fieldArray != null) {
			this.jsonObject.add("fieldArray", this.fieldArray);
		}
		return super.build();
	}

}