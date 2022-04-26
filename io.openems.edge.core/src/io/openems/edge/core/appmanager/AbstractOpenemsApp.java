package io.openems.edge.core.appmanager;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentContext;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.InvalidValueException;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.function.ThrowingBiFunction;
import io.openems.common.function.ThrowingFunction;
import io.openems.common.types.EdgeConfig;
import io.openems.common.types.EdgeConfig.Component;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.validator.CheckCardinality;
import io.openems.edge.core.appmanager.validator.Checkable;
import io.openems.edge.core.appmanager.validator.Validator;
import io.openems.edge.core.host.NetworkInterface.Inet4AddressWithNetmask;

public abstract class AbstractOpenemsApp<PROPERTY extends Enum<PROPERTY>> implements OpenemsApp {

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
	 * Provides a factory for {@link AppConfiguration}s.
	 *
	 * @return a {@link ThrowingFunction} that creates a {@link AppConfiguration}
	 *         from a {@link EnumMap} of configuration properties for a given
	 *         {@link ConfigurationTarget}.
	 */
	protected abstract ThrowingBiFunction<//
			ConfigurationTarget, // ADD, UPDATE, VALIDATE, DELETE or TEST
			EnumMap<PROPERTY, JsonElement>, // configuration properties
			AppConfiguration, // return value of the function
			OpenemsNamedException> // Exception on error
			appConfigurationFactory();

	protected final void assertCheckables(ConfigurationTarget t, Checkable... checkables) throws OpenemsNamedException {
		if (t != ConfigurationTarget.ADD && t != ConfigurationTarget.UPDATE) {
			return;
		}
		final List<String> errors = new ArrayList<>();
		for (Checkable checkable : checkables) {
			if (!checkable.check()) {
				errors.add(checkable.getErrorMessage());
			}
		}
		if (!errors.isEmpty()) {
			throw new OpenemsException(errors.stream().collect(Collectors.joining(";")));
		}
	}

	/**
	 * Gets the {@link AppConfiguration} for the given properties.
	 *
	 * @param errors              a collection of validation errors
	 * @param configurationTarget the target of the configuration
	 * @param properties          the configured App properties
	 * @return the {@link AppConfiguration} or null
	 */
	private AppConfiguration configuration(ArrayList<String> errors, ConfigurationTarget configurationTarget,
			EnumMap<PROPERTY, JsonElement> properties) {
		try {
			return this.appConfigurationFactory().apply(configurationTarget, properties);
		} catch (OpenemsNamedException e) {
			errors.add(e.getMessage());
			return null;
		}
	}

	/**
	 * Convert JsonObject with Properties to EnumMap.
	 *
	 * @param errors     a collection of validation errors
	 * @param properties the configured App properties
	 * @return a typed {@link EnumMap} of Properties
	 */
	private EnumMap<PROPERTY, JsonElement> convertToEnumMap(ArrayList<String> errors, JsonObject properties) {
		var clazz = this.getPropertyClass();
		var result = new EnumMap<PROPERTY, JsonElement>(clazz);
		var unknownProperties = new ArrayList<String>();
		for (Entry<String, JsonElement> entry : properties.entrySet()) {
			final PROPERTY key;
			try {
				key = Enum.valueOf(clazz, entry.getKey());
			} catch (IllegalArgumentException e) {
				// ignore ALIAS if passed but not used
				if (!entry.getKey().equals("ALIAS")) {
					unknownProperties.add(entry.getKey());
				}
				continue;
			}
			result.put(key, entry.getValue());
		}
		if (!unknownProperties.isEmpty()) {
			errors.add("Unknown Configuration Propert" //
					+ (unknownProperties.size() > 1 ? "ies" : "y") + ":" //
					+ unknownProperties.stream().collect(Collectors.joining(",")));
		}
		return result;
	}

