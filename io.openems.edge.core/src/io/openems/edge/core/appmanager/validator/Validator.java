package io.openems.edge.core.appmanager.validator;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.function.ThrowingBiFunction;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.core.appmanager.ConfigurationTarget;

public class Validator {

	private final List<Checkable> compatible;
	private final List<Checkable> installable;

	private ThrowingBiFunction<ConfigurationTarget, //
			JsonObject, //
			List<Checkable>, //
			OpenemsNamedException> //
	configurationValidation;

	public Validator(List<Checkable> compatible, List<Checkable> installable) {
		this.compatible = compatible != null ? compatible : new ArrayList<>();
		this.installable = installable != null ? installable : new ArrayList<>();
	}

	/**
	 * Gets the error messages for compatibility.
	 *
	 * @return the error messages
	 */
	public List<String> getErrorCompatibleMessages() {
		return this.compatible.stream().filter(t -> !t.check()).map(Checkable::getErrorMessage)
				.collect(Collectors.toList());
	}

	/**
	 * Gets the error messages for installation.
	 *
	 * @return the error messages
	 */
	public List<String> getErrorInstallableMessages() {
		return this.installable.stream().filter(t -> !t.check()).map(Checkable::getErrorMessage)
				.collect(Collectors.toList());
	}

	/**
	 * Validates the {@link Checkable}s and gets the Status.
	 *
	 * @return the Status
	 */
	public OpenemsAppStatus getStatus() {
		for (Checkable checkable : this.compatible) {
			if (!checkable.check()) {
				return OpenemsAppStatus.INCOMPATIBLE;
			}
		}
		for (Checkable checkable : this.installable) {
			if (!checkable.check()) {
				return OpenemsAppStatus.COMPATIBLE;
			}
		}
		return OpenemsAppStatus.INSTALLABLE;
	}

	public void setConfigurationValidation(ThrowingBiFunction<ConfigurationTarget, //
			JsonObject, //
			List<Checkable>, OpenemsNamedException> configurationValidation) {
		this.configurationValidation = configurationValidation;
	}

	/**
	 * Builds a {@link JsonObject} out of this {@link Validator}.
	 *
	 * @return the {@link JsonObject}
	 */
	public JsonObject toJsonObject() {
		var compatibleMessages = JsonUtils.buildJsonArray().build();
		for (var message : this.getErrorCompatibleMessages()) {
			compatibleMessages.add(message);
		}
		var installableMessages = JsonUtils.buildJsonArray().build();
		for (var message : this.getErrorInstallableMessages()) {
			installableMessages.add(message);
		}
		return JsonUtils.buildJsonObject() //
				.addProperty("name", this.getStatus().name()) //
				.add("errorCompatibleMessages", compatibleMessages) //
				.add("errorInstallableMessages", installableMessages) //
				.build();
	}

	/**
	 * Validates the Configuration {@link Checkable}s.
	 *
	 * @param target     the target of the configuration
	 * @param properties the configuration properties
	 * @throws OpenemsNamedException on validation error
	 */
	public void validateConfiguration(ConfigurationTarget target, JsonObject properties) throws OpenemsNamedException {
		if (this.configurationValidation == null) {
			return;
		}
		var checkables = this.configurationValidation.apply(target, properties);
		if (checkables == null) {
			return;
		}
		var errors = new ArrayList<String>();
		for (Checkable checkable : checkables) {
			if (!checkable.check()) {
				errors.add(checkable.getErrorMessage());
			}
		}
		if (!errors.isEmpty()) {
			throw new OpenemsException(errors.stream().collect(Collectors.joining(";")));
		}
	}

}
