package io.openems.edge.core.appmanager;

import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.openems.common.session.Language;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.JsonFormlyUtil.FormlyBuilder;
import io.openems.edge.core.appmanager.Type.Parameter;
import io.openems.edge.core.appmanager.Type.Parameter.BundleParameter;

/**
 * AppDef short for definition of a property for an app.
 * 
 * @param <APP>       the type of the app
 * @param <PROPERTY>  the type of the property
 * @param <PARAMETER> the type of the paramters
 */
public class AppDef<APP extends OpenemsApp, //
		PROPERTY extends Nameable, //
		PARAMETER extends Type.Parameter> //
		implements OnlyIf<AppDef<APP, PROPERTY, PARAMETER>>, Self<AppDef<APP, PROPERTY, PARAMETER>> {

	private static final Logger LOG = LoggerFactory.getLogger(AppDef.class);

	/**
	 * Functional interface function with field values.
	 * 
	 * @param <A> the type of the app
	 * @param <P> the type of the property
	 * @param <M> the type of the parameter
	 * @param <O> the type of the return parameter
	 */
	@FunctionalInterface
	public static interface FieldValuesSupplier<A, P, M, O> {

		/**
		 * A function with the values of the current field.
		 * 
		 * @param app       the current app
		 * @param property  the current property
		 * @param l         the current language
		 * @param parameter the current provided parameters
		 * @return the output of the function
		 */
		public O get(A app, P property, Language l, M parameter);

	}

	@FunctionalInterface
	public static interface FieldValuesPredicate<A, P, M> {

		public static FieldValuesPredicate<? super Object, ? super Object, ? super Object> ALWAYS_TRUE = (app, property,
				l, parameter) -> true;
		public static FieldValuesPredicate<? super Object, ? super Object, ? super Object> ALWAYS_FALSE = (app,
				property, l, parameter) -> false;

		/**
		 * A function with the values of the current field.
		 * 
		 * @param app       the current app
		 * @param property  the current property
		 * @param l         the current language
		 * @param parameter the current provided parameters
		 * @return true if the test was successful
		 */
		public boolean test(A app, P property, Language l, M parameter);

	}

	/**
	 * Functional interface with field values and a extra parameter and and return
	 * value.
	 *
	 * @param <A> the type of the app
	 * @param <P> the type of the property
	 * @param <M> the type of the parameter
	 * @param <T> the type of the additional parameter
	 * @param <O> the type of the return parameter
	 */
	@FunctionalInterface
	public static interface FieldValuesFunction<A, P, M, T, O> {

		/**
		 * A function with the values of the current field and one extra parameter.
		 * 
		 * @param app       the current app
		 * @param property  the current property
		 * @param l         the current language
		 * @param parameter the current provided parameters
		 * @param first     the extra parameter
		 * @return the output of the function
		 */
		public O apply(A app, P property, Language l, M parameter, T first);

	}

	/**
	 * Functional interface with field values and a extra parameter.
	 *
	 * @param <A> the type of the app
	 * @param <P> the type of the property
	 * @param <M> the type of the parameter
	 * @param <T> the type of the additional parameter
	 */
	@FunctionalInterface
	public static interface FieldValuesConsumer<A, P, M, T> {

		/**
		 * A Consumer with the values of the current field and one extra parameter.
		 * 
		 * @param app       the current app
		 * @param property  the current property
		 * @param l         the current language
		 * @param parameter the current provided parameters
		 * @param first     the extra parameter
		 */
		public void accept(A app, P property, Language l, M parameter, T first);

	}

	/**
	 * Function to get the label of the field.
	 */
	private FieldValuesSupplier<? super APP, ? super PROPERTY, ? super PARAMETER, String> label;

	/**
	 * Function to get the description of the field.
	 */
	private FieldValuesSupplier<? super APP, ? super PROPERTY, ? super PARAMETER, String> description;

	/**
	 * Function to get the default value of the field (can be any JsonElement =>
	 * JsonArray, JsonPrimitiv(Number, String, Boolean, Character).
	 */
	private FieldValuesSupplier<? super APP, ? super PROPERTY, ? super PARAMETER, JsonElement> defaultValue;

	/**
	 * Function to get the {@link FormlyBuilder} for the input.
	 */
	private FieldValuesSupplier<? super APP, ? super PROPERTY, ? super PARAMETER, FormlyBuilder<?>> field;

	/**
	 * Determines if the field gets added to the {@link AppAssistant}.
	 */
	private FieldValuesPredicate<? super APP, ? super PROPERTY, ? super PARAMETER> shouldAddField = FieldValuesPredicate.ALWAYS_TRUE;

	/**
	 * Determines if the property should get visibly saved in the AppManager
	 * configuration.
	 */
	private boolean isAllowedToSave = true;

	/**
	 * Function for bidirectional binding of a component.
	 */
	private FieldValuesFunction<? super APP, ? super PROPERTY, ? super PARAMETER, JsonObject, JsonElement> bidirectionalValue;

	/**
	 * Function to get the {@link ResourceBundle} for translations.
	 */
	private Function<? super PARAMETER, ResourceBundle> translationBundleSupplier;

	/**
	 * Creates a {@link AppDef} with the componentId as the default value.
	 * 
	 * @param <APP>       the type of the {@link OpenemsApp}
	 * @param <PROPERTY>  the type of the {@link Nameable}
	 * @param <PARAMETER> the type of the {@link Parameter}
	 * @param componentId the id of the component
	 * @return the {@link AppDef}
	 */
	public static final <APP extends OpenemsApp, //
			PROPERTY extends Nameable, //
			PARAMETER extends Type.Parameter> AppDef<APP, PROPERTY, PARAMETER> componentId(String componentId) {
		return new AppDef<APP, PROPERTY, PARAMETER>() //
				.setDefaultValue(componentId);
	}

	/**
	 * Creates a {@link AppDef} of a subclass of an
	 * {@link AbstractOpenemsAppWithProps}.
	 * 
	 * @param <APP>       the type of the app
	 * @param <PROPERTY>  the type of the property
	 * @param <PARAMETER> the type of the parameter
	 * @return the {@link AppDef}
	 */
	public static final <APP extends OpenemsApp, //
			PROPERTY extends Nameable, //
			PARAMETER extends Type.Parameter> AppDef<APP, PROPERTY, PARAMETER> of() {
		return new AppDef<APP, PROPERTY, PARAMETER>();
	}

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
			PROPERTY extends Type<PROPERTY, APP, PARAMETER> & Nameable, //
			PARAMETER extends Type.Parameter.BundleParameter> AppDef<APP, PROPERTY, PARAMETER> of(//
					final Class<APP> clazz //
	) {
		return new AppDef<APP, PROPERTY, PARAMETER>() //
				.setTranslationBundleSupplier(BundleParameter::getBundle);
	}

	/**
	 * Creates a {@link AppDef} of a subclass of an {@link OpenemsApp}.
	 * 
	 * @param <APP>         the type of the app
	 * @param <PROPERTY>    the type of the property
	 * @param <PARAMETER>   the type of the parameter
	 * @param propertyClass the {@link Class} of the PROPERTY
	 * @return the {@link AppDef}
	 */
	public static final <APP extends OpenemsApp, //
			PROPERTY extends Nameable & Type<? extends PROPERTY, ? extends APP, ? extends PARAMETER>, //
			PARAMETER extends Type.Parameter> AppDef<APP, PROPERTY, PARAMETER> genericOf(//
					final Class<PROPERTY> propertyClass //
	) {
		return new AppDef<APP, PROPERTY, PARAMETER>();
	}

	/**
	 * Creates a copy of the otherDef.
	 * 
	 * @param <APP>         the type of the app
	 * @param <PROPERTY>    the type of the property
	 * @param <PARAMETER>   the type of the parameter
	 * @param propertyClass the class of the current property
	 * @param otherDef      the other {@link AppDef}
	 * @return the new {@link AppDef} //
	 */
	public static final <//
			APP extends AbstractOpenemsAppWithProps<APP, PROPERTY, PARAMETER> & OpenemsApp, //
			PROPERTY extends Nameable & Type<PROPERTY, APP, PARAMETER>, //
			PARAMETER extends Type.Parameter.BundleParameter> //
	AppDef<APP, PROPERTY, PARAMETER> copyOf(//
			final Class<PROPERTY> propertyClass, //
			final AppDef<OpenemsApp, Nameable, Type.Parameter.BundleParameter> otherDef //
	) {
		return copyOfGeneric(otherDef);
	}

	/**
	 * Creates a copy of the otherDef. This method is often used instead of the
	 * {@link AppDef#copyOfGeneric(AppDef)} because the return type doesn't have to
	 * be set explicit if a method call is appended to the result of the method.
	 * 
	 * @param <APP>        the type of the app
	 * @param <PROPERTY>   the type of the property
	 * @param <PARAMETER>  the type of the parameter
	 * @param <APPO>       the type of the app from the otherDef
	 * @param <PROPERTYO>  the type of the property from the otherDef
	 * @param <PARAMETERO> the type of the parameter from the otherDef
	 * @param otherDef     the other {@link AppDef}
	 * @param consumer     the {@link Consumer} to set attributes of the
	 *                     {@link AppDef}
	 * @return the new {@link AppDef}
	 */
	public static final <//
			APP extends APPO, //
			PROPERTY extends PROPERTYO, //
			PARAMETER extends PARAMETERO, //
			APPO extends OpenemsApp, //
			PROPERTYO extends Nameable, //
			PARAMETERO extends Type.Parameter> AppDef<APP, PROPERTY, PARAMETER> copyOfGeneric(//
					final AppDef<APPO, PROPERTYO, PARAMETERO> otherDef, //
					Consumer<AppDef<APP, PROPERTY, PARAMETER>> consumer //
	) {
		var a = AppDef.<APP, PROPERTY, PARAMETER, APPO, PROPERTYO, PARAMETERO>copyOfGeneric(otherDef);
		consumer.accept(a);
		return a;
	}

	/**
	 * Creates a copy of the otherDef.
	 * 
	 * @param <APP>        the type of the app
	 * @param <PROPERTY>   the type of the property
	 * @param <PARAMETER>  the type of the parameter
	 * @param <APPO>       the type of the app from the otherDef
	 * @param <PROPERTYO>  the type of the property from the otherDef
	 * @param <PARAMETERO> the type of the parameter from the otherDef
	 * @param otherDef     the other {@link AppDef}
	 * @return the new {@link AppDef}
	 */
	public static final <//
			APP extends APPO, //
			PROPERTY extends PROPERTYO, //
			PARAMETER extends PARAMETERO, //
			APPO extends OpenemsApp, //
			PROPERTYO extends Nameable, //
			PARAMETERO extends Type.Parameter> AppDef<APP, PROPERTY, PARAMETER> copyOfGeneric(//
					final AppDef<APPO, PROPERTYO, PARAMETERO> otherDef //
	) {
		final var def = new AppDef<APP, PROPERTY, PARAMETER>();
		def.translationBundleSupplier = otherDef.translationBundleSupplier;
		def.label = otherDef.label;
		def.description = otherDef.description;
		def.defaultValue = otherDef.defaultValue;
		def.field = otherDef.field;
		def.shouldAddField = otherDef.shouldAddField;
		def.isAllowedToSave = otherDef.isAllowedToSave;
		def.bidirectionalValue = otherDef.bidirectionalValue;
		return def;
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

	public final AppDef<APP, PROPERTY, PARAMETER> setTranslationBundleSupplier(//
			final Function<PARAMETER, ResourceBundle> bundleSupplier //
	) {
		this.translationBundleSupplier = bundleSupplier;
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
			final FieldValuesSupplier<? super APP, ? super PROPERTY, ? super PARAMETER, String> label //
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
		return this.setLabel((app, prop, t, param) -> label);
	}

	/**
	 * Sets the value of the translation as the label.
	 * 
	 * <p>
	 * Note: If this method is used {@link Type#translationBundleSupplier()} must be
	 * overridden and return a non null value.
	 * 
	 * @param key    the key of the translation
	 * @param params the parameter of the translation
	 * @return this
	 */
	public final AppDef<APP, PROPERTY, PARAMETER> setTranslatedLabel(//
			final String key, //
			final Object... params //
	) {
		this.label = this.translate(key, params);
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
	 * @param key    the key of the translation
	 * @param params the parameter of the translation
	 * @return this
	 */
	public final AppDef<APP, PROPERTY, PARAMETER> setTranslatedLabelWithAppPrefix(//
			final String key, //
			final Object... params //
	) {
		this.label = this.translateWithAppPrefix(key, params);
		return this;
	}

	/**
	 * Sets the function as the description.
	 * 
	 * @param description the function to get the description
	 * @return this
	 */
	public final AppDef<APP, PROPERTY, PARAMETER> setDescription(//
			final FieldValuesSupplier<? super APP, ? super PROPERTY, ? super PARAMETER, String> description //
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
	 * @param key    the key of the translation
	 * @param params the parameter of the translation
	 * @return this
	 */
	public final AppDef<APP, PROPERTY, PARAMETER> setTranslatedDescriptionWithAppPrefix(//
			final String key, //
			final Object... params //
	) {
		this.description = this.translateWithAppPrefix(key, params);
		return this;
	}

	/**
	 * Sets the function as the defaultValue.
	 * 
	 * @param defaultValue the function to get the defaultValue
	 * @return this
	 */
	public final AppDef<APP, PROPERTY, PARAMETER> setDefaultValue(//
			final FieldValuesSupplier<? super APP, ? super PROPERTY, ? super PARAMETER, JsonElement> defaultValue //
	) {
		this.defaultValue = defaultValue;
		return this;
	}

	private final <T> AppDef<APP, PROPERTY, PARAMETER> setDefaultValue(//
			final Function<T, JsonPrimitive> converter, //
			final FieldValuesSupplier<? super APP, ? super PROPERTY, ? super PARAMETER, T> value //
	) {
		return this.setDefaultValue((app, prop, l, param) -> converter.apply(value.get(app, prop, l, param)));
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
		return this.setDefaultValue(JsonPrimitive::new, (app, prop, l, param) -> s);
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
		return this.setDefaultValue(JsonPrimitive::new, (app, prop, l, param) -> b);
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
		return this.setDefaultValue(JsonPrimitive::new, (app, prop, l, param) -> n);
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
		return this.setDefaultValue(JsonPrimitive::new, (app, prop, l, param) -> c);
	}

	/**
	 * Sets the function as the defaultValue.
	 * 
	 * @param defaultValue the function to get the defaultValue
	 * @return this
	 */
	public final AppDef<APP, PROPERTY, PARAMETER> setDefaultValueString(//
			final FieldValuesSupplier<? super APP, ? super PROPERTY, ? super PARAMETER, String> defaultValue //
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
			final FieldValuesSupplier<? super APP, ? super PROPERTY, ? super PARAMETER, Number> defaultValue //
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
			final FieldValuesSupplier<? super APP, ? super PROPERTY, ? super PARAMETER, Boolean> defaultValue //
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
			final FieldValuesSupplier<? super APP, ? super PROPERTY, ? super PARAMETER, Character> defaultValue //
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

	private static final <APP extends OpenemsApp, //
			PROPERTY, //
			PARAMETER> //
	String fieldValuesToAppName(//
			final APP app, //
			final PROPERTY prop, //
			final Language language, //
			final PARAMETER param //
	) {
		return app.getName(language);
	}

	public AppDef<APP, PROPERTY, PARAMETER> setFieldFunction(//
			final FieldValuesSupplier<? super APP, ? super PROPERTY, ? super PARAMETER, FormlyBuilder<?>> field //
	) {
		this.field = field;
		return this;
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
			final FieldValuesConsumer<APP, PROPERTY, PARAMETER, T> additionalSettings //
	) {
		Objects.requireNonNull(fieldSupplier);
		this.field = (app, property, language, parameter) -> {
			final var field = fieldSupplier.apply(property);
			if (additionalSettings != null) {
				additionalSettings.accept(app, property, language, parameter, field);
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

	public AppDef<APP, PROPERTY, PARAMETER> setAutoGenerateField(boolean autoGenerateField) {
		this.shouldAddField = autoGenerateField ? FieldValuesPredicate.ALWAYS_TRUE : FieldValuesPredicate.ALWAYS_FALSE;
		return this.self();
	}

	public AppDef<APP, PROPERTY, PARAMETER> setShouldAddField(
			FieldValuesPredicate<? super APP, ? super PROPERTY, ? super PARAMETER> shouldAddField) {
		this.shouldAddField = shouldAddField;
		return this.self();
	}

	public FieldValuesPredicate<? super APP, ? super PROPERTY, ? super PARAMETER> getShouldAddField() {
		return this.shouldAddField;
	}

	/**
	 * Wraps the existing field. If the existing field is not set the wrapper will
	 * not be executed.
	 * 
	 * @param wrapper the wrapper of the current field
	 * @return this
	 */
	public final AppDef<APP, PROPERTY, PARAMETER> wrapField(//
			final FieldValuesConsumer<? super APP, ? super PROPERTY, ? super PARAMETER, FormlyBuilder<?>> wrapper//
	) {
		final var oldField = this.field;
		if (oldField == null) {
			return this;
		}
		Objects.requireNonNull(wrapper);
		this.field = (app, prop, l, param) -> {
			var field = oldField.get(app, prop, l, param);
			wrapper.accept(app, prop, l, param, field);
			return field;
		};
		return this;
	}

	/**
	 * Executes the {@link Consumer} if the valueProvider can provide an instance
	 * with the given values.
	 * 
	 * @param <APP>         the type of the {@link OpenemsApp}
	 * @param <PROPERTY>    the type of the property
	 * @param <PARAMETER>   the type of the parameters
	 * @param <T>           the type of the provided instance
	 * @param app           the app
	 * @param property      the property
	 * @param language      the {@link Language}
	 * @param parameter     the parameter
	 * @param valueProvider the provider of the instance
	 * @param consumer      the consumer to consume the instance
	 */
	private static final <APP, PROPERTY, PARAMETER, T> void doIfPresent(//
			final APP app, //
			final PROPERTY property, //
			final Language language, //
			final PARAMETER parameter, //
			final FieldValuesSupplier<? super APP, ? super PROPERTY, ? super PARAMETER, T> valueProvider, //
			final Consumer<T> consumer //
	) {
		if (valueProvider == null) {
			return;
		}
		var result = valueProvider.get(app, property, language, parameter);
		consumer.accept(result);
	}

	private final Optional<ResourceBundle> usingTranslation(//
			final PARAMETER parameter //
	) {
		return Optional.ofNullable(this.translationBundleSupplier) //
				.map(t -> t.apply(parameter));
	}

	private final FieldValuesSupplier<APP, PROPERTY, PARAMETER, String> translate(//
			final String key, //
			final Object... params //
	) {
		return (app, prop, t, param) -> {
			return this.usingTranslation(param) //
					.map(b -> TranslationUtil.getTranslation(b, key, params)) //
					.orElse(null); //
		};
	}

	private final FieldValuesSupplier<APP, PROPERTY, PARAMETER, String> translateWithAppPrefix(//
			final String key, //
			final Object... params //
	) {
		return (app, prop, t, param) -> {
			return this.usingTranslation(param) //
					.map(b -> TranslationUtil.getTranslation(b, app.getAppId() + key, params)) //
					.orElseGet(() -> {
						LOG.warn("No bundle supplier for Key '" + key + "'!");
						return key;
					});
		};
	}

	/**
	 * Gets the function to get the label.
	 * 
	 * @return the function
	 */
	public FieldValuesSupplier<? super APP, ? super PROPERTY, ? super PARAMETER, String> getLabel() {
		return this.label;
	}

	/**
	 * Gets the function to get the description.
	 * 
	 * @return the function
	 */
	public FieldValuesSupplier<? super APP, ? super PROPERTY, ? super PARAMETER, String> getDescription() {
		return this.description;
	}

	/**
	 * Gets the function to get the label.
	 * 
	 * @return the function
	 */
	public final FieldValuesSupplier<? super APP, ? super PROPERTY, ? super PARAMETER, JsonElement> getDefaultValue() {
		return this.defaultValue;
	}

	/**
	 * Gets the function to get the {@link FormlyBuilder}.
	 * 
	 * @return the function
	 */
	public final FieldValuesSupplier<? super APP, ? super PROPERTY, ? super PARAMETER, FormlyBuilder<?>> getField() {
		if (this.field == null) {
			return null;
		}
		return (app, property, l, parameter) -> {
			final var field = this.field.get(app, property, l, parameter);
			doIfPresent(app, property, l, parameter, this.label, field::setLabel);
			doIfPresent(app, property, l, parameter, this.description, field::setDescription);
			doIfPresent(app, property, l, parameter, this.defaultValue, field::setDefaultValue);
			return field;
		};
	}

	/**
	 * Gets the function to get the bidirectional value.
	 * 
	 * <p>
	 * This value may be obtained from a component
	 * 
	 * @return the function to get the value
	 */
	public FieldValuesFunction<? super APP, ? super PROPERTY, ? super PARAMETER, JsonObject, JsonElement> getBidirectionalValue() {
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
	 * @param propOfComponentId        the key to get the component id from a
	 *                                 configuration
	 * @param property                 the property of the component
	 * @param componentManagerFunction the function to get the component manager
	 * @return this
	 */
	public AppDef<APP, PROPERTY, PARAMETER> bidirectional(//
			final PROPERTY propOfComponentId, //
			final String property, //
			final Function<? super APP, ComponentManager> componentManagerFunction //
	) {
		this.bidirectionalValue = (app, prop, l, param, properties) -> {
			if (properties == null) {
				return null;
			}
			final var componentId = properties.get(propOfComponentId.name());
			if (componentId == null) {
				return null;
			}
			final var componentManager = componentManagerFunction.apply(app);
			final var optionalComponent = componentManager.getEdgeConfig() //
					.getComponent(componentId.getAsString());
			return optionalComponent.map(component -> {
				return component.getProperty(property).orElse(null);
			}).orElseGet(() -> this.getDefaultValue().get(app, prop, l, param));
		};
		// set allowedToSave automatically to false
		this.isAllowedToSave = false;
		return this.self();
	}

}