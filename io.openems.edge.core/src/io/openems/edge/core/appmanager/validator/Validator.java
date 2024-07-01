package io.openems.edge.core.appmanager.validator;

import static java.util.stream.Collectors.joining;

import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.session.Language;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.validator.ValidatorConfig.CheckableConfig;

public interface Validator {

	/**
	 * Gets the error messages for compatibility.
	 *
	 * @param config   the config that gets validated
	 * @param language the language of the errors
	 * @return the error messages
	 */
	public default List<String> getErrorCompatibleMessages(ValidatorConfig config, Language language) {
		return this.getErrorMessages(config.getCompatibleCheckableConfigs(), language, false);
	}

	/**
	 * Gets the error messages for installation.
	 *
	 * @param config   the config that gets validated
	 * @param language the language of the errors
	 * @return the error messages
	 */
	public default List<String> getErrorInstallableMessages(ValidatorConfig config, Language language) {
		return this.getErrorMessages(config.getInstallableCheckableConfigs(), language, false);
	}

	/**
	 * Validates the {@link Checkable}s and gets the Status.
	 *
	 * @param config the config that gets validated
	 * @return the Status
	 */
	public default OpenemsAppStatus getStatus(ValidatorConfig config) {
		// language not need for status
		if (!this.getErrorMessages(config.getCompatibleCheckableConfigs(), Language.DEFAULT, true).isEmpty()) {
			return OpenemsAppStatus.INCOMPATIBLE;
		}
		if (!this.getErrorMessages(config.getInstallableCheckableConfigs(), Language.DEFAULT, true).isEmpty()) {
			return OpenemsAppStatus.COMPATIBLE;
		}
		return OpenemsAppStatus.INSTALLABLE;
	}

	/**
	 * Builds a {@link JsonObject} out of the given {@link ValidatorConfig}.
	 *
	 * @param config   the config that gets validated
	 * @param language the language of the errors
	 * @return the {@link JsonObject}
	 */
	public default JsonObject toJsonObject(ValidatorConfig config, Language language) {
		return JsonUtils.buildJsonObject() //
				.addProperty("name", this.getStatus(config).name()) //
				.add("errorCompatibleMessages",
						this.getErrorCompatibleMessages(config, language).stream().map(JsonPrimitive::new)
								.collect(JsonUtils.toJsonArray())) //
				.add("errorInstallableMessages",
						this.getErrorInstallableMessages(config, language).stream().map(JsonPrimitive::new)
								.collect(JsonUtils.toJsonArray())) //
				.build();
	}

	/**
	 * Gets the error messages for the given {@link Checkable}.
	 *
	 * @param checkableConfigs the {@link Checkable}s to be checked.
	 * @param language         the language of the errors
	 * @param returnImmediate  after the first checkable who returns false
	 * @return a list of errors
	 */
	public List<String> getErrorMessages(List<CheckableConfig> checkableConfigs, Language language,
			boolean returnImmediate);

	/**
	 * Checks the current status of an {@link OpenemsApp} and throws an exception if
	 * the app can not be installed.
	 * 
	 * @param openemsApp the {@link OpenemsApp} to check
	 * @param language   the current {@link Language}
	 * @throws OpenemsNamedException on status error
	 */
	public default void checkStatus(OpenemsApp openemsApp, Language language) throws OpenemsNamedException {
		var validatorConfig = openemsApp.getValidatorConfig();
		var status = this.getStatus(validatorConfig);
		switch (status) {
		case INCOMPATIBLE:
			throw new OpenemsException(
					"App is not compatible! " + this.getErrorCompatibleMessages(validatorConfig, language).stream() //
							.collect(joining(";")));
		case COMPATIBLE:
			throw new OpenemsException(
					"App can not be installed! " + this.getErrorInstallableMessages(validatorConfig, language).stream() //
							.collect(joining(";")));
		case INSTALLABLE:
			// app can be installed
			return;
		}
		throw new OpenemsException("Status '" + status.name() + "' is not implemented.");
	}

}
