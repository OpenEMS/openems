package io.openems.common.jsonrpc.serialization;

import com.google.gson.JsonElement;

public interface JsonSerializer<T> {

	/**
	 * Gets the {@link SerializerDescriptor} of the object this serializer
	 * serializes.
	 * 
	 * @return the {@link SerializerDescriptor}
	 */
	public SerializerDescriptor descriptor();

	/**
	 * Serializes from a object to a {@link JsonElement}.
	 * 
	 * @param obj the object to serialize
	 * @return the serialized object as a {@link JsonElement}
	 */
	public JsonElement serialize(T obj);

	/**
	 * Deserializes from a {@link JsonElement} to the object.
	 * 
	 * @param json the {@link JsonElement} to deserialize into a object
	 * @return the deserialized object from the {@link JsonElement}
	 */
	public T deserializePath(JsonElementPath json);

	/**
	 * Deserializes from a {@link JsonElement} to the object.
	 * 
	 * @param json the {@link JsonElement} to deserialize into a object
	 * @return the deserialized object from the {@link JsonElement}
	 */
	public default T deserialize(JsonElement json) {
		return this.deserializePath(new JsonElementPathActual(json));
	}

}
