package io.openems.edge.core.appmanager.validator;

import java.util.List;
import java.util.stream.Collectors;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.validator.ValidatorConfig.CheckableConfig;

public interface Validator {

	/**
	 * Gets the error messages for compatibility.
	 *
	 * @return the error messages
	 */
	public default List<String> getErrorCompatibleMessages(ValidatorConfig config) {
		return getErrorMessages(config.getCompatibleCheckableConfigs(), false);
	}

	/**
	 * Gets the error messages for installation.
	 *
	 * @return the error messages
	 */
	public default List<String> getErrorInstallableMessages(ValidatorConfig config) {
		return getErrorMessages(config.getInstallableCheckableConfigs(), false);
	}

	/**
	 * Validates the Configuration {@link Checkable}s.
	 *
	 * @param target     the target of the configuration
	 * @param properties the configuration properties
	 * @throws OpenemsNamedException on validation error
	 */
	public default void validateConfiguration(ValidatorConfig config, ConfigurationTarget target, JsonObject properties)
			throws OpenemsNamedException {
		if (config.getConfigurationValidation() == null) {
			return;
		}
		var checkables = config.getConfigurationValidation().apply(target, properties);
		if (checkables == null) {
			return;
		}
		var errors = getErrorMessages(config.getCompatibleCheckableConfigs(), false);
		if (!errors.isEmpty()) {
			throw new OpenemsException(errors.stream().collect(Collectors.joining(";")));
		}
	}

	/**
	 * Validates the {@link Checkable}s and gets the Status.
	 *
	 * @return the Status
	 */
	public default OpenemsAppStatus getStatus(ValidatorConfig config) {
		if (!this.getErrorMessages(config.getCompatibleCheckableConfigs(), true).isEmpty()) {
			return OpenemsAppStatus.INCOMPATIBLE;
		}
		if (!this.getErrorMessages(config.getInstallableCheckableConfigs(), true).isEmpty()) {
			return OpenemsAppStatus.COMPATIBLE;
		}
		return OpenemsAppStatus.INSTALLABLE;
	}

	/**
	 * Builds a {@link JsonObject} out of this {@link Validator}.
	 *
	 * @return the {@link JsonObject}
	 */
	public default JsonObject toJsonObject(ValidatorConfig config) {
		return JsonUtils.buildJsonObject() //
				.addProperty("name", this.getStatus(config).name()) //
				.add("errorCompatibleMessages",
						this.getErrorCompatibleMessages(config).stream().map(s -> new JsonPrimitive(s))
								.collect(JsonUtils.toJsonArray())) //
				.add("errorInstallableMessages",
						this.getErrorInstallableMessages(config).stream().map(s -> new JsonPrimitive(s))
								.collect(JsonUtils.toJsonArray())) //
				.build();
	}

	/**
	 * Gets the error messages for the given {@link Checkable}.
	 *
	 * @param checkableConfigs the {@link Checkable}s to be checked.
	 * @param returnImmediate  after the first checkable who returns false
	 * @return a list of errors
	 */
	public abstract List<String> getErrorMessages(List<CheckableConfig> checkableConfigs, boolean returnImmediate);

}
