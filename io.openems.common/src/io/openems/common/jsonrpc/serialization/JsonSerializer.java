package io.openems.common.jsonrpc.serialization;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonSerializer;
import static io.openems.common.utils.JsonUtils.toJsonArray;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.IntFunction;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.exceptions.OpenemsRuntimeException;
import io.openems.common.utils.JsonUtils;

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
	 * Deserializes from a {@link String} to the object.
	 * 
	 * @param string the {@link String} to deserialize into a object
	 * @return the deserialized object from the {@link JsonElement}
	 * @throws OpenemsNamedException on parse error
	 */
	public default T deserialize(String string) throws OpenemsNamedException {
		try {
			return this.deserialize(JsonUtils.parse(string));
		} catch (OpenemsRuntimeException e) {
			throw new OpenemsException(e);
		}
	}

	/**
	 * Deserializes from a {@link JsonElement} to the object.
	 * 
	 * @param json the {@link JsonElement} to deserialize into a object
	 * @return the deserialized object from the {@link JsonElement}
	 */
	public default T deserialize(JsonElement json) {
		return this.deserializePath(new JsonElementPathActual.JsonElementPathActualNonNull(json));
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

	/**
	 * Creates a new {@link JsonSerializer} which is able to serialize
	 * {@link ImmutableList ImmutableLists} with their generic type of the current
	 * {@link JsonSerializer}.
	 *
	 * @return the new {@link JsonSerializer} of a {@link ImmutableList}
	 */
	public default JsonSerializer<ImmutableList<T>> toImmutableListSerializer() {
		return jsonSerializer(//
				json -> json.getAsJsonArrayPath().getAsImmutableList(this), //
				obj -> obj.stream() //
						.map(this::serialize) //
						.collect(toJsonArray()));
	}

	/**
	 * Creates a new {@link JsonSerializer} which is able to serialize {@link Set
	 * Sets} with their generic type of the current {@link JsonSerializer}.
	 * 
	 * @return the new {@link JsonSerializer} of a {@link Set}
	 */
	public default JsonSerializer<Set<T>> toSetSerializer() {
		return jsonSerializer(//
				json -> json.getAsJsonArrayPath().getAsSet(this), //
				obj -> obj.stream() //
						.map(this::serialize) //
						.collect(toJsonArray()));
	}

	/**
	 * Creates a new {@link JsonSerializer} which is able to serialize {@link Array
	 * Arrays} with their type of the current {@link JsonSerializer}.
	 * 
	 * @param generator a function which produces a new array of the desired type
	 *                  and the provided length
	 * @return the new {@link JsonSerializer} of a {@link Array}
	 */
	public default JsonSerializer<T[]> toArraySerializer(IntFunction<T[]> generator) {
		return jsonSerializer(//
				json -> json.getAsJsonArrayPath().getAsList(this).toArray(generator), //
				obj -> Arrays.stream(obj) //
						.map(this::serialize) //
						.collect(toJsonArray()));
	}

}
