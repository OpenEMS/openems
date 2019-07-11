package io.openems.common.types;

import java.util.Optional;

import com.google.gson.JsonElement;

import io.openems.common.exceptions.CheckedFunction;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;

/**
 * Holds the value of a configuration property.
 * 
 * <p>
 * This implementation is similar to Optional<>, but it makes a difference if a
 * value is 'null' - i.e. configuration property should be set to 'null' - or
 * 'not set' - i.e. configuration property should not be changed.
 *
 * @param <T>
 */
public class ConfigurationProperty<T> {

	public static <T> ConfigurationProperty<T> of(T value) {
		return new ConfigurationProperty<T>(value);
	}

	public static <T> ConfigurationProperty<T> asNull() {
		return new ConfigurationProperty<T>(null);
	}

	public static <T> ConfigurationProperty<T> asNotSet() {
		return new ConfigurationProperty<T>();
	}

	public static <T> ConfigurationProperty<T> fromJsonElement(Optional<JsonElement> element,
			CheckedFunction<JsonElement, T> function) throws OpenemsNamedException {
		if (element.isPresent()) {
			if (element.get().isJsonNull()) {
				return ConfigurationProperty.asNull();
			} else {
				return ConfigurationProperty.of(function.apply(element.get()));
			}
		} else {
			return ConfigurationProperty.asNotSet();
		}
	}

	private final T value;
	private final boolean isSet;

	private ConfigurationProperty(T value) {
		this.value = value;
		this.isSet = true;
	}

	private ConfigurationProperty() {
		this.value = null;
		this.isSet = false;
	}

	public T getValue() {
		return this.value;
	}

	public boolean isNull() {
		return this.value == null;
	}

	public boolean isSet() {
		return this.isSet;
	}

	public boolean isSetAndNotNull() {
		return this.isSet() && !this.isNull();
	}

}