package io.openems.edge.core.appmanager.formly.builder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.openems.common.utils.JsonUtils;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OnlyIf;
import io.openems.edge.core.appmanager.Self;
import io.openems.edge.core.appmanager.formly.DefaultValueOptions;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;
import io.openems.edge.core.appmanager.formly.enums.Wrappers;
import io.openems.edge.core.appmanager.formly.expression.BooleanExpression;
import io.openems.edge.core.appmanager.formly.expression.StringExpression;

/**
 * A Builder for a Formly field.
 *
 * <pre>
 * {
 * 	"key": "key",
 * 	"type": "input",
 * 	"templateOptions": {
 * 		"label": "label",
 * 		"required": true
 * 	},
 * 	"expressionProperties": {
 * 		"templateOptions.required": "model.PROPERTY"
 * 	},
 * 	"hideExpression": "!model.PROPERTY",
 * 	"defaultValue": "defaultValue",
 *  "wrappers": []{@link Wrappers}
 * }
 * </pre>
 *
 */
public abstract class FormlyBuilder<T extends FormlyBuilder<T>> implements OnlyIf<T>, Self<T> {

	protected final JsonObject jsonObject = new JsonObject();
	protected final JsonObject templateOptions = new JsonObject();
	private JsonObject expressionProperties = null;
	private final List<String> wrappers = new ArrayList<>();
	private JsonObject validators = null;

	protected FormlyBuilder(Nameable property) {
		this.setType(this.getType());
		if (property == null) {
			return;
		}
		this.setKey(property.name());
		this.setLabel(property.name());
	}

	private final T setType(String type) {
		if (type == null) {
			this.jsonObject.remove("type");
			return this.self();
		}
		this.jsonObject.addProperty("type", type);
		return this.self();
	}

	public final T setKey(String key) {
		if (key != null) {
			this.jsonObject.addProperty("key", key);
		} else if (this.jsonObject.has("key")) {
			this.jsonObject.remove("key");
		}
		return this.self();
	}

	public final T setDefaultValue(String defaultValue) {
		if (defaultValue != null) {
			this.jsonObject.addProperty("defaultValue", defaultValue);
		} else if (this.jsonObject.has("defaultValue")) {
			this.jsonObject.remove("defaultValue");
		}

		return this.self();
	}

	public final T setDefaultValue(Boolean defaultValue) {
		if (defaultValue != null) {
			this.jsonObject.addProperty("defaultValue", defaultValue);
		} else if (this.jsonObject.has("defaultValue")) {
			this.jsonObject.remove("defaultValue");
		}

		return this.self();
	}

	public final T setDefaultValue(Number defaultValue) {
		if (defaultValue != null) {
			this.jsonObject.addProperty("defaultValue", defaultValue);
		} else if (this.jsonObject.has("defaultValue")) {
			this.jsonObject.remove("defaultValue");
		}

		return this.self();
	}

	public final T setDefaultValue(JsonElement defaultValue) {
		if (defaultValue != null) {
			this.jsonObject.add("defaultValue", defaultValue);
		} else if (this.jsonObject.has("defaultValue")) {
			this.jsonObject.remove("defaultValue");
		}

		return this.self();
	}

	public final T setDefaultValueWithStringSupplier(Supplier<String> supplieDefaultValue) {
		return this.setDefaultValue(supplieDefaultValue.get());
	}

	public final T setDefaultValueWithBooleanSupplier(Supplier<Boolean> supplieDefaultValue) {
		return this.setDefaultValue(supplieDefaultValue.get());
	}

	public final JsonElement getDefaultValue() {
		return this.jsonObject.get("defaultValue");
	}

	/**
	 * Sets if the input is required. Default: 'false'
	 * 
	 * @param isRequired if the input is required
	 * @return this
	 */
	public final T isRequired(boolean isRequired) {
		if (isRequired) {
			this.templateOptions.addProperty("required", isRequired);
		} else if (this.templateOptions.has("required")) {
			this.templateOptions.remove("required");
		}
		return this.self();
	}

	public final T setLabel(String label) {
		if (label != null) {
			this.templateOptions.addProperty("label", label);
		} else if (this.templateOptions.has("label")) {
			this.templateOptions.remove("label");
		}
		return this.self();
	}

	public final T setDescription(String description) {
		this.templateOptions.addProperty("description", description);
		return this.self();
	}

	private final T onlyShowIf(String expression) {
		this.getExpressionProperties().addProperty("templateOptions.required", expression);
		this.jsonObject.addProperty("hideExpression", "!(" + expression + ")");
		return this.self();
	}

	/**
	 * Only shows the current input if the given {@link ExpressionBuilder} returns
	 * true.
	 * 
	 * @param expression the {@link BooleanExpression} to set
	 * @return this
	 */
	public final T onlyShowIf(BooleanExpression expression) {
		return this.onlyShowIf(expression.expression());
	}

	/**
	 * Sets if input is hidden by default.
	 * 
	 * @param hide true if the input should be hidden
	 * @return this
	 */
	public final T hide(boolean hide) {
		if (!hide) {
			this.jsonObject.remove("hide");
			return this.self();
		}
		this.jsonObject.addProperty("hide", true);
		return this.self();
	}

