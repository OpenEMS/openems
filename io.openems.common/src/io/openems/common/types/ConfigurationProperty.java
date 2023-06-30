package io.openems.common.types;

import java.util.Optional;

import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingFunction;

/**
 * Holds the value of a configuration property.
 *
 * <p>
 * This implementation is similar to Optional<>, but it makes a difference if a
 * value is 'null' - i.e. configuration property should be set to 'null' - or
 * 'not set' - i.e. configuration property should not be changed.
 *
 * @param <T> type of the configuration property
 */
public class ConfigurationProperty<T> {

	/**
	 * Creates a {@link ConfigurationProperty} object from a value.
	 *
	 * @param <T>   the type of the value
	 * @param value the value
	 * @return the {@link ConfigurationProperty}
	 */
	public static <T> ConfigurationProperty<T> of(T value) {
		return new ConfigurationProperty<>(value);
	}

	/**
	 * Creates a {@link ConfigurationProperty} object with 'null' value.
	 *
	 * @param <T> the type of the value
	 * @return the {@link ConfigurationProperty}
	 */
	public static <T> ConfigurationProperty<T> asNull() {
		return new ConfigurationProperty<>(null);
	}

	/**
	 * Creates a {@link ConfigurationProperty} object with 'not set' value.
	 *
	 * @param <T> the type of the value
	 * @return the {@link ConfigurationProperty}
	 */
	public static <T> ConfigurationProperty<T> asNotSet() {
		return new ConfigurationProperty<>();
	}

	/**
	 * Creates a {@link ConfigurationProperty} object from a {@link JsonElement}
	 * value.
	 *
	 * @param <T>      the type of the value
	 * @param element  the {@link JsonElement} value
	 * @param function conversion function from {@link JsonElement} to type T
	 * @return the {@link ConfigurationProperty}
	 * @throws OpenemsNamedException on error
	 */
	public static <T> ConfigurationProperty<T> fromJsonElement(Optional<JsonElement> element,
			ThrowingFunction<JsonElement, T, OpenemsNamedException> function) throws OpenemsNamedException {
		if (!element.isPresent()) {
			return ConfigurationProperty.asNotSet();
		}
		if (element.get().isJsonNull()) {
			return ConfigurationProperty.asNull();
		} else {
			return ConfigurationProperty.of(function.apply(element.get()));
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