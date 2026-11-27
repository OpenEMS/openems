package io.openems.edge.core.appmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.openems.common.session.Language;
import io.openems.edge.core.appmanager.validator.ExpressionEvaluator;

/**
 * Assertions for a property of an {@link AbstractOpenemsAppWithProps}.
 *
 * @param <APP>       the app type
 * @param <PROPERTY>  the property type
 * @param <PARAMETER> the parameter type
 */
public final class PropertyAssertions<//
		APP extends AbstractOpenemsAppWithProps<APP, PROPERTY, PARAMETER>, //
		PROPERTY extends Type<PROPERTY, APP, PARAMETER>, //
		PARAMETER> {

	private final APP app;
	private final PROPERTY property;
	private final PARAMETER parameter;

	PropertyAssertions(APP app, PROPERTY property, PARAMETER parameter) {
		this.app = app;
		this.property = property;
		this.parameter = parameter;
	}

	/**
	 * Verifies that the property is visible for the given model value.
	 *
	 * @param modelProperty the model property
	 * @param value         the model value
	 * @return this
	 */
	public PropertyAssertions<APP, PROPERTY, PARAMETER> isVisibleWhen(PROPERTY modelProperty, Object value) {
		return this.assertVisibility(modelProperty, value, true);
	}

	/**
	 * Verifies that the property is hidden for the given model value.
	 *
	 * @param modelProperty the model property
	 * @param value         the model value
	 * @return this
	 */
	public PropertyAssertions<APP, PROPERTY, PARAMETER> isHiddenWhen(PROPERTY modelProperty, Object value) {
		return this.assertVisibility(modelProperty, value, false);
	}

	/**
	 * Verifies the default value of the property.
	 *
	 * @param expectedValue the expected default value
	 * @return this
	 */
	public PropertyAssertions<APP, PROPERTY, PARAMETER> hasDefaultValue(Object expectedValue) {
		final var defaultValueSupplier = this.property.def().getDefaultValue();
		assertNotNull(defaultValueSupplier, //
				() -> "Property [" + this.property.name() + "] of app [" + this.app.getAppId()
						+ "] has no default value");

		final JsonElement actualValue = getFieldValue(defaultValueSupplier, this.app, this.property, Language.DEFAULT,
				this.parameter);
		assertEquals(toJsonElement(expectedValue), actualValue, //
				() -> "Unexpected default value for property [" + this.property.name() + "] of app ["
						+ this.app.getAppId() + "]");
		return this;
	}

	/**
	 * Verifies that the property has no default value.
	 * 
	 * @return this
	 */
	public PropertyAssertions<APP, PROPERTY, PARAMETER> hasNoDefaultValue() {
		assertTrue(this.property.def().getDefaultValue() == null, //
				() -> "Property [" + this.property.name() + "] of app [" + this.app.getAppId()
						+ "] unexpectedly has a default value");
		return this;
	}

	/**
	 * Verifies that the property is required.
	 * 
	 * @return this
	 */
	public PropertyAssertions<APP, PROPERTY, PARAMETER> isRequired() {
		assertTrue(this.property.def().isRequired(), //
				() -> "Property [" + this.property.name() + "] of app [" + this.app.getAppId() + "] is not required");
		return this;
	}

	/**
	 * Verifies the min value.
	 * 
	 * @param expectedMin expected min value
	 * @return this
	 */
	public PropertyAssertions<APP, PROPERTY, PARAMETER> min(int expectedMin) {
		final var field = this.getField();
		final var templateOptions = field.getAsJsonObject("templateOptions");
		assertTrue(templateOptions.has("min"), //
				() -> "Property [" + this.property.name() + "] of app [" + this.app.getAppId() + "] has no min value");
		assertEquals(expectedMin, templateOptions.get("min").getAsInt(), //
				() -> "Unexpected min value for property [" + this.property.name() + "] of app [" + this.app.getAppId()
						+ "]");
		return this;
	}

	/**
	 * Verifies that value has max value.
	 * 
	 * @param expectedMax expected max value
	 * @return this
	 */
	public PropertyAssertions<APP, PROPERTY, PARAMETER> max(int expectedMax) {
		final var field = this.getField();
		final var templateOptions = field.getAsJsonObject("templateOptions");
		assertTrue(templateOptions.has("max"), //
				() -> "Property [" + this.property.name() + "] of app [" + this.app.getAppId() + "] has no max value");
		assertEquals(expectedMax, templateOptions.get("max").getAsInt(), //
				() -> "Unexpected max value for property [" + this.property.name() + "] of app [" + this.app.getAppId()
						+ "]");
		return this;
	}

	private JsonObject getField() {
		final var fieldSupplier = this.property.def().getField();
		assertNotNull(fieldSupplier, //
				() -> "Property [" + this.property.name() + "] of app [" + this.app.getAppId() + "] has no field");
		return getFieldValue(fieldSupplier, this.app, this.property, Language.DEFAULT, this.parameter).build();
	}

	private PropertyAssertions<APP, PROPERTY, PARAMETER> assertVisibility(PROPERTY modelProperty, Object value,
			boolean expectedVisible) {
		final var field = this.getField();
		final var expressionProperties = field.getAsJsonObject("expressionProperties");
		assertNotNull(expressionProperties, //
				() -> "Property [" + this.property.name() + "] of app [" + this.app.getAppId()
						+ "] has no expressionProperties");
		final var hideElement = expressionProperties.get("hide");
		assertTrue(hideElement != null && hideElement.isJsonPrimitive() && hideElement.getAsJsonPrimitive().isString(), //
				() -> "Property [" + this.property.name() + "] of app [" + this.app.getAppId()
						+ "] has no string hide expression");

		final var hideExpression = hideElement.getAsString();
		final var model = new JsonObject();
		model.add(modelProperty.name(), toJsonElement(value));
		final var actualVisible = !ExpressionEvaluator.evaluateExpression(hideExpression, model);
		final var message = "Unexpected visibility for property [" + this.property.name() + "] of app ["
				+ this.app.getAppId() + "] with [" + modelProperty.name() + "=" + value + "] and hide expression ["
				+ hideExpression + "] - expected visible: " + expectedVisible + ", actual visible: " + actualVisible;
		if (expectedVisible) {
			assertTrue(actualVisible, message);
		} else {
			assertFalse(actualVisible, message);
		}
		return this;
	}

	@SuppressWarnings("unchecked")
	private static <O> O getFieldValue(AppDef.FieldValuesSupplier<?, ?, ?, O> supplier, Object app, Object property,
			Language language, Object parameter) {
		final var typedSupplier = (AppDef.FieldValuesSupplier<Object, Object, Object, O>) supplier;
		return typedSupplier.get(app, property, language, parameter);
	}

	private static JsonElement toJsonElement(Object value) {
		return switch (value) {
		case null -> JsonNull.INSTANCE;
		case JsonElement element -> element;
		case Enum<?> enumValue -> new JsonPrimitive(enumValue.name());
		case String string -> new JsonPrimitive(string);
		case Number number -> new JsonPrimitive(number);
		case Boolean bool -> new JsonPrimitive(bool);
		case Character character -> new JsonPrimitive(character);
		default ->
			throw new IllegalArgumentException("Unsupported assertion value type: " + value.getClass().getName());
		};
	}
}