	/**
	 * Sets if input is disabled by default.
	 * 
	 * @param disabled true if the input should be disabled
	 * @return this
	 */
	public final T disabled(boolean disabled) {
		if (!disabled) {
			this.templateOptions.remove("disabled");
			return this.self();
		}
		this.templateOptions.addProperty("disabled", true);
		return this.self();
	}

	/**
	 * Sets if input is readonly.
	 * 
	 * @param readonly true if the input should be readonly
	 * @return this
	 */
	public final T readonly(boolean readonly) {
		if (!readonly) {
			this.templateOptions.remove("readonly");
			return this.self();
		}
		this.templateOptions.addProperty("readonly", true);
		return this.self();
	}

	public final T setLabelExpression(StringExpression expression) {
		this.getExpressionProperties().addProperty("templateOptions.label", expression.expression());
		return this.self();
	}

	public final T setDefaultValueCases(DefaultValueOptions... defaultValueOptions) {
		this.templateOptions.add("defaultValueOptions", Arrays.stream(defaultValueOptions)
				.map(DefaultValueOptions::toJsonObject).collect(JsonUtils.toJsonArray()));
		return this.addWrapper(Wrappers.DEFAULT_OF_CASES);
	}

	/**
	 * Hides the current key of the input. Results are all child inputs are not in
	 * the model as a JsonObject value of this key instead the are on the same level
	 * saved as this field.
	 * 
	 * @return this
	 */
	public T hideKey() {
		this.setKey(null);
		return this.self();
	}

	/**
	 * Adds a wrapper to the current input.
	 * 
	 * @param wrapper the {@link Wrappers} to add
	 * @return this
	 */
	public final T addWrapper(Wrappers wrapper) {
		this.wrappers.add(wrapper.getWrapperClass());
		return this.self();
	}

	public T setCustomValidation(//
			String name, //
			BooleanExpression validationExpression, //
			String errorMessage, //
			Nameable propertyToShowError //
	) {
		this.getValidators().add(name, JsonUtils.buildJsonObject() //
				.addProperty("expressionString", validationExpression.expression()) //
				.addProperty("message", errorMessage) //
				.onlyIf(propertyToShowError != null, //
						b -> b.addProperty("errorPath", propertyToShowError.name())) //
				.build());
		return this.self();
	}

	/**
	 * Sets a custom validation of the input.
	 * 
	 * <p>
	 * This sets a formly validation like explained in the <a href=
	 * "https://formly.dev/docs/examples/validation/custom-validation/">formly
	 * documentation</a> with the exception, that the validation is not directly
	 * passed as a function instead it needs to be a string which is converted into
	 * a validation function from the ui. If you want detailed information about how
	 * the string gets converted to a function in the ui have a look at the post
	 * process function <a href=
	 * "https://github.com/OpenEMS/openems/blob/6cb439e93d78e0c8af04fb98f5b7fca276cac25d/ui/src/app/edge/settings/app/jsonrpc/getAppAssistant.ts#L65">here</a>.
	 * 
	 * <p>
	 * Inside the string expression you have access to:
	 * <ul>
	 * <li>model: the current values</li>
	 * <li>formState: the state of the form</li>
	 * <li>field: the current field</li>
	 * <li>control: the form control</li>
	 * <li>initialModel: the initial model (only set when modifying an existing
	 * instance)</li>
	 * </ul>
	 * 
	 * @param name                 the name of the validation
	 * @param validationExpression the expression of the validation
	 * @param messageExpression    the expression of the error message
	 * @param propertyToShowError  the path property to show the error message
	 * @return this
	 */
	public T setCustomValidation(//
			String name, //
			BooleanExpression validationExpression, //
			StringExpression messageExpression, //
			Nameable propertyToShowError //
	) {
		this.getValidators().add(name, JsonUtils.buildJsonObject() //
				.addProperty("expressionString", validationExpression.expression()) //
				.addProperty("messageString", messageExpression.expression()) //
				.onlyIf(propertyToShowError != null, //
						b -> b.addProperty("errorPath", propertyToShowError.name())) //
				.build());
		return this.self();
	}

	public T setCustomValidation(//
			String name, //
			BooleanExpression validationExpression, //
			String errorMessage //
	) {
		return this.setCustomValidation(name, validationExpression, errorMessage, null);
	}

	public JsonObject build() {
		this.jsonObject.add("templateOptions", this.templateOptions);
		if (this.expressionProperties != null && this.expressionProperties.size() > 0) {
			this.jsonObject.add("expressionProperties", this.expressionProperties);
		}
		if (!this.wrappers.isEmpty()) {
			this.jsonObject.add("wrappers",
					this.wrappers.stream().map(JsonPrimitive::new).collect(JsonUtils.toJsonArray()));
		}
		if (this.validators != null) {
			this.jsonObject.add("validators", this.validators);
		}
		return this.jsonObject;
	}

	protected abstract String getType();

	protected final JsonObject getExpressionProperties() {
		return this.expressionProperties = JsonFormlyUtil.single(this.expressionProperties);
	}

	protected final JsonObject getValidators() {
		return this.validators = JsonFormlyUtil.single(this.validators);
	}

	@Override
	@SuppressWarnings("unchecked")
	public T self() {
		return (T) this;
	}

}