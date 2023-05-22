package io.openems.edge.core.appmanager;

import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.openems.common.session.Language;
import io.openems.edge.core.appmanager.JsonFormlyUtil.FormlyBuilder;

/**
 * AppDef short for definition of a property for an app.
 * 
 * @param <APP>       the type of the app
 * @param <PROPERTY>  the type of the property
 * @param <PARAMETER> the type of the paramters
 */
public class AppDef<APP extends AbstractOpenemsAppWithProps<APP, PROPERTY, PARAMETER>, //
		PROPERTY extends Enum<PROPERTY> & Type<PROPERTY, APP, PARAMETER>, //
		PARAMETER extends Type.Parameter> //
		implements OnlyIf<AppDef<APP, PROPERTY, PARAMETER>>, Self<AppDef<APP, PROPERTY, PARAMETER>> {

	public static final class FieldValues<APP, PROPERTY, PARAMETER> {
		public final APP app;
		public final PROPERTY property;
		public final Language language;
		public final PARAMETER parameter;

		public FieldValues(APP app, PROPERTY property, Language language, PARAMETER parameter) {
			super();
			this.app = app;
			this.property = property;
			this.language = language;
			this.parameter = parameter;
		}

	}

	/**
	 * Function to get the label of the field.
	 */
	private Function<FieldValues<APP, PROPERTY, PARAMETER>, String> label;

	/**
	 * Function to get the description of the field.
	 */
	private Function<FieldValues<APP, PROPERTY, PARAMETER>, String> description;

	/**
	 * Function to get the default value of the field (can be any JsonElement =>
	 * JsonArray, JsonPrimitiv(Number, String, Boolean, Character).
	 */
	private Function<FieldValues<APP, PROPERTY, PARAMETER>, JsonElement> defaultValue;

	/**
	 * Function to get the {@link FormlyBuilder} for the input.
	 */
	private Function<FieldValues<APP, PROPERTY, PARAMETER>, FormlyBuilder<?>> field;

	/**
	 * Determines if the property should get visibly saved in the AppManager
	 * configuration.
	 */
	private boolean isAllowedToSave = true;

	/**
	 * Function for bidirectional binding of a component.
	 */
	private BiFunction<FieldValues<APP, PROPERTY, PARAMETER>, JsonObject, JsonElement> bidirectionalValue;

	/**
	 * Creates a {@link AppDef} of a subclass of an
	 * {@link AbstractOpenemsAppWithProps}.
	 * 
	 * @param <APP>       the type of the app
	 * @param <PROPERTY>  the type of the property
	 * @param <PARAMETER> the type of the parameter
	 * @param clazz       the {@link Class} of the
	 *                    {@link AbstractOpenemsAppWithProps}
	 * @return the {@link AppDef}
	 */
	public static final <APP extends AbstractOpenemsAppWithProps<APP, PROPERTY, PARAMETER>, //
			PROPERTY extends Enum<PROPERTY> & Type<PROPERTY, APP, PARAMETER>, //
			PARAMETER extends Type.Parameter> AppDef<APP, PROPERTY, PARAMETER> of(//
					final Class<APP> clazz //
	) {
		return new AppDef<APP, PROPERTY, PARAMETER>();
	}

	/**
	 * Sets if the property is allowed to be saved. May be used for passwords and
	 * apiKeys.
	 * 
	 * <p>
	 * DefaultValue: {@link Boolean#TRUE}
	 * 
	 * @param isAllowedToSave if the property is allowed to be saved
	 * @return this
	 */
	public AppDef<APP, PROPERTY, PARAMETER> setAllowedToSave(//
			final boolean isAllowedToSave //
	) {
		this.isAllowedToSave = isAllowedToSave;
		return this;
	}

	/**
	 * Gets if the property is allowed to be saved.
	 * 
	 * @return if the property is allowed to be saved
	 */
	public boolean isAllowedToSave() {
		return this.isAllowedToSave;
	}

	/**
	 * Sets the function as the label.
	 * 
	 * @param label the function to get the label
	 * @return this
	 */
	public final AppDef<APP, PROPERTY, PARAMETER> setLabel(//
			final Function<FieldValues<APP, PROPERTY, PARAMETER>, String> label //
	) {
		this.label = label;
		return this;
	}

	/**
	 * Sets the given string label as the return of the function label.
	 * 
	 * @param label the label to set
	 * @return this
	 */
	public final AppDef<APP, PROPERTY, PARAMETER> setLabel(//
			final String label //
	) {
		return this.setLabel(t -> label);
	}

	/**
	 * Sets the value of the translation as the label.
	 * 
	 * <p>
	 * Note: If this method is used {@link Type#translationBundleSupplier()} must be
	 * overridden and return a non null value.
	 * 
	 * @param key the key of the translation
	 * @return this
	 */
	public final AppDef<APP, PROPERTY, PARAMETER> setTranslatedLabel(//
			final String key //
	) {
		this.label = this.translate(key);
		return this;
	}

	/**
	 * Sets the value of the translation with the {@link OpenemsApp#getAppId()} as
	 * prefixed as the label.
	 * 
	 * <p>
	 * Note: If this method is used {@link Type#translationBundleSupplier()} must be
	 * overridden and return a non null value.
	 * 
	 * @param key the key of the translation
	 * @return this
	 */
	public final AppDef<APP, PROPERTY, PARAMETER> setTranslatedLabelWithAppPrefix(//
			final String key //
	) {
		this.label = this.translateWithAppPrefix(key);
		return this;
	}

	/**
	 * Sets the function as the description.
	 * 
	 * @param description the function to get the description
	 * @return this
	 */
	public final AppDef<APP, PROPERTY, PARAMETER> setDescription(//
			final Function<FieldValues<APP, PROPERTY, PARAMETER>, String> description //
	) {
		this.description = description;
		return this;
	}

	/**
	 * Sets the value of the translation as the description.
	 * 
	 * <p>
	 * Note: If this method is used {@link Type#translationBundleSupplier()} must be
	 * overridden and return a non null value.
	 * 
	 * @param key the key of the translation
	 * @return this
	 */
	public final AppDef<APP, PROPERTY, PARAMETER> setTranslatedDescription(//
			final String key //
	) {
		this.description = this.translate(key);
		return this;
	}

	/**
	 * Sets the value of the translation with the {@link OpenemsApp#getAppId()} as
	 * prefixed as the description.
	 * 
	 * <p>
	 * Note: If this method is used {@link Type#translationBundleSupplier()} must be
	 * overridden and return a non null value.
	 * 
	 * @param key the key of the translation
	 * @return this
	 */
	public final AppDef<APP, PROPERTY, PARAMETER> setTranslatedDescriptionWithAppPrefix(//
			final String key //
	) {
		this.description = this.translateWithAppPrefix(key);
		return this;
	}

	/**
	 * Sets the function as the defaultValue.
	 * 
	 * @param defaultValue the function to get the defaultValue
	 * @return this
	 */
	public final AppDef<APP, PROPERTY, PARAMETER> setDefaultValue(//
			final Function<FieldValues<APP, PROPERTY, PARAMETER>, JsonElement> defaultValue //
	) {
		this.defaultValue = defaultValue;
		return this;
	}

	private final <T> AppDef<APP, PROPERTY, PARAMETER> setDefaultValue(//
			final Function<T, JsonPrimitive> converter, //
			final Function<FieldValues<APP, PROPERTY, PARAMETER>, T> value //
	) {
		return this.setDefaultValue(t -> converter.apply(value.apply(t)));
	}

	/**
	 * Wraps the {@link String} in a {@link JsonPrimitive} and sets it as the
	 * default value with {@link AppDef#setDefaultValue(Function)}.
	 * 
	 * @param s the {@link String} as the default value
	 * @return this
	 */
	public final AppDef<APP, PROPERTY, PARAMETER> setDefaultValue(//
			final String s //
	) {
		return this.setDefaultValue(JsonPrimitive::new, ignore -> s);
	}

	/**
	 * Wraps the {@link Boolean} in a {@link JsonPrimitive} and sets it as the
	 * default value with {@link AppDef#setDefaultValue(Function)}.
	 * 
	 * @param b the {@link Boolean} as the default value
	 * @return this
	 */
	public final AppDef<APP, PROPERTY, PARAMETER> setDefaultValue(//
			final Boolean b //
	) {
		return this.setDefaultValue(JsonPrimitive::new, ignore -> b);
	}

	/**
	 * Wraps the {@link Number} in a {@link JsonPrimitive} and sets it as the
	 * default value with {@link AppDef#setDefaultValue(Function)}.
	 * 
	 * @param n the {@link Number} as the default value
	 * @return this
	 */
	public final AppDef<APP, PROPERTY, PARAMETER> setDefaultValue(//
			final Number n //
	) {
		return this.setDefaultValue(JsonPrimitive::new, ignore -> n);
	}

	/**
	 * Wraps the {@link Character} in a {@link JsonPrimitive} and sets it as the
	 * default value with {@link AppDef#setDefaultValue(Function)}.
	 * 
	 * @param c the {@link Character} as the default value
	 * @return this
	 */
	public final AppDef<APP, PROPERTY, PARAMETER> setDefaultValue(//
			final Character c //
	) {
		return this.setDefaultValue(JsonPrimitive::new, ignore -> c);
	}

	/**
	 * Sets the function as the defaultValue.
	 * 
	 * @param defaultValue the function to get the defaultValue
	 * @return this
	 */
	public final AppDef<APP, PROPERTY, PARAMETER> setDefaultValueString(//
			final Function<FieldValues<APP, PROPERTY, PARAMETER>, String> defaultValue //
	) {
		return this.setDefaultValue(JsonPrimitive::new, defaultValue);
	}

	/**
	 * Sets the function as the defaultValue.
	 * 
	 * @param defaultValue the function to get the defaultValue
	 * @return this
	 */
	public final AppDef<APP, PROPERTY, PARAMETER> setDefaultValueNumber(//
			final Function<FieldValues<APP, PROPERTY, PARAMETER>, Number> defaultValue //
	) {
		return this.setDefaultValue(JsonPrimitive::new, defaultValue);
	}

	/**
	 * Sets the function as the defaultValue.
	 * 
	 * @param defaultValue the function to get the defaultValue
	 * @return this
	 */
	public final AppDef<APP, PROPERTY, PARAMETER> setDefaultValueBoolean(//
			final Function<FieldValues<APP, PROPERTY, PARAMETER>, Boolean> defaultValue //
	) {
		return this.setDefaultValue(JsonPrimitive::new, defaultValue);
	}

	/**
	 * Sets the function as the defaultValue.
	 * 
	 * @param defaultValue the function to get the defaultValue
	 * @return this
	 */
	public final AppDef<APP, PROPERTY, PARAMETER> setDefaultValueCharacter(//
			final Function<FieldValues<APP, PROPERTY, PARAMETER>, Character> defaultValue //
	) {
		return this.setDefaultValue(JsonPrimitive::new, defaultValue);
	}

	/**
	 * Sets a function as the default value which returns the result of
	 * {@link OpenemsApp#getName(Language)}.
	 * 
	 * @return this
	 */
	public final AppDef<APP, PROPERTY, PARAMETER> setDefaultValueToAppName() {
		return this.setDefaultValueString(AppDef::fieldValuesToAppName);
	}

	private static final <APP extends AbstractOpenemsAppWithProps<APP, PROPERTY, PARAMETER>, //
			PROPERTY extends Enum<PROPERTY> & Type<PROPERTY, APP, PARAMETER>, //
			PARAMETER extends Type.Parameter> String fieldValuesToAppName(//
					final FieldValues<APP, PROPERTY, PARAMETER> f //
	) {
		return f.app.getName(f.language);
	}

	/**
	 * Sets the field of the input.
	 * 
	 * @param <T>                the type of the input
	 * @param fieldSupplier      the supplier to get the {@link FormlyBuilder}
	 * @param additionalSettings the additional settings on the input
	 * @return this
	 */
	public final <T extends FormlyBuilder<?>> AppDef<APP, PROPERTY, PARAMETER> setField(//
			final Function<PROPERTY, T> fieldSupplier, //
			final BiConsumer<FieldValues<APP, PROPERTY, PARAMETER>, T> additionalSettings //
	) {
		Objects.requireNonNull(fieldSupplier);
		this.field = (v) -> {
			final var field = fieldSupplier.apply(v.property);
			doIfPresent(v, this.label, field::setLabel);
			doIfPresent(v, this.description, field::setDescription);
			doIfPresent(v, this.defaultValue, field::setDefaultValue);
			if (additionalSettings != null) {
				additionalSettings.accept(v, field);
			}
			return field;
		};
		return this;
	}

	/**
	 * Sets the field of the input.
	 * 
	 * @param <T>           the type of the input
	 * @param fieldSupplier the supplier to get the {@link FormlyBuilder}
	 * @return this
	 */
	public final <T extends FormlyBuilder<?>> AppDef<APP, PROPERTY, PARAMETER> setField(//
			final Function<PROPERTY, T> fieldSupplier //
	) {
		return this.setField(fieldSupplier, null);
	}

	/**
	 * Executes the {@link Consumer} if the valueProvider can provide an instance
	 * with the given values.
	 * 
	 * @param <APP>         the type of the {@link OpenemsApp}
	 * @param <PROPERTY>    the type of the property
	 * @param <PARAMETER>   the type of the parameters
	 * @param <T>           the type of the provided instance
	 * @param values        the values to pass to the valueProvider
	 * @param valueProvider the provider of the instance
	 * @param consumer      the consumer to consume the instance
	 */
	private static final <APP, PROPERTY, PARAMETER, T> void doIfPresent(//
			final FieldValues<APP, PROPERTY, PARAMETER> values, //
			final Function<FieldValues<APP, PROPERTY, PARAMETER>, T> valueProvider, //
			final Consumer<T> consumer //
	) {
		if (valueProvider == null) {
			return;
		}
		var result = valueProvider.apply(values);
		consumer.accept(result);
	}

	private final Optional<ResourceBundle> usingTranslation(//
			final FieldValues<APP, PROPERTY, PARAMETER> v //
	) {
		return Optional.ofNullable(v.property.translationBundleSupplier()) //
				.map(t -> t.apply(v.parameter));
	}

	private final Function<FieldValues<APP, PROPERTY, PARAMETER>, String> translate(//
			final String key, //
			final Object... params //
	) {
		return v -> {
			return this.usingTranslation(v) //
					.map(b -> TranslationUtil.getTranslation(b, key, params)) //
					.orElse(null); //
		};
	}

	private final Function<FieldValues<APP, PROPERTY, PARAMETER>, String> translateWithAppPrefix(//
			final String key, //
			final Object... params //
	) {
		return v -> {
			return this.usingTranslation(v) //
					.map(b -> TranslationUtil.getTranslation(b, v.app.getApp().getAppId() + key, params)) //
					.orElse(null); //
		};
	}

	/**
	 * Gets the function to get the label.
	 * 
	 * @return the function
	 */
	public Function<FieldValues<APP, PROPERTY, PARAMETER>, String> getLabel() {
		return this.label;
	}

	/**
	 * Gets the function to get the description.
	 * 
	 * @return the function
	 */
	public Function<FieldValues<APP, PROPERTY, PARAMETER>, String> getDescription() {
		return this.description;
	}

	/**
	 * Gets the function to get the label.
	 * 
	 * @return the function
	 */
	public final Function<FieldValues<APP, PROPERTY, PARAMETER>, JsonElement> getDefaultValue() {
		return this.defaultValue;
	}

	/**
	 * Gets the function to get the {@link FormlyBuilder}.
	 * 
	 * @return the function
	 */
	public final Function<FieldValues<APP, PROPERTY, PARAMETER>, FormlyBuilder<?>> getField() {
		return this.field;
	}

	/**
	 * Gets the function to get the bidirectional value.
	 * 
	 * <p>
	 * This value may be obtained from a component
	 * 
	 * @return the function to get the value
	 */
	public BiFunction<FieldValues<APP, PROPERTY, PARAMETER>, JsonObject, JsonElement> getBidirectionalValue() {
		return this.bidirectionalValue;
	}

	@Override
	public AppDef<APP, PROPERTY, PARAMETER> self() {
		return this;
	}

	/**
	 * Binds a property bidirectional.
	 * 
	 * <p>
	 * The property itself will not be stored in the app configuration only in the
	 * component. If the user doesn't provide the value of a property and there is a
	 * bidirectional binding for it it will be filled up with the value of the
	 * bidirectional binding. If there is no component id in the configuration or
	 * the component doesn't exist or the property of the value is null then null is
	 * returned inside the bidirectional function.
	 * 
	 * @param propOfComponentId the key to get the component id from a configuration
	 * @param property          the property of the component
	 * @return this
	 */
	public AppDef<APP, PROPERTY, PARAMETER> bidirectional(//
			final PROPERTY propOfComponentId, //
			final String property //
	) {
		this.bidirectionalValue = (t, properties) -> {
			if (properties == null) {
				return null;
			}
			final var componentId = properties.get(propOfComponentId.name());
			if (componentId == null) {
				return null;
			}
			final var componentManager = t.app.componentManager;
			final var optionalComponent = componentManager.getEdgeConfig() //
					.getComponent(componentId.getAsString());
			return optionalComponent.map(component -> {
				var value = component.getProperty(property);
				return value.orElse(null);
			}).orElse(null);
		};
		// set allowedToSave automatically to false
		this.isAllowedToSave = false;
		return this.self();
	}

}