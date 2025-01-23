package io.openems.common.jsonrpc.serialization;

import com.google.gson.JsonElement;

public class SerializerDescriptor {

	private final JsonElementPathDummy obj;

	public SerializerDescriptor(JsonElementPathDummy obj) {
		this.obj = obj;
	}

	/**
	 * Creates a {@link JsonElement} of the object description.
	 * 
	 * @return the created {@link JsonElementPath}
	 */
	public JsonElement toJson() {
		return this.obj.buildPath();
	}

	public JsonElementPathDummy getObj() {
		return this.obj;
	}

}
