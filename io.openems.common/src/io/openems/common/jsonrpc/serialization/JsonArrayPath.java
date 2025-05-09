package io.openems.common.jsonrpc.serialization;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSortedSet.toImmutableSortedSet;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Collectors.toUnmodifiableList;

import java.lang.reflect.Array;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Collector;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public interface JsonArrayPath extends JsonPath {

	/**
	 * Collects the values of the current {@link JsonArray} with the provided
	 * {@link Collector}.
	 * 
	 * @param <A>       the mutable accumulation type of the reduction operation
	 *                  (often hidden as an implementation detail)
	 * @param <R>       the result type of the reduction operation
	 * @param collector the {@link Collector} describing the reduction
	 * @return the result of the reduction
	 */
	public <A, R> R collect(Collector<JsonElementPath, A, R> collector);

	/**
	 * Collects all elements into a immutable {@link List}.
	 * 
	 * @param <T>    the type of the elements
	 * @param mapper the mapper to convert the elements
	 * @return the result {@link List} containing all {@link JsonElement
	 *         JsonElements} converted by the provided mapper
	 */
	public default <T> List<T> getAsList(Function<JsonElementPath, T> mapper) {
		return this.collect(mapping(mapper, toUnmodifiableList()));
	}

	/**
	 * Collects all elements into a immutable {@link List}.
	 * 
	 * @param <T>        the type of the elements
	 * @param serializer the {@link JsonSerializer} to convert the elements
	 * @return the result {@link List} containing all {@link JsonElement
	 *         JsonElements} converted by the provided {@link JsonSerializer}
	 */
	public default <T> List<T> getAsList(JsonSerializer<T> serializer) {
		return this.getAsList(serializer::deserializePath);
	}

	/**
	 * Collects all elements into a {@link ImmutableList}.
	 * 
	 * @param <T>    the type of the elements
	 * @param mapper the mapper to convert the elements
	 * @return the result {@link ImmutableList} containing all {@link JsonElement
	 *         JsonElements} converted by the provided mapper
	 */
	public default <T> ImmutableList<T> getAsImmutableList(Function<JsonElementPath, T> mapper) {
		return this.collect(mapping(mapper, toImmutableList()));
	}

	/**
	 * Collects all elements into a {@link ImmutableList}.
	 * 
	 * @param <T>        the type of the elements
	 * @param serializer the {@link JsonSerializer} to convert the elements
	 * @return the result {@link ImmutableList} containing all {@link JsonElement
	 *         JsonElements} converted by the provided {@link JsonSerializer}
	 */
	public default <T> ImmutableList<T> getAsImmutableList(JsonSerializer<T> serializer) {
		return this.getAsImmutableList(serializer::deserializePath);
	}

	/**
	 * Collects all elements into a {@link Set}.
	 * 
	 * @param <T>    the type of the elements
	 * @param mapper the mapper to convert the elements
	 * @return the result {@link Set} containing all {@link JsonElement
	 *         JsonElements} converted by the provided mapper
	 */
	public default <T> Set<T> getAsSet(Function<JsonElementPath, T> mapper) {
		return this.collect(mapping(mapper, toSet()));
	}

	/**
	 * Collects all elements into a {@link Set}.
	 * 
	 * @param <T>        the type of the elements
	 * @param serializer the {@link JsonSerializer} to convert the elements
	 * @return the result {@link Set} containing all {@link JsonElement
	 *         JsonElements} converted by the provided {@link JsonSerializer}
	 */
	public default <T> Set<T> getAsSet(JsonSerializer<T> serializer) {
		return this.getAsSet(serializer::deserializePath);
	}

	/**
	 * Collects all elements into a {@link ImmutableSortedSet}.
	 * 
	 * @param <T>        the type of the elements
	 * @param mapper     the mapper to convert the elements
	 * @param comparator the {@link Comparator} for the {@link ImmutableSortedSet}
	 * @return the result {@link ImmutableSortedSet} containing all
	 *         {@link JsonElement JsonElements} converted by the provided mapper
	 */
	public default <T> ImmutableSortedSet<T> getAsImmutableSortedSet(Function<JsonElementPath, T> mapper,
			Comparator<? super T> comparator) {
		return this.collect(mapping(mapper, toImmutableSortedSet(comparator)));
	}

	/**
	 * Collects all elements into a {@link ImmutableSortedSet}.
	 * 
	 * @param <T>        the type of the elements
	 * @param serializer the {@link JsonSerializer} to convert the elements
	 * @param comparator the {@link Comparator} for the {@link ImmutableSortedSet}
	 * @return the result {@link ImmutableSortedSet} containing all
	 *         {@link JsonElement JsonElements} converted by the provided
	 *         {@link JsonSerializer}
	 */
	public default <T> ImmutableSortedSet<T> getAsImmutableSortedSet(JsonSerializer<T> serializer,
			Comparator<? super T> comparator) {
		return this.getAsImmutableSortedSet(serializer::deserializePath, comparator);
	}

	/**
	 * Collects all elements into a {@link Array}.
	 * 
	 * @param <T>       the type of the elements
	 * @param generator a function which produces a new array of the desired type
	 *                  and the provided length
	 * @param mapper    the mapper to convert the elements
	 * @return the result {@link Array} containing all {@link JsonElement
	 *         JsonElements} converted by the provided mapper
	 */
	public default <T> T[] getAsArray(IntFunction<T[]> generator, Function<JsonElementPath, T> mapper) {
		return this.getAsList(mapper).toArray(generator);
	}

	/**
	 * Collects all elements into a {@link Array}.
	 * 
	 * @param <T>        the type of the elements
	 * @param generator  a function which produces a new array of the desired type
	 *                   and the provided length
	 * @param serializer the {@link JsonSerializer} to convert the elements
	 * @return the result {@link Array} containing all {@link JsonElement
	 *         JsonElements} converted by the provided {@link JsonSerializer}
	 */
	public default <T> T[] getAsArray(IntFunction<T[]> generator, JsonSerializer<T> serializer) {
		return this.getAsList(serializer).toArray(generator);
	}

	/**
	 * Gets the current element of the path.
	 * 
	 * @return the {@link JsonObject}
	 */
	public JsonArray get();

}