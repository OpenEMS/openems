package io.openems.edge.core.appmanager.formly.builder;

import com.google.gson.JsonObject;

import io.openems.common.session.Language;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;

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

	private JsonObject validation;

	public CheckboxBuilder(Nameable property) {
		super(property);
	}

	/**
	 * Requires the checkbox to be checked.
	 * 
	 * @param l the language of the message
	 * @return this
	 */
	public CheckboxBuilder requireTrue(Language l) {
		this.templateOptions.addProperty("pattern", "true");
		final var message = TranslationUtil.getTranslation(AbstractOpenemsApp.getTranslationBundle(l),
				"formly.validation.requireChecked");
		this.getValidation().add("messages", JsonUtils.buildJsonObject() //
				.addProperty("pattern", message) //
				.build());

		return this;
	}

	private JsonObject getValidation() {
		return this.validation = JsonFormlyUtil.single(this.validation);
	}

	@Override
	public JsonObject build() {
		final var result = super.build();
		result.add("validation", this.validation);
		return result;
	}

	@Override
	protected String getType() {
		return "checkbox";
	}

}