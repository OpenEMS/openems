package io.openems.edge.core.appmanager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.openems.common.channel.Unit;
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
	 * Creates a JsonObject Formly Input Builder for the given enum.
	 *
	 * @param <T>      the type of the enum
	 * @param property the enum property
	 * @return a {@link InputBuilder}
	 */
	public static <T extends Enum<T>> FieldGroupBuilder buildFieldGroup(T property) {
		return new FieldGroupBuilder(toNameable(property));
	}

	/**
	 * Creates a JsonObject Formly Input Builder for the given enum.
	 *
	 * @param nameable the {@link Nameable} property
	 * @return a {@link InputBuilder}
	 */
	public static FieldGroupBuilder buildFieldGroupFromNameable(Nameable nameable) {
		return new FieldGroupBuilder(nameable);
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

	/**
	 * Creates a JsonObject Formly Text Builder for the given enum.
	 *
	 * @return a {@link TextBuilder}
	 */
	public static TextBuilder buildText() {
		return new TextBuilder();
	}

	private static <T extends Enum<T>> Nameable toNameable(T property) {
		return new StaticNameable(property.name());
	}

	public static final class StaticNameable implements Nameable {

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

	public enum Wrappers {
		/**
		 * Wrapper for setting the default value dynamically based on the different
		 * {@link Case Cases}.
		 */
		DEFAULT_OF_CASES("formly-wrapper-default-of-cases"), //

		/**
		 * Wrapper for a panel.
		 */
		PANEL("panel"), //

		/**
		 * Input with a popup.
		 */
		SAFE_INPUT("formly-safe-input-wrapper"), //

		/**
		 * Input with unit.
		 */
		INPUT_WITH_UNIT("input-with-unit"), //
		;

		private final String wrapperClass;

		private Wrappers(String wrapperClass) {
			this.wrapperClass = wrapperClass;
		}

		public String getWrapperClass() {
			return this.wrapperClass;
		}

	}

	public static class DefaultValueOptions {

		private final Nameable field;
		private final List<Case> cases;

		public DefaultValueOptions(Nameable field, Case... cases) {
			super();
			this.field = field;
			this.cases = Arrays.stream(cases).collect(Collectors.toList());
		}

		/**
		 * Creates a {@link JsonObject} from this {@link DefaultValueOptions}.
		 * 
		 * @return the {@link JsonObject}
		 */
		public JsonObject toJsonObject() {
			return JsonUtils.buildJsonObject() //
					.addProperty("field", this.field.name()) //
					.add("cases", this.cases.stream().map(Case::toJsonObject).collect(JsonUtils.toJsonArray())) //
					.build();
		}

	}

	public static class Case {
		private final JsonElement value;
		private final JsonElement defaultValue;

		public Case(JsonElement value, JsonElement defaultValue) {
			super();
			this.value = value;
			this.defaultValue = defaultValue;
		}

		public Case(String value, String defaultValue) {
			this(new JsonPrimitive(value), new JsonPrimitive(defaultValue));
		}

		public Case(Number value, String defaultValue) {
			this(new JsonPrimitive(value), new JsonPrimitive(defaultValue));
		}

		/**
		 * Creates a {@link JsonObject} from this {@link Case}.
		 * 
		 * @return the {@link JsonObject}
		 */
		public JsonObject toJsonObject() {
			return JsonUtils.buildJsonObject() //
					.add("case", this.value) //
					.add("defaultValue", this.defaultValue) //
					.build();
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
	 * 	"defaultValue": "defaultValue",
	 *  "wrappers": []{@link Wrappers}
	 * }
	 * </pre>
	 *
	 */
	public abstract static class FormlyBuilder<T extends FormlyBuilder<T>> implements OnlyIf<T>, Self<T> {

		protected final JsonObject jsonObject = new JsonObject();
		protected final JsonObject templateOptions = new JsonObject();
		private JsonObject expressionProperties = null;
		private final List<String> wrappers = new ArrayList<>();
		private JsonObject validators = null;

		private FormlyBuilder(Nameable property) {
			this.setType(this.getType());
			if (property == null) {
				return;
			}
			this.setKey(property.name());
			this.setLabel(property.name());
		}

		private FormlyBuilder(DefaultEnum property) {
			this.setKey(property.name());
			this.setType(this.getType());
			this.setDefaultValue(property.getDefaultValue());
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
		 * Only shows the current input if the input of the given property is checked.
		 * 
		 * @param nameable the {@link Nameable}
		 * @return this
		 */
		public final T onlyShowIfChecked(Nameable nameable) {
			return this.onlyShowIf(ExpressionBuilder.of(nameable));
		}

		/**
		 * Only shows the current input if the input of the given property is not
		 * checked.
		 * 
		 * @param nameable the {@link Nameable}
		 * @return this
		 */
		public final T onlyShowIfNotChecked(Nameable nameable) {
			return this.onlyShowIf(ExpressionBuilder.of(nameable).negotiate());
		}

		/**
		 * Only shows the current input if the value of the input of the given property
		 * is the same as the given value.
		 * 
		 * @param nameable the {@link Nameable}
		 * @param value    the value to validate against
		 * @return this
		 */
		public final T onlyShowIfValueEquals(Nameable nameable, String value) {
			return this.onlyShowIf(ExpressionBuilder.of(nameable, ExpressionBuilder.Operator.EQ, value));
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
		 * @param expressionBuilder the {@link ExpressionBuilder} to set
		 * @return this
		 */
		public final T onlyShowIf(ExpressionBuilder expressionBuilder) {
			return this.onlyShowIf(expressionBuilder.toString());
		}

		public final T setLabelExpression(ExpressionBuilder expression, String trueLabel, String falseLabel) {
			this.getExpressionProperties().addProperty("templateOptions.label",
					expression.toString() + " ? '" + trueLabel + "' : '" + falseLabel + "'");
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

		public T setCustomValidation(String name, ExpressionBuilder expression, String errorMessage,
				Nameable propertyToShowError) {
			this.getValidators().add(name, JsonUtils.buildJsonObject() //
					.addProperty("expressionString", expression.toString()) //
					.addProperty("message", errorMessage) //
					.addProperty("errorPath", propertyToShowError.name()) //
					.build());
			return this.self();
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
			return this.expressionProperties = single(this.expressionProperties);
		}

		protected final JsonObject getValidators() {
			return this.validators = single(this.validators);
		}

		@Override
		@SuppressWarnings("unchecked")
		public T self() {
			return (T) this;
		}

	}

	public static final class ExpressionBuilder {

		public static enum Operator {
			// Equals
			EQ("=="), //
			// Not-Equals
			NEQ("!="), //
			// Greater-Than-Equals
			GTE(">="), //
			// Greater-Than
			GT(">"), //
			// Lower-Than-Equals
			LTE("<="), //
			// Lower-Than
			LT("<"), //
			;

			private final String operation;

			private Operator(String operation) {
				this.operation = operation;
			}

			public String getOperation() {
				return this.operation;
			}
		}

		private StringBuilder sb;

		/**
		 * Creates a {@link ExpressionBuilder} where the input of the given property
		 * gets validated against the given value.
		 * 
		 * @param nameable the {@link Nameable}
		 * @param operator the {@link Operator} to validate against the value
		 * @param value    the value to validate against
		 * @return the {@link ExpressionBuilder}
		 */
		public static final ExpressionBuilder of(Nameable nameable, Operator operator, String value) {
			return new ExpressionBuilder(expressionOf(nameable, operator, value));
		}

		/**
		 * Creates a {@link ExpressionBuilder} where the value of the given nameable
		 * gets validated against the value of the otherNameable.
		 * 
		 * @param nameable      the first property
		 * @param operator      the validation {@link Operator}
		 * @param otherNameable the second property
		 * @return the {@link ExpressionBuilder}
		 */
		public static final ExpressionBuilder of(Nameable nameable, Operator operator, Nameable otherNameable) {
			return new ExpressionBuilder(expressionOf(nameable, operator, otherNameable));
		}

		/**
		 * Creates a {@link ExpressionBuilder} where the value of the input of the given
		 * property gets validated.
		 * 
		 * @param nameable the {@link Nameable}
		 * @return the {@link ExpressionBuilder}
		 */
		public static final ExpressionBuilder of(Nameable nameable) {
			return new ExpressionBuilder(expressionOf(nameable));
		}

		/**
		 * Creates a {@link ExpressionBuilder} where the value of the input of the given
		 * property gets validated to be in the given values.
		 * 
		 * @param nameable the {@link Nameable}
		 * @param values   the values the value of the nameable should be in
		 * @return the {@link ExpressionBuilder}
		 */
		public static final ExpressionBuilder ofIn(Nameable nameable, String... values) {
			return new ExpressionBuilder(expressionOfIn(nameable, values));
		}

		/**
		 * Creates a {@link ExpressionBuilder} where the value of the input of the given
		 * property gets validated to not be in the given values.
		 * 
		 * @param nameable the {@link Nameable}
		 * @param values   the values the value of the nameable should not be in
		 * @return the {@link ExpressionBuilder}
		 */
		public static final ExpressionBuilder ofNotIn(Nameable nameable, String... values) {
			return new ExpressionBuilder(expressionOfNotIn(nameable, values));
		}

		private ExpressionBuilder(String baseExpression) {
			this.sb = new StringBuilder(baseExpression);
		}

		/**
		 * Combines the current expression with the given expression with an and.
		 * 
		 * @param nameable the {@link Nameable}
		 * @param operator the {@link Operator}
		 * @param value    the value to validate the input of the property
		 * @return this
		 */
		public ExpressionBuilder and(Nameable nameable, Operator operator, String value) {
			return this.and(expressionOf(nameable, operator, value));
		}

		/**
		 * Combines the current expression with the given expression with an and.
		 * 
		 * @param nameable the {@link Nameable}
		 * @return this
		 */
		public ExpressionBuilder and(Nameable nameable) {
			return this.and(expressionOf(nameable));
		}

		/**
		 * Combines the current expression with the given expression with an and.
		 * 
		 * @param builder the other expression
		 * @return this
		 */
		public ExpressionBuilder and(ExpressionBuilder builder) {
			return this.and(builder.toString());
		}

		private final ExpressionBuilder and(String expression) {
			this.sb.append(" && ");
			this.sb.append(expression);
			return this;
		}

		/**
		 * Combines the current expression with the given expression with an or.
		 * 
		 * @param nameable the {@link Nameable}
		 * @param operator the {@link Operator}
		 * @param value    the value to validate the input of the property
		 * @return this
		 */
		public ExpressionBuilder or(Nameable nameable, Operator operator, String value) {
			return this.or(expressionOf(nameable, operator, value));
		}

		/**
		 * Combines the current expression with the given expression with an or.
		 * 
		 * @param nameable the {@link Nameable}
		 * @return this
		 */
		public ExpressionBuilder or(Nameable nameable) {
			return this.or(expressionOf(nameable));
		}

		/**
		 * Combines the current expression with the given expression with an or.
		 * 
		 * @param builder the other expression
		 * @return this
		 */
		public ExpressionBuilder or(ExpressionBuilder builder) {
			return this.or(builder.toString());
		}

		private final ExpressionBuilder or(String expression) {
			this.sb.append(" || ");
			this.sb.append(expression);
			return this;
		}

		private static final String expressionOfNotIn(Nameable nameable, String... values) {
			if (values.length == 0) {
				return "";
			}
			ExpressionBuilder expression = null;
			for (var item : values) {
				if (expression == null) {
					expression = ExpressionBuilder.of(nameable, Operator.NEQ, item);
					continue;
				}
				expression.and(nameable, Operator.NEQ, item);
			}

			return expression.toString();
		}

		private static final String expressionOfIn(Nameable nameable, String... values) {
			if (values.length == 0) {
				return "";
			}
			ExpressionBuilder expression = null;
			for (var item : values) {
				if (expression == null) {
					expression = ExpressionBuilder.of(nameable, Operator.EQ, item);
					continue;
				}
				expression.or(nameable, Operator.EQ, item);
			}

			return expression.toString();
		}

		private static final String expressionOf(Nameable nameable, Operator operator, String value) {
			return "model." + nameable.name() + " " + operator.getOperation() + " '" + value + "'";
		}

		private static final String expressionOf(Nameable nameable, Operator operator, Nameable secondNameable) {
			return "model." + nameable.name() + " " + operator.getOperation() + " model." + secondNameable.name();
		}

		private static final String expressionOf(Nameable nameable) {
			return "model." + nameable.name();
		}

		private ExpressionBuilder addToFront(String string) {
			final var nextBuilder = new StringBuilder(string);
			this.sb = nextBuilder.append(this.sb);
			return this;
		}

		/**
		 * Puts the current statement in brackets.
		 * 
		 * @return this
		 */
		public ExpressionBuilder inBrackets() {
			this.sb.append(")");
			return this.addToFront("(");
		}

		/**
		 * Negotiates the whole expression.
		 * 
		 * @return this
		 */
		public ExpressionBuilder negotiate() {
			this.sb.append(")");
			return this.addToFront("!(");
		}

		@Override
		public String toString() {
			return this.sb.toString();
		}

	}

	public static final class FieldGroupBuilder extends FormlyBuilder<FieldGroupBuilder> {

		private JsonArray fieldGroup;

		private FieldGroupBuilder(Nameable property) {
			super(property);
		}

		private FieldGroupBuilder(DefaultEnum property) {
			super(property);
		}

		public FieldGroupBuilder setFieldGroup(JsonArray fieldGroup) {
			this.fieldGroup = fieldGroup;
			return this.self();
		}

		public FieldGroupBuilder setPopupInput(Nameable displayValue) {
			this.addWrapper(Wrappers.SAFE_INPUT);
			this.templateOptions.addProperty("pathToDisplayValue", displayValue.name());
			return this;
		}

		@Override
		protected String getType() {
			return null;
		}

		@Override
		public JsonObject build() {
			final var object = super.build();
			final var templateOptions = object.get("templateOptions").getAsJsonObject();
			templateOptions.remove("required");
			JsonUtils.getAsOptionalJsonObject(object, "expressionProperties") //
					.map(t -> t.remove("templateOptions.required"));
			object.add("fieldGroup", this.fieldGroup);
			return JsonUtils.buildJsonObject() //
					.add("hideExpression", object.remove("hideExpression")) //
					.add("fieldGroup", JsonUtils.buildJsonArray() //
							.add(object) //
							.build())
					.build();
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

		public InputBuilder setUnit(Unit unit, Language l) {
			var unitString = switch (unit) {
			case WATT -> TranslationUtil.getTranslation(AbstractOpenemsApp.getTranslationBundle(l), "watt");
			default -> unit.getSymbol();
			};
			this.templateOptions.addProperty("unit", unitString);
			this.addWrapper(Wrappers.INPUT_WITH_UNIT);
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
			return this.validation = single(this.validation);
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

		private JsonObject validation;

		private CheckboxBuilder(Nameable property) {
			super(property);
		}

		private CheckboxBuilder(DefaultEnum property) {
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
			return this.validation = single(this.validation);
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

		public static final Function<OpenemsComponent, JsonElement> DEFAULT_COMPONENT_2_LABEL = t -> new JsonPrimitive(
				t.alias() == null || t.alias().isEmpty() ? t.id() : t.id() + ": " + t.alias());
		public static final Function<OpenemsComponent, JsonElement> DEFAULT_COMPONENT_2_VALUE = t -> new JsonPrimitive(
				t.id());

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
			return this.setOptions(items, JsonPrimitive::new, JsonPrimitive::new);
		}

		public <T> SelectBuilder setOptions(List<? extends T> items, Function<T, JsonElement> item2Label,
				Function<T, JsonElement> item2Value) {
			var options = JsonUtils.buildJsonArray();
			for (var item : items) {
				options.add(JsonUtils.buildJsonObject() //
						.add("label", item2Label.apply(item)) //
						.add("value", item2Value.apply(item)) //
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

	public static final class TextBuilder extends FormlyBuilder<TextBuilder> {

		private TextBuilder() {
			super((Nameable) null);
		}

		public TextBuilder setText(String text) {
			return this.setDescription(text);
		}

		@Override
		protected String getType() {
			return "text";
		}

	}

	private static final JsonObject single(JsonObject o) {
		if (o == null) {
			o = new JsonObject();
		}
		return o;
	}

}