	@Override
	public AppConfiguration getAppConfiguration(ConfigurationTarget target, JsonObject config)
			throws OpenemsNamedException {
		var errors = new ArrayList<String>();
		var enumMap = this.convertToEnumMap(target != ConfigurationTarget.TEST ? errors : new ArrayList<>(), config);
		var c = this.configuration(errors, target, enumMap);

		// TODO remove and maybe add @AttributeDefinition above enums
		// this is for removing passwords so they do not get saved
		if (config.size() != enumMap.size()) {
			// remove entries that got removed
			var toRemoveKeys = new LinkedList<String>();
			for (var configEntry : config.entrySet()) {
				var key = configEntry.getKey();
				var contains = false;
				for (var entry : enumMap.entrySet()) {
					if (entry.getKey().name().equals(key)) {
						contains = true;
						break;
					}
				}
				if (!contains) {
					toRemoveKeys.add(key);
				}
			}
			for (var key : toRemoveKeys) {
				config.remove(key);
			}
		}

		if (!errors.isEmpty()) {
			throw new OpenemsException(errors.stream().collect(Collectors.joining("|")));
		}
		return c;
	}

	@Override
	public String getAppId() {
		return this.componentContext.getProperties().get(ComponentConstants.COMPONENT_NAME).toString();
	}

