package io.openems.common.jsonrpc.serialization;

import java.util.List;
import java.util.function.Function;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public interface JsonArrayPath extends JsonPath {

	/**
	 * Gets the elements as a list parsed to the object.
	 * 
	 * @param <T>    the type of the objects
	 * @param mapper the {@link JsonElement} to object mapper
	 * @return the list with the parsed values
	 */
	public <T> List<T> getAsList(Function<JsonElementPath, T> mapper);

	/**
	 * Gets the elements as a list parsed to the object.
	 * 
	 * @param <T>        the type of the objects
	 * @param serializer the {@link JsonSerializer} to deserialize the elements
	 * @return the list with the parsed values
	 */
	public default <T> List<T> getAsList(JsonSerializer<T> serializer) {
		return this.getAsList(serializer::deserializePath);
	}

	/**
	 * Gets the current element of the path.
	 * 
	 * @return the {@link JsonObject}
	 */
	public JsonArray get();

}