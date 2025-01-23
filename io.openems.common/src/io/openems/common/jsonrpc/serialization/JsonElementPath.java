package io.openems.common.jsonrpc.serialization;

import com.google.gson.JsonElement;

public interface JsonElementPath extends JsonPath {

	/**
	 * Gets the current {@link JsonElementPath} as a {@link JsonObjectPath}.
	 * 
	 * @return the current element as a {@link JsonObjectPath}
	 */
	public JsonObjectPath getAsJsonObjectPath();

	/**
	 * Gets the current {@link JsonElementPath} as a {@link JsonArrayPath}.
	 * 
	 * @return the current element as a {@link JsonArrayPath}
	 */
	public JsonArrayPath getAsJsonArrayPath();

	/**
	 * Gets the current {@link JsonElementPath} as a {@link StringPath}.
	 * 
	 * @return the current element as a {@link StringPath}
	 */
	public StringPath getAsStringPath();

	/**
	 * Gets the current {@link JsonElementPath} as a {@link String}.
	 * 
	 * @return the current element as a {@link String}
	 */
	public default String getAsString() {
		return this.getAsStringPath().get();
	}

	/**
	 * Gets the current {@link JsonElementPath} as a Object serialized with the
	 * provided {@link JsonSerializer}.
	 * 
	 * @param <O>        the type of the final object
	 * @param serializer the {@link JsonSerializer} to deserialize the
	 *                   {@link JsonElement} to the object
	 * @return the current element as a {@link StringPath}
	 */
	public <O> O getAsObject(JsonSerializer<O> serializer);

}