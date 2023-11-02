package io.openems.edge.core.appmanager.formly.builder;

import com.google.gson.JsonObject;

import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.session.Language;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;
import io.openems.edge.core.appmanager.formly.enums.InputType;
import io.openems.edge.core.appmanager.formly.enums.Validation;
import io.openems.edge.core.appmanager.formly.enums.Wrappers;

/**
 * A Builder for a Formly Input.
 *
 * <pre>
 * {
 * 	"key": "key",
 * 	"type": "input",
 * 	"templateOptions": {
 * 		"type": "number",
 * 		"label": "label",
 * 		"placeholder": "placeholder",
 * 		"required": true,
 * 		"min": 0,
 * 		"max": 100,
 * 		"minLenght": 6,
 * 		"maxLenght": 18,
 * 		"pattern": /(\d{1,3}\.){3}\d{1,3}/
 * 	},
 * 	"validation": {
 * 		"messages": {
 * 			"pattern": "Input is not a valid IP Address!",
 * 		},
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
public final class InputBuilder extends FormlyBuilder<InputBuilder> {

	private JsonObject validation = null;
	private InputType type = InputType.TEXT;

	public InputBuilder(Nameable property) {
		super(property);
	}

	/**
	 * Sets the type of the input.
	 *
	 * <p>
	 * Default: {@link InputType#TEXT}
	 *
	 * @param type to be set
	 * @return this
	 */
	public InputBuilder setInputType(InputType type) {
		this.type = type;
		return this;
	}

	public InputBuilder setPlaceholder(String placeholder) {
		if (placeholder != null && !placeholder.isBlank()) {
			this.templateOptions.addProperty("placeholder", placeholder);
		} else if (this.templateOptions.has("placeholder")) {
			this.templateOptions.remove("placeholder");
		}
		return this;
	}

	/**
	 * Sets the min value of the input.
	 *
	 * @param min the min number that can be set
	 * @return this
	 * @throws IllegalArgumentException if the type is not set to number
	 */
	public InputBuilder setMin(int min) {
		if (this.type != InputType.NUMBER) {
			throw new IllegalArgumentException("Value min can only be set on Number inputs!");
		}
		this.templateOptions.addProperty("min", min);
		return this;
	}

	/**
	 * Sets the max value of the input.
	 *
	 * @param max the max number that can be set
	 * @return this
	 * @throws IllegalArgumentException if the type is not set to number
	 */
	public InputBuilder setMax(int max) {
		if (this.type != InputType.NUMBER) {
			throw new IllegalArgumentException("Value max can only be set on Number inputs!");
		}
		this.templateOptions.addProperty("max", max);
		return this;
	}

	/**
	 * Sets the minLength of the input.
	 *
	 * @param minLength the min length the input needs
	 * @return this
	 * @throws IllegalArgumentException if the type is not set to password or text
	 */
	public InputBuilder setMinLenght(int minLength) {
		if (this.type == InputType.NUMBER) {
			throw new IllegalArgumentException("Value minLength can only be set on Password or Text inputs!");
		}
		this.templateOptions.addProperty("minLength", minLength);
		return this;
	}

	/**
	 * Sets the minLength of the input.
	 *
	 * @param maxLength the max length the input needs
	 * @return this
	 * @throws IllegalArgumentException if the type is not set to password or text
	 */
	public InputBuilder setMaxLenght(int maxLength) {
		if (this.type == InputType.NUMBER) {
			throw new IllegalArgumentException("Value maxLength can only be set on Password or Text inputs!");
		}
		this.templateOptions.addProperty("maxLength", maxLength);
		return this;
	}

	/**
	 * Sets the validation of the Input.
	 * <p>
	 * e. g. to set the validation of an IP use {@link Validation#IP}
	 * </p>
	 *
	 * @param validation the validation to be set
	 * @return this
	 */
	public InputBuilder setValidation(Validation validation) {
		this.setPattern(validation.getPattern());
		this.setValidationMessage("pattern", validation.getErrorMsg());
		return this;
	}

	/**
	 * Only allows positive number as a input.
	 * 
	 * @return this
	 * @throws IllegalArgumentException if this {@link InputBuilder} has not been
	 *                                  set to a {@link InputType#NUMBER} input via
	 *                                  the
	 *                                  {@link InputBuilder}{@link #setInputType(InputType)}
	 *                                  method.
	 */
	public InputBuilder onlyPositiveNumbers() {
		if (this.type != InputType.NUMBER) {
			throw new IllegalArgumentException("OnlyPositiveNumbers can only be set on number inputs!");
		}
		this.getValidators().add("validation", JsonUtils.buildJsonArray() //
				.add("onlyPositiveInteger") //
				.build());
		return this;
	}

	public InputBuilder setUnit(Unit unit, Language l) {
		var unitString = switch (unit) {
		case WATT -> TranslationUtil.getTranslation(AbstractOpenemsApp.getTranslationBundle(l), "watt");
		default -> unit.symbol;
		};
		this.templateOptions.addProperty("unit", unitString);
		this.addWrapper(Wrappers.INPUT_WITH_UNIT);
		return this;
	}

	private InputBuilder setPattern(String pattern) {
		if (this.type != InputType.TEXT) {
			throw new IllegalArgumentException("Pattern can only be set on Text inputs!");
		}
		this.templateOptions.addProperty("pattern", pattern);
		this.setValidationMessage("pattern", "Input is not a valid IP Address!");
		return this;
	}

	private InputBuilder setValidationMessage(String field, String msg) {
		var validatonObject = this.getValidation();
		var messages = validatonObject.get("messages");
		if (messages == null) {
			messages = new JsonObject();
			validatonObject.add("messages", messages);
		}
		JsonObject messagesObject;
		try {
			messagesObject = JsonUtils.getAsJsonObject(messages);
			if (msg == null) {
				messagesObject.remove(field);
			} else {
				messagesObject.addProperty(field, msg);
			}
		} catch (OpenemsNamedException e) {
			e.printStackTrace();
		}
		return this;
	}

	@Override
	protected String getType() {
		return "input";
	}

	@Override
	public JsonObject build() {
		if (this.type != InputType.TEXT) {
			this.templateOptions.addProperty("type", this.type.getFormlyTypeName());
		}
		if (this.validation != null && this.validation.size() > 0) {
			this.jsonObject.add("validation", this.validation);
		}
		return super.build();
	}

	protected final JsonObject getValidation() {
		return this.validation = JsonFormlyUtil.single(this.validation);
	}

}