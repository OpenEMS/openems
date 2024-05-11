package io.openems.edge.common.jsonapi;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.emptyObjectSerializer;

import io.openems.common.jsonrpc.serialization.JsonSerializer;

public record EmptyObject() {

	/**
	 * Returns a {@link JsonSerializer} for a
	 * {@link JsonApiBuilderTest.EmptyObject}.
	 * 
	 * @return the created {@link JsonSerializer}
	 */
	public static JsonSerializer<EmptyObject> serializer() {
		return emptyObjectSerializer(EmptyObject::new);
	}

}