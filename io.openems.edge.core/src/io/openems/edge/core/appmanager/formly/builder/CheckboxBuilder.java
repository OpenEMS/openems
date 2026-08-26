package io.openems.edge.core.appmanager.formly.builder;

import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.formly.enums.HintIcon;
import io.openems.edge.core.appmanager.formly.enums.Wrappers;

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

	public CheckboxBuilder setHint(String hint, HintIcon icon) {
		this.templateOptions.addProperty("hint", hint);
		if (icon != null) {
			this.templateOptions.addProperty("icon", icon.getIconName());
		} else {
			this.templateOptions.remove("icon");
		}
		this.addWrapper(Wrappers.CHECKBOX_WITH_HINT);
		return this;
	}

	@Override
	protected String getType() {
		return "checkbox";
	}

}