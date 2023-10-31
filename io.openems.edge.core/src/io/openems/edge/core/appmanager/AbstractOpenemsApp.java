package io.openems.edge.core.appmanager;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentContext;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.function.ThrowingBiFunction;
import io.openems.common.function.ThrowingFunction;
import io.openems.common.function.ThrowingTriFunction;
import io.openems.common.session.Language;
import io.openems.common.types.EdgeConfig.Component;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.user.User;
import io.openems.edge.core.appmanager.validator.CheckCardinality;
import io.openems.edge.core.appmanager.validator.Checkable;
import io.openems.edge.core.appmanager.validator.ValidatorConfig;

public abstract class AbstractOpenemsApp<PROPERTY extends Nameable> //
		implements OpenemsApp, ComponentUtilSupplier, ComponentManagerSupplier {

	protected final ComponentManager componentManager;
	protected final ConfigurationAdmin cm;
	protected final ComponentContext componentContext;
	protected final ComponentUtil componentUtil;

	protected AbstractOpenemsApp(ComponentManager componentManager, ComponentContext componentContext,
			ConfigurationAdmin cm, ComponentUtil componentUtil) {
		this.componentManager = componentManager;
		this.componentContext = componentContext;
		this.cm = cm;
		this.componentUtil = componentUtil;
	}

	/**
	 * Provides a factory for {@link AppConfiguration AppConfigurations}.
	 *
	 * @return a {@link ThrowingFunction} that creates a {@link AppConfiguration}
	 *         from a {@link Map} of configuration properties for a given
	 *         {@link ConfigurationTarget} in the specified language.
	 */
	protected abstract ThrowingTriFunction<//
			ConfigurationTarget, //
			Map<PROPERTY, JsonElement>, //
			Language, //
			AppConfiguration, //
			OpenemsNamedException> appPropertyConfigurationFactory();

	/**
	 * Gets the {@link AppConfiguration} for the given properties.
	 *
	 * @param errors              a collection of validation errors
	 * @param configurationTarget the target of the configuration
	 * @param language            the language of the configuration
	 * @param properties          the configured App properties
	 * @return the {@link AppConfiguration} or null
	 */
	private AppConfiguration configuration(ArrayList<String> errors, ConfigurationTarget configurationTarget,
			Language language, Map<PROPERTY, JsonElement> properties) {
		try {
			return this.appPropertyConfigurationFactory().apply(configurationTarget, properties, language);
		} catch (OpenemsNamedException e) {
			errors.add(e.getMessage());
			return null;
		}
	}

	/**
	 * Convert JsonObject with Properties to Map.
	 *
	 * @param errors     a collection of validation errors
	 * @param properties the configured App properties
	 * @return a typed {@link Map} of Properties
	 */
	private Map<PROPERTY, JsonElement> convertToMap(List<String> errors, JsonObject properties) {
		final var nameableByName = Arrays.stream(this.propertyValues()) //
				.collect(Collectors.toMap(t -> t.name(), Function.identity()));
		final var resultMap = ImmutableMap.<PROPERTY, JsonElement>builder();
		final var unknownProperties = new ArrayList<String>();
		for (var entry : properties.entrySet()) {
			final var name = entry.getKey();
			if (!nameableByName.containsKey(name)) {
				if ("ALIAS".equals(name)) {
					// ignore alias if passed but not used
					continue;
				}
				unknownProperties.add(entry.getKey());
				continue;
			}
			// TODO maybe type validation of value
			resultMap.put(nameableByName.get(entry.getKey()), entry.getValue());
		}
		if (!unknownProperties.isEmpty()) {
			errors.add("Unknown Configuration Propert" //
					+ (unknownProperties.size() > 1 ? "ies" : "y") + ":" //
					+ unknownProperties.stream().collect(Collectors.joining(",")));
		}
		return resultMap.build();
	}

	@Override
	public AppConfiguration getAppConfiguration(ConfigurationTarget target, JsonObject config, Language language)
			throws OpenemsNamedException {
		var errors = new ArrayList<String>();
		var enumMap = this.convertToMap(target != ConfigurationTarget.TEST ? errors : new ArrayList<>(), config);
		var configuration = this.configuration(errors, target, language, enumMap);

		if (!errors.isEmpty()) {
			throw new OpenemsException(this.getAppId() + ": " + errors.stream().collect(Collectors.joining("|")));
		}
		return configuration;
	}

	@Override
	public String getAppId() {
		return this.componentContext.getProperties().get(ComponentConstants.COMPONENT_NAME).toString();
	}

	/**
	 * Gets the id of the map with the given default id
	 *
	 * <p>
	 * e. g. defaultId: "ess0" => the next available id with the base-name "ess" and
	 * the the next available number
	 *
	 * @param t         the configuration target
	 * @param map       the configuration map
	 * @param p         the Property which stores the id
	 * @param defaultId the defaultId to be used
	 * @return the found id
	 */
	protected String getId(ConfigurationTarget t, Map<PROPERTY, JsonElement> map, PROPERTY p, String defaultId) {
		if (t == ConfigurationTarget.TEST) {
			return JsonUtils.getAsOptionalString(map.get(p)) //
					.map(id -> id + p.name() + ":" + defaultId) //
					.orElse(p.name());
		}
		return this.getValueOrDefault(map, p, defaultId);
	}

	@Override
	public final ValidatorConfig getValidatorConfig() {
		Map<String, Object> properties = new TreeMap<>();
		properties.put("openemsApp", this);

		// add check for cardinality for every app
		var validator = this.getValidateBuilder().build();

		validator.getInstallableCheckableConfigs()
				.add(new ValidatorConfig.CheckableConfig(CheckCardinality.COMPONENT_NAME, properties));

		return validator;
	}

	protected ValidatorConfig.Builder getValidateBuilder() {
		return ValidatorConfig.create();
	}

	/**
	 * Gets the value of the property in the map or the defaulValue if the property
	 * was not found.
	 *
	 * @param map          the configuration map
	 * @param property     the property to be searched for
	 * @param defaultValue the default value
	 * @return the String value
	 */
	protected String getValueOrDefault(Map<PROPERTY, JsonElement> map, PROPERTY property, String defaultValue) {
		var element = map.get(property);
		if (element != null) {
			return JsonUtils.getAsOptionalString(element).orElse(defaultValue);
		}
		return defaultValue;
	}

	/**
	 * Checks if the given property is in the Property class included.
	 *
	 * @param property the enum property
	 * @return true if it is included else false
	 */
	public boolean hasProperty(String property) {
		return Arrays.stream(this.propertyValues()) //
				.anyMatch(t -> t.name().equals(property));
	}

	/**
	 * The returning function gets called during app add or update. The returned
	 * {@link Checkable}s are executed after setting the network configuration.
	 *
	 * <p>
	 * e. g. the function can return a {@link Checkable} for checking if a device is
	 * reachable via network.
	 * </p>
	 *
	 * @return a factory function which returns {@link Checkable}s
	 */
	protected ThrowingBiFunction<//
			ConfigurationTarget, // ADD, UPDATE, VALIDATE, DELETE or TEST
			Map<PROPERTY, JsonElement>, // configuration properties
			Map<String, Map<String, ?>>, // return value of the function
			OpenemsNamedException> // Exception on error
			installationValidation() {
		return null;
	}

	/**
	 * Creates a copy of the original configuration and fills up properties which
	 * are binded bidirectional.
	 * 
	 * <p>
	 * e. g. a property in a component is the same as one configured in the app so
	 * it directly gets stored in the component configuration and not twice to avoid
	 * miss matching errors.
	 * 
	 * @param original the original configuration
	 * @param app      the app to get the properties from
	 * @return a copy of the original one with the filled up properties
	 */
	public static JsonObject fillUpProperties(//
			final OpenemsApp app, //
			final JsonObject original //
	) {
		final var copy = original.deepCopy();
		for (var prop : app.getProperties()) {
			if (copy.has(prop.name)) {
				continue;
			}
			if (prop.bidirectionalValue == null) {
				continue;
			}
			var value = prop.bidirectionalValue.apply(copy);
			if (value == null) {
				continue;
			}
			// add value to configuration
			copy.add(prop.name, value);
		}
		return copy;
	}

	@Override
	public OpenemsAppPermissions getAppPermissions() {
		return OpenemsAppPermissions.create().build();
	}

	@Override
	public String getName(Language language) {
		return AbstractOpenemsApp.getTranslation(language, this.getAppId() + ".Name");
	}

	@Override
	public String getShortName(Language language) {
		return AbstractOpenemsApp.getNullableTranslation(language, this.getAppId() + ".Name.short");
	}

	@Override
	public String getImage() {
		var imageName = this.getClass().getSimpleName() + ".png";
		var image = base64OfImage(this.getClass().getResource(imageName));
		if (image != null) {
			return image;
		}
		return OpenemsApp.FALLBACK_IMAGE;
	}

	@Override
	public OpenemsAppPropertyDefinition[] getProperties() {
		throw new UnsupportedOperationException();
	}

	@Override
	public AppAssistant getAppAssistant(User user) {
		return this.getAppAssistant(user.getLanguage());
	}

	protected AppAssistant getAppAssistant(Language l) {
		return AppAssistant.create(this.getName(l)) //
				.build();
	}

	protected abstract PROPERTY[] propertyValues();

	protected final PROPERTY getPropertyByName(String name) {
		return Arrays.stream(this.propertyValues()) //
				.filter(t -> t.name().equals(name)) //
				.findFirst().orElse(null); //
	}

	protected static String getTranslation(Language language, String key) {
		return TranslationUtil.getTranslation(getTranslationBundle(language), key);
	}

	protected static String getNullableTranslation(Language language, String key) {
		return TranslationUtil.getNullableTranslation(getTranslationBundle(language), key);
	}

	/**
	 * Gets the {@link ResourceBundle} based on the given {@link Language}.
	 * 
	 * @param language the {@link Language} of the translations
	 * @return the {@link ResourceBundle}
	 */
	public static ResourceBundle getTranslationBundle(Language language) {
		if (language == null) {
			language = Language.DEFAULT;
		}
		// TODO add language support
		switch (language) {
		case CZ:
		case ES:
		case FR:
		case NL:
			language = Language.EN;
			break;
		case DE:
		case EN:
			break;
		}
		return ResourceBundle.getBundle("io.openems.edge.core.appmanager.translation", language.getLocal());
	}

	/**
	 * Gets the {@link ResourceBundle} based on the given {@link Language}.
	 * 
	 * <p>
	 * Used in {@link OpenemsApps} to create their {@link Type#getParamter()}.
	 * 
	 * @param l the {@link Language} of the translations
	 * @return the {@link ResourceBundle}
	 * @implNote just a name alias to
	 *           {@link AbstractOpenemsApp#getTranslationBundle(Language)}
	 */
	public static ResourceBundle createResourceBundle(Language l) {
		return getTranslationBundle(l);
	}

	protected static final String base64OfImage(URL url) {
		if (url == null) {
			return null;
		}
		final var prefix = "data:image/png;base64,";
		try (var is = url.openStream()) {
			return prefix + Base64.getEncoder().encodeToString(is.readAllBytes());
		} catch (IOException e) {
			// image not found
			e.printStackTrace();
			return null;
		}
	}

	protected static final Component getComponentWithFactoryId(List<Component> components, String factoryId) {
		return components.stream().filter(t -> t.getFactoryId().equals(factoryId)).findFirst().orElse(null);
	}

	@Override
	public ComponentManager getComponentManager() {
		return this.componentManager;
	}

	@Override
	public ComponentUtil getComponentUtil() {
		return this.componentUtil;
	}

}
