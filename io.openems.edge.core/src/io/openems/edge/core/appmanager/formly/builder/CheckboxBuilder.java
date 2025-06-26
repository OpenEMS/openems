package io.openems.edge.core.appmanager.formly.builder;

import io.openems.edge.core.appmanager.Nameable;

/**
 * A Builder for a Formly Checkbox.
 *
 * <pre>
 * {
 * 	"key": "key",
 * 	"type": "checkbox",
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
public final class CheckboxBuilder extends FormlyBuilder<CheckboxBuilder> {

	public CheckboxBuilder(Nameable property) {
		super(property);
	}

	@Override
	protected String getType() {
		return "checkbox";
	}

}