	/**
	 * Gets the id of the map with the given DefaultEnum
	 *
	 * <p>
	 * e. g. defaultValue: "ess0" => the next available id with the base-name "ess"
	 * and the the next available number
	 *
	 * @param t   the configuration target
	 * @param map the configuration map
	 * @param key the key to be searched for
	 * @return the found id
	 */
	protected String getId(ConfigurationTarget t, EnumMap<PROPERTY, JsonElement> map, DefaultEnum key) {
		try {
			return this.getId(t, map, Enum.valueOf(this.getPropertyClass(), key.name()), key.getDefaultValue());
		} catch (IllegalArgumentException ex) {
			// not a enum of property
		}
		return key.getDefaultValue();
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
	protected String getId(ConfigurationTarget t, EnumMap<PROPERTY, JsonElement> map, PROPERTY p, String defaultId) {
		if (t == ConfigurationTarget.TEST) {
			if (map.containsKey(p)) {
				return map.get(p).getAsString() + p.name() + ":" + defaultId;
			}
			return p.name();
		}
		return this.getValueOrDefault(map, p, defaultId);
	}

	protected abstract Class<PROPERTY> getPropertyClass();

	/**
	 * Validate the App configuration.
	 *
	 * @param jProperties a JsonObject holding the App properties
	 * @return a list of validation errors. Empty list says 'no errors'
	 */
	private List<String> getValidationErrors(JsonObject jProperties) {
		final var errors = new ArrayList<String>();

		final var properties = this.convertToEnumMap(errors, jProperties);
		final var appConfiguration = this.configuration(errors, ConfigurationTarget.VALIDATE, properties);
		if (appConfiguration == null) {
			return errors;
		}

		final var edgeConfig = this.componentManager.getEdgeConfig();

		this.validateComponentConfigurations(errors, edgeConfig, appConfiguration);
		this.validateScheduler(errors, edgeConfig, appConfiguration);

		// TODO remove 'if' if it works on windows
		// changing network settings only works on linux
		if (!System.getProperty("os.name").startsWith("Windows")) {
			this.validateIps(errors, edgeConfig, appConfiguration);
		}

		return errors;
	}

	@Override
	public final Validator getValidator() {
		Map<String, Object> properties = new TreeMap<>();
		properties.put("openemsApp", this);

		// add check for cardinality for every app
		var validator = this.getValidateBuilder().build();
		validator.getInstallableCheckableNames().put(CheckCardinality.COMPONENT_NAME, properties);

		if (this.installationValidation() != null) {
			validator.setConfigurationValidation((t, u) -> {
				var p = this.convertToEnumMap(new ArrayList<>(), u);
				return this.installationValidation().apply(t, p);
			});
		}
		return validator;
	}

	protected Validator.Builder getValidateBuilder() {
		return Validator.create();
	}

	/**
	 * Gets the value of the property name in the map or the defaulValue if the
	 * property was not found.
	 *
	 * @param map      the configuration map
	 * @param property the property to be searched for
	 * @return the String value
	 */
	protected String getValueOrDefault(EnumMap<PROPERTY, JsonElement> map, DefaultEnum property) {
		var key = Enum.valueOf(this.getPropertyClass(), property.name());
		return this.getValueOrDefault(map, key, property.getDefaultValue());
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
	protected String getValueOrDefault(EnumMap<PROPERTY, JsonElement> map, PROPERTY property, String defaultValue) {
		var element = map.get(property);
		if (element != null) {
			return element.getAsString();
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
		try {
			Enum.valueOf(this.getPropertyClass(), property);
			return true;
		} catch (IllegalArgumentException ex) {
			// property not an enum property
		}
		return false;
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
			EnumMap<PROPERTY, JsonElement>, // configuration properties
			Map<String, Map<String, ?>>, // return value of the function
			OpenemsNamedException> // Exception on error
			installationValidation() {
		return null;
	}

	@Override
	public void validate(OpenemsAppInstance instance) throws OpenemsNamedException {
		var errors = this.getValidationErrors(instance.properties);
		if (!errors.isEmpty()) {
			var error = errors.stream().collect(Collectors.joining("|"));
			throw new OpenemsException(error);
		}
	}

	/**
	 * Compare actual and expected Components.
	 *
	 * @param errors                   a collection of validation errors
	 * @param actualEdgeConfig         the currently active {@link EdgeConfig}
	 * @param expectedAppConfiguration the expected {@link AppConfiguration}
	 */
	private void validateComponentConfigurations(ArrayList<String> errors, EdgeConfig actualEdgeConfig,
			AppConfiguration expectedAppConfiguration) {
		var missingComponents = new ArrayList<String>();
		for (Component expectedComponent : expectedAppConfiguration.components) {
			var componentId = expectedComponent.getId();

			// Get Actual Component Configuration
			Component actualComponent;
			try {

				actualComponent = actualEdgeConfig.getComponentOrError(componentId);
			} catch (InvalidValueException e) {
				missingComponents.add(componentId);
				continue;
			}
			// ALIAS is not really necessary to validate
			ComponentUtilImpl.isSameConfigurationWithoutAlias(errors, expectedComponent, actualComponent);
		}

		if (!missingComponents.isEmpty()) {
			errors.add("Missing Component" //
					+ (missingComponents.size() > 1 ? "s" : "") + ":" //
					+ missingComponents.stream().collect(Collectors.joining(",")));
		}
	}

	private void validateIps(ArrayList<String> errors, EdgeConfig actualEdgeConfig,
			AppConfiguration expectedAppConfiguration) {

		if (expectedAppConfiguration.ips.isEmpty()) {
			return;
		}
		List<Inet4AddressWithNetmask> addresses = new ArrayList<>(expectedAppConfiguration.ips.size());
		for (var address : expectedAppConfiguration.ips) {
			try {
				addresses.add(Inet4AddressWithNetmask.fromString(address));
			} catch (OpenemsException e) {
				errors.add("Could not parse ip '" + address + "'.");
			}
		}

		try {
			var interfaces = this.componentUtil.getInterfaces();
			var eth0 = interfaces.stream().filter(t -> t.getName().equals("eth0")).findFirst().get();
			var eth0Adresses = eth0.getAddresses();
			addresses.removeAll(eth0Adresses.getValue());
			for (var address : addresses) {
				errors.add("Address '" + address + "' is not added.");
			}
		} catch (NullPointerException | IllegalStateException | OpenemsNamedException e) {
			errors.add("Can not validate host config!");
			errors.add(e.getMessage());
		}
	}

	/**
	 * Validates the execution order in the Scheduler.
	 *
	 * @param errors                   a collection of validation errors
	 * @param actualEdgeConfig         the currently active {@link EdgeConfig}
	 * @param expectedAppConfiguration the expected {@link AppConfiguration}
	 */
	private void validateScheduler(ArrayList<String> errors, EdgeConfig actualEdgeConfig,
			AppConfiguration expectedAppConfiguration) {

		List<String> schedulerIds;
		try {
			schedulerIds = this.componentUtil.getSchedulerIds();
		} catch (OpenemsNamedException e) {
			errors.add(e.getMessage());
			return;
		}

		// Prepare Queue
		var controllers = new LinkedList<>(this.componentUtil.removeIdsWhichNotExist(
				expectedAppConfiguration.schedulerExecutionOrder, expectedAppConfiguration.components));

		var nextControllerId = controllers.poll();

		// Remove found Controllers from Queue in order
		for (var controllerId : schedulerIds) {
			if (controllerId.equals(nextControllerId)) {
				nextControllerId = controllers.poll();
			}
		}
		if (nextControllerId != null) {
			errors.add("Controller [" + nextControllerId + "] is not/wrongly configured in Scheduler");
		}
	}

	protected final String getTestImage() {
		return "data:image/png;base64,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";
	}

}
