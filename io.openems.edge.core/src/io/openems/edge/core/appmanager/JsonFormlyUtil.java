package io.openems.edge.core.appmanager;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.session.Language;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.enums.OptionsFactory;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.core.host.NetworkConfiguration;

/**
 * Source https://formly.dev/examples/introduction.
 */
public class JsonFormlyUtil {

	private JsonFormlyUtil() {
	}

	/**
	 * Creates a JsonObject Formly Checkbox Builder for the given enum.
	 *
	 * @param <T>      the type of the enum
	 * @param property the enum property
	 * @return a {@link CheckboxBuilder}
	 */
	public static <T extends Enum<T>> CheckboxBuilder buildCheckbox(T property) {
		return new CheckboxBuilder(toNameable(property));
	}

	/**
	 * Creates a JsonObject Formly Checkbox Builder for the given enum.
	 *
	 * @param nameable the {@link Nameable} property
	 * @return a {@link CheckboxBuilder}
	 */
	public static CheckboxBuilder buildCheckboxFromNameable(Nameable nameable) {
		return new CheckboxBuilder(nameable);
	}

	/**
	 * Creates a JsonObject Formly Input Builder for the given enum.
	 *
	 * @param <T>      the type of the enum
	 * @param property the enum property
	 * @return a {@link InputBuilder}
	 */
	public static <T extends Enum<T>> InputBuilder buildInput(T property) {
		return new InputBuilder(toNameable(property));
	}

	/**
	 * Creates a JsonObject Formly Input Builder for the given enum.
	 *
	 * @param nameable the {@link Nameable} property
	 * @return a {@link InputBuilder}
	 */
	public static InputBuilder buildInputFromNameable(Nameable nameable) {
		return new InputBuilder(nameable);
	}

	/**
	 * Creates a JsonObject Formly Select Builder for the given enum.
	 *
	 * @param <T>      the type of the enum
	 * @param property the enum property
	 * @return a {@link SelectBuilder}
	 */
	public static <T extends Enum<T>> SelectBuilder buildSelect(T property) {
		return new SelectBuilder(toNameable(property));
	}

	/**
	 * Creates a JsonObject Formly Select Builder for the given enum.
	 *
	 * @param nameable the {@link Nameable} property
	 * @return a {@link SelectBuilder}
	 */
	public static SelectBuilder buildSelectFromNameable(Nameable nameable) {
		return new SelectBuilder(nameable);
	}

	/**
	 * Creates a JsonObject Formly Range Builder for the given enum.
	 *
	 * @param <T>      the type of the enum
	 * @param property the enum property
	 * @return a {@link RangeBuilder}
	 */
	public static <T extends Enum<T>> RangeBuilder buildRange(T property) {
		return new RangeBuilder(toNameable(property));
	}

	/**
	 * Creates a JsonObject Formly Range Builder for the given enum.
	 *
	 * @param nameable the {@link Nameable} property
	 * @return a {@link RangeBuilder}
	 */
	public static RangeBuilder buildRangeFromNameable(Nameable nameable) {
		return new RangeBuilder(nameable);
	}

	/**
	 * Creates a JsonObject Formly Repeat Builder for the given enum.
	 *
	 * @param <T>      the type of the enum
	 * @param property the enum property
	 * @return a {@link RepeatBuilder}
	 */
	public static <T extends Enum<T>> RepeatBuilder buildRepeat(T property) {
		return new RepeatBuilder(toNameable(property));
	}

	/**
	 * Creates a JsonObject Formly Repeat Builder for the given enum.
	 *
	 * @param nameable the {@link Nameable} property
	 * @return a {@link RepeatBuilder}
	 */
	public static RepeatBuilder buildRepeat(Nameable nameable) {
		return new RepeatBuilder(nameable);
	}

	private static <T extends Enum<T>> Nameable toNameable(T property) {
		return new StaticNameable(property.name());
	}

	private static final class StaticNameable implements Nameable {

		private final String name;

		public StaticNameable(String name) {
			super();
			this.name = name;
		}

