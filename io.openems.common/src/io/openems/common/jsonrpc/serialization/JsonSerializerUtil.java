package io.openems.common.jsonrpc.serialization;

import static io.openems.common.utils.FunctionUtils.lazySingleton;

import java.util.function.Function;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public final class JsonSerializerUtil {

	private static final Logger LOG = LoggerFactory.getLogger(JsonSerializerUtil.class);

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
	 * Returns a {@link JsonSerializer} for a {@link String}.
	 * 
	 * @return the created {@link JsonSerializer}
	 */
	public static JsonSerializer<String> stringSerializer() {
		return jsonSerializer(String.class, JsonElementPath::getAsString, JsonPrimitive::new);
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
		return new SimpleJsonSerializer<>(toJsonMapper, toObjMapper);
	}

	private JsonSerializerUtil() {
	}

	private static final class SimpleJsonSerializer<T> implements JsonSerializer<T> {

		private final Supplier<SerializerDescriptor> descriptor;
		private final Function<T, JsonElement> serialize;
		private final Function<JsonElementPath, T> deserialize;

		public SimpleJsonSerializer(Function<T, JsonElement> serialize, Function<JsonElementPath, T> deserialize) {
			super();
			this.descriptor = lazySingleton(this::createSerializerDescriptor);
			this.serialize = serialize;
			this.deserialize = deserialize;
		}

		@Override
		public SerializerDescriptor descriptor() {
			return this.descriptor.get();
		}

		@Override
		public JsonElement serialize(T a) {
			return this.serialize.apply(a);
		}

		@Override
		public T deserializePath(JsonElementPath a) {
			return this.deserialize.apply(a);
		}

		private SerializerDescriptor createSerializerDescriptor() {
			final var path = new JsonElementPathDummy.JsonElementPathDummyNonNull();
			try {
				this.deserialize.apply(path);
			} catch (RuntimeException e) {
				LOG.error("Unexpected error while trying to create SerializerDescriptor", e);
			}
			return new SerializerDescriptor(path);
		}

	}

}
