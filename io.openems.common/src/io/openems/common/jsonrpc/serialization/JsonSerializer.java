package io.openems.common.jsonrpc.serialization;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonSerializer;
import static io.openems.common.utils.JsonUtils.toJsonArray;

import java.util.List;

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

	/**
	 * Creates a new {@link JsonSerializer} which is able to serialize {@link List
	 * Lists} with their generic type of the current {@link JsonSerializer}.
	 * 
	 * @return the new {@link JsonSerializer} of a {@link List}
	 */
	public default JsonSerializer<List<T>> toListSerializer() {
		return jsonSerializer(//
				json -> json.getAsJsonArrayPath().getAsList(this), //
				obj -> obj.stream() //
						.map(this::serialize) //
						.collect(toJsonArray()));
	}

}
