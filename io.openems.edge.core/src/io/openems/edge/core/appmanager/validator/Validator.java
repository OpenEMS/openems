package io.openems.edge.core.appmanager.validator;

import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.session.Language;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.validator.ValidatorConfig.CheckableConfig;

public interface Validator {

	/**
	 * Gets the {@link OpenemsNamedException}s for compatibility.
	 *
	 * @param config   the config that gets validated
	 * @param language the language of the errors
	 * @return the error
	 */
	public default List<OpenemsNamedException> getCompatibleOpenemsExceptions(ValidatorConfig config,
			Language language) {
		return this.getNamedExceptions(config.getCompatibleCheckableConfigs(), language, false);
	}

	/**
	 * Gets the {@link OpenemsNamedException}s for installation.
	 *
	 * @param config   the config that gets validated
	 * @param language the language of the errors
	 * @return the error
	 */
	public default List<OpenemsNamedException> getInstallableOpenemsExceptions(ValidatorConfig config,
			Language language) {
		return this.getNamedExceptions(config.getInstallableCheckableConfigs(), language, false);
	}

	/**
	 * Gets the {@link DetailedAppStatus} for a validated config.
	 *
	 * @param config   the config that gets validated
	 * @param language the language
	 * @return the {@link DetailedAppStatus}
	 */
	public default DetailedAppStatus getDetailedAppStatus(ValidatorConfig config, Language language) {
		final var compatibleExceptions = this.getNamedExceptions(config.getCompatibleCheckableConfigs(), language,
				true);
		final var installableExceptions = this.getNamedExceptions(config.getInstallableCheckableConfigs(), language,
				true);

		OpenemsAppStatus status;

		if (!compatibleExceptions.isEmpty()) {
			status = OpenemsAppStatus.INCOMPATIBLE;
		} else if (!installableExceptions.isEmpty()) {
			status = OpenemsAppStatus.COMPATIBLE;
		} else {
			status = OpenemsAppStatus.INSTALLABLE;
		}

		return new DetailedAppStatus(status, compatibleExceptions, installableExceptions);
	}

	/**
	 * Builds a {@link JsonObject} out of the given {@link ValidatorConfig}.
	 *
	 * @param config   the config that gets validated
	 * @param language the language of the errors
	 * @return the {@link JsonObject}
	 */
	public default JsonObject toJsonObject(ValidatorConfig config, Language language) {
		final var detailedStatus = this.getDetailedAppStatus(config, language);
		return JsonUtils.buildJsonObject() //
				.addProperty("name", detailedStatus.status.name()) //
				.add("errorCompatibleMessages", detailedStatus.compatibleExceptions.stream() //
						.map(OpenemsNamedException::getMessage) //
						.map(JsonPrimitive::new) //
						.collect(JsonUtils.toJsonArray())) //
				.add("errorInstallableMessages", detailedStatus.installableExceptions.stream() //
						.map(OpenemsNamedException::getMessage) //
						.map(JsonPrimitive::new) //
						.collect(JsonUtils.toJsonArray())) //
				.build();
	}

	/**
	 * Gets the {@link OpenemsNamedException}s for the given {@link Checkable}.
	 *
	 * @param checkableConfigs the {@link Checkable}s to be checked.
	 * @param language         the language of the errors
	 * @param returnImmediate  after the first checkable who returns false
	 * @return a list of errors
	 */
	public List<OpenemsNamedException> getNamedExceptions(List<CheckableConfig> checkableConfigs, Language language,
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

		final var compatibleExceptions = this.getCompatibleOpenemsExceptions(validatorConfig, language);
		if (!compatibleExceptions.isEmpty()) {
			throw new OpenemsNamedException(compatibleExceptions.getFirst().getError(),
					compatibleExceptions.getFirst().getParams());
		}

		final var installableExceptions = this.getInstallableOpenemsExceptions(validatorConfig, language);
		if (!installableExceptions.isEmpty()) {
			throw new OpenemsNamedException(installableExceptions.getFirst().getError(),
					installableExceptions.getFirst().getParams());
		}
	}

	public record DetailedAppStatus(OpenemsAppStatus status, List<OpenemsNamedException> compatibleExceptions,
			List<OpenemsNamedException> installableExceptions) {
	}

}
