package io.openems.common.jsonrpc.serialization;

import java.util.function.Function;

import com.google.gson.JsonObject;

public interface JsonObjectPathNullable extends JsonPath {

	/**
	 * Maps the current value if present with the provided mapping function other
	 * returns null.
	 * 
	 * @param <T>    the type of the result object
	 * @param mapper the mapping function
	 * @return the result object or null if the current value is not present
	 */
	public <T> T mapIfPresent(Function<JsonObjectPath, T> mapper);

	/**
	 * Checks if the current value is present.
	 * 
	 * @return true if the current value is present; else false
	 */
	public boolean isPresent();

	/**
	 * Gets the {@link JsonObject} value of the current path.
	 * 
	 * @return the value; or null if not present
	 */
	public JsonObject getOrNull();
}