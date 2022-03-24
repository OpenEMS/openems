package io.openems.edge.core.appmanager;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentContext;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.InvalidValueException;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.function.ThrowingBiFunction;
import io.openems.common.function.ThrowingFunction;
import io.openems.common.types.EdgeConfig;
import io.openems.common.types.EdgeConfig.Component;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.validator.Checkable;
import io.openems.edge.core.appmanager.validator.Validator;
import io.openems.edge.core.host.NetworkInterface.Inet4AddressWithNetmask;

public abstract class AbstractOpenemsApp<PROPERTY extends Enum<PROPERTY>> implements OpenemsApp {

	protected final ComponentManager componentManager;
	protected final ComponentContext componentContext;
	protected final ComponentUtil componentUtil;

	protected AbstractOpenemsApp(ComponentManager componentManager, ComponentContext componentContext) {
		this.componentManager = componentManager;
		this.componentContext = componentContext;
		this.componentUtil = new ComponentUtil(componentManager);
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
	 * Builds an JsonArray of all available Relay Options.
	 *
	 * @return an JsonArray
	 */
	protected JsonArray buildAvailableRelayOptions() {
		var options = JsonUtils.buildJsonArray();
		for (var io : this.componentUtil.getAvailableRelays()) {
			for (var relay : io.relays) {
				options.add(JsonUtils.buildJsonObject() //
						.addProperty("label", relay) //
						.addProperty("value", relay) //
						.build());
			}
		}

		return options.build();
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
		var c = this.configuration(errors, target,
				this.convertToEnumMap(target != ConfigurationTarget.TEST ? errors : new ArrayList<>(), config));
		if (!errors.isEmpty()) {
			throw new OpenemsException(errors.stream().collect(Collectors.joining("|")));
		}
		return c;
	}

	@Override
	public String getAppId() {
		return this.componentContext.getProperties().get(ComponentConstants.COMPONENT_NAME).toString();
	}

	protected List<Checkable> getCompatibleCheckables() {
		return null;
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

	protected List<Checkable> getInstallableCheckables() {
		return null;
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
		var validator = new Validator(this.getCompatibleCheckables(), this.getInstallableCheckables());
		if (this.installationValidation() != null) {
			validator.setConfigurationValidation((t, u) -> {
				var p = this.convertToEnumMap(new ArrayList<>(), u);
				return this.installationValidation().apply(t, p);
			});
		}
		return validator;
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

	// validation
	protected ThrowingBiFunction<//
			ConfigurationTarget, // ADD, UPDATE, VALIDATE, DELETE or TEST
			EnumMap<PROPERTY, JsonElement>, // configuration properties
			List<Checkable>, // return value of the function
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
			ComponentUtil.isSameConfigurationWithoutAlias(errors, expectedComponent, actualComponent);
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
		var schedulerComponents = actualEdgeConfig.getComponentsByFactory("Scheduler.AllAlphabetically");
		if (schedulerComponents.isEmpty()) {
			errors.add("Scheduler is missing");
			return;
		}
		if (schedulerComponents.size() > 1) {
			errors.add("More than one Scheduler configured");
			return;
		}

		var schedulerComponent = schedulerComponents.get(0);
		var controllerIdsElement = schedulerComponent.getProperty("controllers.ids").orElse(new JsonArray());
		JsonArray controllerIds;
		try {
			controllerIds = JsonUtils.getAsJsonArray(controllerIdsElement);
		} catch (OpenemsNamedException e) {
			errors.add("Undefined error in Scheduler: " + e.getMessage());
			return;
		}

		// Prepare Queue
		var controllers = new LinkedList<>(expectedAppConfiguration.schedulerExecutionOrder);
		var nextControllerId = controllers.poll();

		// Remove found Controllers from Queue in order
		for (var controllerIdElement : controllerIds) {
			String controllerId;
			try {
				controllerId = JsonUtils.getAsString(controllerIdElement);
			} catch (OpenemsNamedException e) {
				errors.add("Undefined error in Scheduler: " + e.getMessage());
				continue;
			}

			if (controllerId.equals(nextControllerId)) {
				nextControllerId = controllers.poll();
			}
		}
		if (nextControllerId != null) {
			errors.add("Controller [" + nextControllerId + "] is not/wrongly configured in Scheduler");
		}
	}

}
