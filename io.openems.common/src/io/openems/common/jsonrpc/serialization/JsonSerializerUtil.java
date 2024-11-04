package io.openems.common.jsonrpc.serialization;

import java.util.function.Function;

import com.google.common.base.Supplier;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public final class JsonSerializerUtil {

	/**
	 * Creates a {@link JsonSerializer} for a empty {@link JsonObject}.
	 * 
	 * @param <T>    the type of the object
	 * @param object the object supplier to create an empty instance of it
	 * @return the created {@link JsonSerializer}
	 */
	public static <T> JsonSerializer<T> emptyObjectSerializer(//
			Supplier<T> object //
	) {
		return jsonObjectSerializer(json -> object.get(), json -> new JsonObject());
	}

	/**
	 * Creates a {@link JsonSerializer} for the provided type.
	 * 
	 * @param <T>          the type of the object to serialize and deserialize.
	 * @param clazz        the {@link Class} of the object
	 * @param toObjMapper  the deserializer from {@link JsonObject} to object
	 * @param toJsonMapper the serializer from object to {@link JsonElement}
	 * @return the created {@link JsonSerializer}
	 */
	public static <T> JsonSerializer<T> jsonObjectSerializer(//
			Class<T> clazz, //
			Function<JsonObjectPath, T> toObjMapper, //
			Function<T, JsonElement> toJsonMapper //
	) {
		return jsonObjectSerializer(toObjMapper, toJsonMapper);
	}

	/**
	 * Creates a {@link JsonSerializer} for the provided type.
	 * 
	 * @param <T>          the type of the object to serialize and deserialize.
	 * @param toObjMapper  the deserializer from {@link JsonObject} to object
	 * @param toJsonMapper the serializer from object to {@link JsonElement}
	 * @return the created {@link JsonSerializer}
	 */
	public static <T> JsonSerializer<T> jsonObjectSerializer(//
			Function<JsonObjectPath, T> toObjMapper, //
			Function<T, JsonElement> toJsonMapper //
	) {
		return jsonSerializer(toObjMapper.compose(JsonElementPath::getAsJsonObjectPath), toJsonMapper);
	}

	/**
	 * Creates a {@link JsonSerializer} for the provided type.
	 * 
	 * @param <T>          the type of the object to serialize and deserialize.
	 * @param clazz        the {@link Class} of the object
	 * @param toObjMapper  the deserializer from {@link JsonArray} to object
	 * @param toJsonMapper the serializer from object to {@link JsonElement}
	 * @return the created {@link JsonSerializer}
	 */
	public static <T> JsonSerializer<T> jsonArraySerializer(//
			Class<T> clazz, //
			Function<JsonArrayPath, T> toObjMapper, //
			Function<T, JsonElement> toJsonMapper //
	) {
		return jsonArraySerializer(toObjMapper, toJsonMapper);
	}

	/**
	 * Creates a {@link JsonSerializer} for the provided type.
	 * 
	 * @param <T>          the type of the object to serialize and deserialize.
	 * @param toObjMapper  the deserializer from {@link JsonArray} to object
	 * @param toJsonMapper the serializer from object to {@link JsonElement}
	 * @return the created {@link JsonSerializer}
	 */
	public static <T> JsonSerializer<T> jsonArraySerializer(//
			Function<JsonArrayPath, T> toObjMapper, //
			Function<T, JsonElement> toJsonMapper //
	) {
		return jsonSerializer(toObjMapper.compose(JsonElementPath::getAsJsonArrayPath), toJsonMapper);
	}

	/**
	 * Creates a {@link JsonSerializer} for the provided type.
	 * 
	 * @param <T>          the type of the object to serialize and deserialize.
	 * @param clazz        the {@link Class} of the object
	 * @param toObjMapper  the deserializer from {@link JsonElement} to object
	 * @param toJsonMapper the serializer from object ot {@link JsonElement}
	 * @return the created {@link JsonSerializer}
	 */
	public static <T> JsonSerializer<T> jsonSerializer(//
			Class<T> clazz, //
			Function<JsonElementPath, T> toObjMapper, //
			Function<T, JsonElement> toJsonMapper //
	) {
		return jsonSerializer(toObjMapper, toJsonMapper);
	}

	/**
	 * Creates a {@link JsonSerializer} for the provided type.
	 * 
	 * @param <T>          the type of the object to serialize and deserialize.
	 * @param toObjMapper  the deserializer from {@link JsonElement} to object
	 * @param toJsonMapper the serializer from object to {@link JsonElement}
	 * @return the created {@link JsonSerializer}
	 */
	public static <T> JsonSerializer<T> jsonSerializer(//
			Function<JsonElementPath, T> toObjMapper, //
			Function<T, JsonElement> toJsonMapper //
	) {
		final var path = new JsonElementPathDummy();
		toObjMapper.apply(path);
		return new SimpleJsonSerializer<T>(new SerializerDescriptor(path), toJsonMapper, toObjMapper);
	}

	private JsonSerializerUtil() {
	}

	private static final class SimpleJsonSerializer<T> implements JsonSerializer<T> {

		private final SerializerDescriptor descriptor;
		private final Function<T, JsonElement> serialize;
		private final Function<JsonElementPath, T> deserialize;

		public SimpleJsonSerializer(SerializerDescriptor descriptor, Function<T, JsonElement> serialize,
				Function<JsonElementPath, T> deserialize) {
			super();
			this.descriptor = descriptor;
			this.serialize = serialize;
			this.deserialize = deserialize;
		}

		@Override
		public SerializerDescriptor descriptor() {
			return this.descriptor;
		}

		@Override
		public JsonElement serialize(T a) {
			return this.serialize.apply(a);
		}

		@Override
		public T deserializePath(JsonElementPath a) {
			return this.deserialize.apply(a);
		}

	}

}
