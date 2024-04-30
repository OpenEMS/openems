package io.openems.common.jsonrpc.serialization;

import com.google.gson.JsonElement;

public interface JsonPathDummy {

	/**
	 * Creates the description of the Path as a {@link JsonElement}.
	 * 
	 * @return the created {@link JsonElement}
	 */
	public JsonElement buildPath();

}
