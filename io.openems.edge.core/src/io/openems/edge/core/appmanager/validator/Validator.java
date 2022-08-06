package io.openems.edge.core.appmanager.validator;

import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.openems.common.session.Language;
import io.openems.common.utils.JsonUtils;
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

}