		@Override
		public String name() {
			return this.name;
		}

	}

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
	 * 	"defaultValue": "defaultValue"
	 * }
	 * </pre>
	 *
	 */
	public abstract static class FormlyBuilder<T extends FormlyBuilder<T>> implements OnlyIf<T>, Self<T> {

		protected final JsonObject jsonObject = new JsonObject();
		protected final JsonObject templateOptions = new JsonObject();
		private JsonObject expressionProperties = null;

		private FormlyBuilder(Nameable property) {
			this.setKey(property.name());
			this.setType(this.getType());
			this.setLabel(property.name());
		}

		private FormlyBuilder(DefaultEnum property) {
			this.setKey(property.name());
			this.setType(this.getType());
			this.setDefaultValue(property.getDefaultValue());
			this.setLabel(property.name());
		}

		private final T setType(String type) {
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

		/**
		 * Only shows the input if the given property is checked.
		 * 
		 * @param <PROPERTEY> the type of the property
		 * @param property    the property to be checked
		 * @return this
		 */
		public final <PROPERTEY extends Enum<PROPERTEY>> T onlyShowIfChecked(PROPERTEY property) {
			this.getExpressionProperties().addProperty("templateOptions.required", "model." + property.name());
			this.jsonObject.addProperty("hideExpression", "!model." + property.name());
			return this.self();
		}

		/**
		 * Only shows the input if the given property is not checked.
		 * 
		 * @param <PROPERTEY> the type of the property
		 * @param property    the property to be not checked
		 * @return this
		 */
		public final <PROPERTEY extends Enum<PROPERTEY>> T onlyShowIfNotChecked(PROPERTEY property) {
			this.getExpressionProperties().addProperty("templateOptions.required", "!model." + property.name());
			this.jsonObject.addProperty("hideExpression", "model." + property.name());
			return this.self();
		}

		public JsonObject build() {
			this.jsonObject.add("templateOptions", this.templateOptions);
			if (this.expressionProperties != null && this.expressionProperties.size() > 0) {
				this.jsonObject.add("expressionProperties", this.expressionProperties);
			}
			return this.jsonObject;
		}

		protected abstract String getType();

		protected final JsonObject getExpressionProperties() {
			if (this.expressionProperties == null) {
				this.expressionProperties = new JsonObject();
			}
			return this.expressionProperties;
		}

		@Override
		@SuppressWarnings("unchecked")
		public T self() {
			return (T) this;
		}

	}

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
	public static final class InputBuilder extends FormlyBuilder<InputBuilder> {

		public static enum Type {
			TEXT("text"), //
			PASSWORD("password"), //
			NUMBER("number"), //
			;

			private String formlyTypeName;

			private Type(String type) {
				this.formlyTypeName = type;
			}

			public String getFormlyTypeName() {
				return this.formlyTypeName;
			}
		}

		public static enum Validation {
			// TODO translation
			IP(NetworkConfiguration.PATTERN_INET4ADDRESS, "Input is not a valid IP Address!"), //
			;

			private String pattern;
			private String errorMsg;

			private Validation(String pattern, String errorMsg) {
				this.pattern = pattern;
				this.errorMsg = errorMsg;
			}

			public String getErrorMsg() {
				return this.errorMsg;
			}

			public String getPattern() {
				return this.pattern;
			}

		}

		private JsonObject validation = null;
		private Type type = Type.TEXT;

		private InputBuilder(Nameable property) {
			super(property);
		}

		private InputBuilder(DefaultEnum property) {
			super(property);
		}

		/**
		 * Sets the type of the input.
		 *
		 * <p>
		 * Default: {@link Type#TEXT}
		 *
		 * @param type to be set
		 * @return this
		 */
		public InputBuilder setInputType(Type type) {
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
			if (this.type != Type.NUMBER) {
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
			if (this.type != Type.NUMBER) {
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
			if (this.type == Type.NUMBER) {
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
			if (this.type == Type.NUMBER) {
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

		private InputBuilder setPattern(String pattern) {
			if (this.type != Type.TEXT) {
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
			if (this.type != Type.TEXT) {
				this.templateOptions.addProperty("type", this.type.getFormlyTypeName());
			}
			if (this.validation != null && this.validation.size() > 0) {
				this.jsonObject.add("validation", this.validation);
			}
			return super.build();
		}

		protected final JsonObject getValidation() {
			if (this.validation == null) {
				this.validation = new JsonObject();
			}
			return this.validation;
		}

	}

	public static final class RangeBuilder extends FormlyBuilder<RangeBuilder> {

		private RangeBuilder(Nameable property) {
			super(property);
		}

		private RangeBuilder(DefaultEnum property) {
			super(property);
		}

		/**
		 * Sets the min value of the input.
		 *
		 * @param min the min number that can be set
		 * @return this
		 */
		public RangeBuilder setMin(int min) {
			this.templateOptions.addProperty("min", min);
			return this;
		}

		/**
		 * Sets the max value of the input.
		 *
		 * @param max the max number that can be set
		 * @return this
		 */
		public RangeBuilder setMax(int max) {
			this.templateOptions.addProperty("max", max);
			return this;
		}

		@Override
		public JsonObject build() {
			this.templateOptions.add("attributes", JsonUtils.buildJsonObject() //
					.addProperty("pin", true) //
					.build());
			return super.build();
		}

		@Override
		protected String getType() {
			return "range";
		}

	}

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
	public static final class CheckboxBuilder extends FormlyBuilder<CheckboxBuilder> {

		private CheckboxBuilder(Nameable property) {
			super(property);
		}

		private CheckboxBuilder(DefaultEnum property) {
			super(property);
		}

		@Override
		protected String getType() {
			return "checkbox";
		}

	}

	/**
	 * A Builder for a Formly Select.
	 *
	 * <pre>
	 * {
	 * 	"key": "key",
	 * 	"type": "select",
	 * 	"templateOptions": {
	 * 		"label": "label",
	 * 		"required": true,
	 * 		"multiple": true,
	 * 		"options": [
	 * 			{
	 * 				"label": "label",
	 * 				"value": "value"
	 * 			}, ...
	 * 		]
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
	public static final class SelectBuilder extends FormlyBuilder<SelectBuilder> {

		public static final Function<OpenemsComponent, String> DEFAULT_COMPONENT_2_LABEL = t -> t.alias() == null
				|| t.alias().isEmpty() ? t.id() : t.id() + ": " + t.alias();
		public static final Function<OpenemsComponent, String> DEFAULT_COMPONENT_2_VALUE = OpenemsComponent::id;

		private SelectBuilder(Nameable property) {
			super(property);
		}

		private SelectBuilder(DefaultEnum property) {
			super(property);
		}

		public SelectBuilder setOptions(JsonArray options) {
			this.templateOptions.add("options", options);
			return this;
		}

		/**
		 * Note the {@link Map#entry(Object, Object)} does not return a
		 * {@link Comparable} Object so the {@link Set} can not be a {@link TreeSet}.
		 *
		 * @param items the options
		 * @return this
		 */
		public SelectBuilder setOptions(Set<Entry<String, String>> items) {
			return this.setOptions(items, t -> t, t -> t);
		}

		public <T, C> SelectBuilder setOptions(Set<Entry<T, C>> items, Function<T, String> item2Label,
				Function<C, String> item2Value) {
			var options = JsonUtils.buildJsonArray();
			items.stream().forEach(t -> {
				options.add(JsonUtils.buildJsonObject() //
						.addProperty("label", item2Label.apply(t.getKey())) //
						.addProperty("value", item2Value.apply(t.getValue())) //
						.build());
			});
			return this.setOptions(options.build());
		}

		public SelectBuilder setOptions(List<String> items) {
			return this.setOptions(items, t -> t, t -> t);
		}

		public <T> SelectBuilder setOptions(List<? extends T> items, Function<T, String> item2Label,
				Function<T, String> item2Value) {
			var options = JsonUtils.buildJsonArray();
			for (var item : items) {
				options.add(JsonUtils.buildJsonObject() //
						.addProperty("label", item2Label.apply(item)) //
						.addProperty("value", item2Value.apply(item)) //
						.build());
			}
			return this.setOptions(options.build());
		}

		public SelectBuilder setOptions(OptionsFactory factory, Language l) {
			return this.setOptions(factory.options(l));
		}

		/**
		 * Sets if more than one options can be selected.
		 *
		 * @param isMulti if more options can be selected
		 * @return this
		 */
		public SelectBuilder isMulti(boolean isMulti) {
			if (isMulti) {
				this.templateOptions.addProperty("multiple", isMulti);
			} else if (this.templateOptions.has("multiple")) {
				this.templateOptions.remove("multiple");
			}
			return this;
		}

		@Override
		protected String getType() {
			return "select";
		}

	}

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
	public static final class RepeatBuilder extends FormlyBuilder<RepeatBuilder> {

		private JsonObject fieldArray;

		private RepeatBuilder(Nameable property) {
			super(property);
		}

		private RepeatBuilder(DefaultEnum property) {
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

}
