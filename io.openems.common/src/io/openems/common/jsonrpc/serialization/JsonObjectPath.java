package io.openems.common.jsonrpc.serialization;

import java.util.List;
import java.util.function.Function;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public interface JsonObjectPath extends JsonPath {

	/**
	 * Gets the element associated with the member name from this object.
	 * 
	 * @param member the name of the member
	 * @return the {@link JsonElementPath} of the member value
	 */
	public JsonElementPath getJsonElementPath(String member);

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link StringPath}.
	 * 
	 * @param member the name of the member
	 * @return the {@link StringPath} of the member value
	 */
	public default StringPath getStringPath(String member) {
		return this.getJsonElementPath(member).getAsStringPath();
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link String}.
	 * 
	 * @param member the name of the member
	 * @return the {@link String} of the member value
	 */
	public default String getString(String member) {
		return this.getStringPath(member).get();
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link JsonObjectPath}.
	 * 
	 * @param member the name of the member
	 * @return the {@link JsonObjectPath} of the member value
	 */
	public default JsonObjectPath getJsonObjectPath(String member) {
		return this.getJsonElementPath(member).getAsJsonObjectPath();
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link JsonObject}.
	 * 
	 * @param member the name of the member
	 * @return the {@link JsonObject} of the member value
	 */
	public default JsonObject getJsonObject(String member) {
		return this.getJsonObjectPath(member).get();
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link JsonArrayPath}.
	 * 
	 * @param member the name of the member
	 * @return the {@link JsonArrayPath} of the member value
	 */
	public default JsonArrayPath getJsonArrayPath(String member) {
		return this.getJsonElementPath(member).getAsJsonArrayPath();
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link JsonArray}.
	 * 
	 * @param member the name of the member
	 * @return the {@link JsonArray} of the member value
	 */
	public default JsonArray getJsonArray(String member) {
		return this.getJsonArrayPath(member).get();
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link List}.
	 * 
	 * @param <T>    the type of the elements in the list
	 * @param member the name of the member
	 * @param mapper the mapper to deserialize the elements
	 * @return the {@link List} of the member value
	 */
	public default <T> List<T> getList(String member, Function<JsonElementPath, T> mapper) {
		return this.getJsonArrayPath(member).getAsList(mapper);
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link List}.
	 * 
	 * @param <T>        the type of the elements in the list
	 * @param member     the name of the member
	 * @param serializer the {@link JsonSerializer} to deserialize the elements
	 * @return the {@link List} of the member value
	 */
	public default <T> List<T> getList(String member, JsonSerializer<T> serializer) {
		return this.getJsonArrayPath(member).getAsList(serializer);
	}

	/**
	 * Gets the element associated with the member name from this object as the
	 * generic object.
	 * 
	 * @param <T>        the type of the element
	 * @param member     the name of the member
	 * @param serializer the {@link JsonSerializer} to deserialize the element
	 * @return the object of the member value
	 */
	public default <T> T getElement(String member, JsonSerializer<T> serializer) {
		return this.getJsonElementPath(member).getAsObject(serializer);
	}

	/**
	 * Gets the current element of the path.
	 * 
	 * @return the {@link JsonObject}
	 */
	public JsonObject get();

}