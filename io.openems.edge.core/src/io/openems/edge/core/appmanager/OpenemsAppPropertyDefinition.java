package io.openems.edge.core.appmanager;

import java.util.Optional;
import java.util.function.Function;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.session.Language;

public class OpenemsAppPropertyDefinition {

	public final String name;

	public final Function<Language, JsonElement> defaultValue;

	public final boolean isAllowedToSave;

	public final Function<JsonObject, JsonElement> bidirectionalValue;

	public OpenemsAppPropertyDefinition(//
			final String name, //
			final Function<Language, JsonElement> defaultValue, //
			final boolean isAllowedToSave, //
			final Function<JsonObject, JsonElement> bidirectionalValue //
	) {
		this.name = name;
		this.defaultValue = defaultValue;
		this.isAllowedToSave = isAllowedToSave;
		this.bidirectionalValue = bidirectionalValue;
	}

	/**
	 * Gets an {@link Optional} of the defaultValue.
	 * 
	 * @param language the {@link Language} of the value
	 * @return the defaultValue
	 */
	public final Optional<JsonElement> getDefaultValue(Language language) {
		return Optional.ofNullable(this.defaultValue) //
				.map(t -> t.apply(language));
	}

}
