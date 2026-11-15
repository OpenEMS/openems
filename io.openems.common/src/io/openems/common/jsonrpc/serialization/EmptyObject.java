package io.openems.common.jsonrpc.serialization;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;

import com.google.gson.JsonObject;

public final class EmptyObject {

	public static final EmptyObject INSTANCE = new EmptyObject();

	/**
	 * Returns a {@link JsonSerializer} for a {@link EmptyObject}.
	 * 
	 * @return the created {@link JsonSerializer}
	 */
	public static JsonSerializer<EmptyObject> serializer() {
		return jsonObjectSerializer(EmptyObject.class, //
				_ -> EmptyObject.INSTANCE, //
				_ -> new JsonObject());
	}

	private EmptyObject() {
	}

}
