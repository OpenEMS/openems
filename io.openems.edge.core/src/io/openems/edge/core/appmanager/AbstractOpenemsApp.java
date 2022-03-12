package io.openems.edge.core.appmanager;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentContext;

import com.google.common.base.Objects;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.InvalidValueException;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.function.ThrowingFunction;
import io.openems.common.types.EdgeConfig;
import io.openems.common.types.EdgeConfig.Component;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.component.ComponentManager;

public abstract class AbstractOpenemsApp<PROPERTY extends Enum<PROPERTY>> implements OpenemsApp {

	private final ComponentManager componentManager;
	private final ComponentContext componentContext;

	protected AbstractOpenemsApp(ComponentManager componentManager, ComponentContext componentContext) {
		this.componentManager = componentManager;
		this.componentContext = componentContext;
	}

	@Override
	public String getAppId() {
		return this.componentContext.getProperties().get(ComponentConstants.COMPONENT_NAME).toString();
	}

	protected static class AppConfiguration {
		public final List<Component> components;
		public final List<String> schedulerExecutionOrder;

		public AppConfiguration(List<Component> components, List<String> schedulerExecutionOrder) {
			this.components = components;
			this.schedulerExecutionOrder = schedulerExecutionOrder;
		}
	}

	protected abstract Class<PROPERTY> getPropertyClass();

	/**
	 * Provides a factory for {@link AppConfiguration}s.
	 *
	 * @return a {@link Function} that creates a {@link AppConfiguration} from a
	 *         JsonObject config.
	 */
	protected abstract ThrowingFunction<EnumMap<PROPERTY, JsonElement>, AppConfiguration, OpenemsNamedException> appConfigurationFactory();

	@Override
	public void validate(JsonObject properties) throws OpenemsException {
		var errors = this.getValidationErrors(properties);
		if (!errors.isEmpty()) {
			throw new OpenemsException(errors.stream().collect(Collectors.joining("|")));
		}
	}

	/**
	 * Validate the App configuration.
	 *
	 * @param jProperties a JsonObject holding the App properties
	 * @return a list of validation errors. Empty list says 'no errors'
	 */
	private List<String> getValidationErrors(JsonObject jProperties) {
		final var errors = new ArrayList<String>();

		final var properties = this.convertToEnumMap(errors, jProperties);
		final var appConfiguration = this.getAppConfiguration(errors, properties);
		if (appConfiguration == null) {
			return errors;
		}

		final var edgeConfig = this.componentManager.getEdgeConfig();

		this.validateComponentConfigurations(errors, edgeConfig, appConfiguration);
		this.validateScheduler(errors, edgeConfig, appConfiguration);

		return errors;
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
		var controllers = new LinkedList<String>(expectedAppConfiguration.schedulerExecutionOrder);
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

	/**
	 * Gets the {@link AppConfiguration} for the given properties.
	 *
	 * @param errors     a collection of validation errors
	 * @param properties the configured App properties
	 * @return the {@link AppConfiguration} or null
	 */
	private AppConfiguration getAppConfiguration(ArrayList<String> errors, EnumMap<PROPERTY, JsonElement> properties) {
		try {
			return this.appConfigurationFactory().apply(properties);
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
				unknownProperties.add(entry.getKey());
				continue;
			}
			result.put(key, entry.getValue());
		}
		if (!unknownProperties.isEmpty()) {
			errors.add("Unknown Configuration Property" //
					+ (unknownProperties.size() > 1 ? "ies" : "y") + ":" //
					+ unknownProperties.stream().collect(Collectors.joining(",")));
		}
		return result;
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

			var componentErrors = new ArrayList<String>();

			// Validate the Component Factory (i.e. is the Component of the correct type)
			if (!Objects.equal(expectedComponent.getFactoryId(), actualComponent.getFactoryId())) {
				componentErrors.add("Factory-ID: " //
						+ "expected '" + expectedComponent.getFactoryId() + "', " //
						+ "got '" + actualComponent.getFactoryId() + "'");
			}

			for (Entry<String, JsonElement> entry : expectedComponent.getProperties().entrySet()) {
				var key = entry.getKey();
				var expectedProperty = entry.getValue();
				JsonElement actualProperty;
				try {
					actualProperty = actualComponent.getPropertyOrError(key);
				} catch (InvalidValueException e) {
					componentErrors.add("Property '" + key + "': " //
							+ "expected '" + expectedProperty.toString() + "', " //
							+ "but property does not exist");
					continue;
				}

				if (!equals(expectedProperty, actualProperty)) {
					componentErrors.add("Property '" + key + "': " //
							+ "expected '" + expectedProperty.toString() + "', " //
							+ "got '" + actualProperty.toString() + "'");
				}
			}

			if (!componentErrors.isEmpty()) {
				errors.add(componentId + ": " //
						+ componentErrors.stream().collect(Collectors.joining("; ")));
			}
		}

		if (!missingComponents.isEmpty()) {
			errors.add("Missing Component" //
					+ (missingComponents.size() > 1 ? "s" : "") + ":" //
					+ missingComponents.stream().collect(Collectors.joining(",")));
		}
	}

	/**
	 * Validates if the 'actual' matches the 'expected' value.
	 *
	 * @param expected the expected value
	 * @param actual   the actual value
	 * @return true if they match
	 */
	protected static boolean equals(JsonElement expected, JsonElement actual) {
		if (Objects.equal(expected, actual)) {
			return true;
		}
		if ((expected == null && actual != null) || (expected != null && actual == null)) {
			return false;
		}
		// both are not null

		if (!expected.isJsonPrimitive() || !actual.isJsonPrimitive()) {
			return false;
		}
		// both are JsonPrimitives
		var e = expected.getAsJsonPrimitive();
		var a = actual.getAsJsonPrimitive();

		if (e.getAsString().equals(a.getAsString())) {
			// compare 'toString'
			return true;
		}
		return false;
	}

}
