package io.openems.common.jsonrpc.serialization;

import java.lang.reflect.Array;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntFunction;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import com.google.gson.JsonArray;

public interface JsonArrayPathNullable extends JsonPath {

	/**
	 * Maps the current value using the provided mapper if the current value is not
	 * null otherwise returns null.
	 * 
	 * @param <T>    the type of the mapping result
	 * @param mapper the mapper to convert the non-null {@link JsonArrayPath} to a
	 *               result object
	 * @return the result of the mapper function if the current value is not null
	 *         otherwise null
	 */
	public <T> T mapIfPresent(Function<JsonArrayPath, T> mapper);

	/**
	 * Maps the current value using the provided mapper to a {@link Optional}.
	 * 
	 * @param <T>    the type of the mapping result
	 * @param mapper the mapper to convert the non-null {@link JsonArrayPath} to a
	 *               result object
	 * @return the result of the mapper function of the current value in a
	 *         {@link Optional}
	 */
	public default <T> Optional<T> mapToOptional(Function<JsonArrayPath, T> mapper) {
		return Optional.ofNullable(this.mapIfPresent(mapper));
	}

	/**
	 * Maps the current value to a {@link List} using the provided mapper.
	 * 
	 * @param <T>    the type of the mapping result
	 * @param mapper the mapper to convert the elements
	 * @return a {@link Optional} of the result {@link List}
	 */
	public default <T> Optional<List<T>> getAsOptionalList(Function<JsonElementPath, T> mapper) {
		return this.mapToOptional(t -> t.getAsList(mapper));
	}

	/**
	 * Maps the current value to a {@link List} using the provided mapper.
	 * 
	 * @param <T>        the type of the mapping result
	 * @param serializer the {@link JsonSerializer} to convert elements
	 * @return a {@link Optional} of the result {@link List}
	 */
	public default <T> Optional<List<T>> getAsOptionalList(JsonSerializer<T> serializer) {
		return this.mapToOptional(t -> t.getAsList(serializer));
	}

	/**
	 * Maps the current value to a {@link ImmutableList} using the provided mapper.
	 *
	 * @param <T>    the type of the mapping result
	 * @param mapper the mapper to convert the elements
	 * @return a {@link Optional} of the result {@link ImmutableList}
	 */
	public default <T> Optional<ImmutableList<T>> getAsOptionalImmutableList(Function<JsonElementPath, T> mapper) {
		return this.mapToOptional(t -> t.getAsImmutableList(mapper));
	}

	/**
	 * Maps the current value to a {@link ImmutableList} using the provided mapper.
	 *
	 * @param <T>        the type of the mapping result
	 * @param serializer the {@link JsonSerializer} to convert elements
	 * @return a {@link Optional} of the result {@link ImmutableList}
	 */
	public default <T> Optional<ImmutableList<T>> getAsOptionalImmutableList(JsonSerializer<T> serializer) {
		return this.mapToOptional(t -> t.getAsImmutableList(serializer));
	}

	/**
	 * Maps the current value to a {@link Array} using the provided mapper.
	 * 
	 * @param <T>       the type of the mapping result
	 * @param generator a function which produces a new array of the desired type
	 *                  and the provided length
	 * @param mapper    the mapper to convert the elements
	 * @return a {@link Optional} of the result {@link Array}
	 */
	public default <T> Optional<T[]> getAsOptionalArray(IntFunction<T[]> generator,
			Function<JsonElementPath, T> mapper) {
		return this.mapToOptional(t -> t.getAsArray(generator, mapper));
	}

	/**
	 * Maps the current value to a {@link Array} using the provided mapper.
	 * 
	 * @param <T>        the type of the mapping result
	 * @param generator  a function which produces a new array of the desired type
	 *                   and the provided length
	 * @param serializer the {@link JsonSerializer} to convert elements
	 * @return a {@link Optional} of the result {@link Array}
	 */
	public default <T> Optional<T[]> getAsOptionalArray(IntFunction<T[]> generator, JsonSerializer<T> serializer) {
		return this.mapToOptional(t -> t.getAsArray(generator, serializer));
	}

	/**
	 * Maps the current value to a {@link Set} using the provided mapper.
	 * 
	 * @param <T>    the type of the mapping result
	 * @param mapper the mapper to convert the elements
	 * @return a {@link Optional} of the result {@link Set}
	 */
	public default <T> Optional<Set<T>> getAsOptionalSet(Function<JsonElementPath, T> mapper) {
		return this.mapToOptional(t -> t.getAsSet(mapper));
	}

	/**
	 * Maps the current value to a {@link Set} using the provided mapper.
	 * 
	 * @param <T>        the type of the mapping result
	 * @param serializer the {@link JsonSerializer} to convert elements
	 * @return a {@link Optional} of the result {@link Set}
	 */
	public default <T> Optional<Set<T>> getAsOptionalSet(JsonSerializer<T> serializer) {
		return this.mapToOptional(t -> t.getAsSet(serializer));
	}

	/**
	 * Maps the current value to a {@link ImmutableSortedSet} using the provided
	 * mapper.
	 *
	 * @param <T>        the type of the mapping result
	 * @param mapper     the mapper to convert the elements
	 * @param comparator the {@link Comparator} for the {@link ImmutableSortedSet}
	 * @return a {@link Optional} of the result {@link ImmutableSortedSet}
	 */
	public default <T> Optional<ImmutableSortedSet<T>> getAsOptionalImmutableSortedSet(
			Function<JsonElementPath, T> mapper, Comparator<? super T> comparator) {
		return this.mapToOptional(t -> t.getAsImmutableSortedSet(mapper, comparator));
	}

	/**
	 * Maps the current value to a {@link ImmutableSortedSet} using the provided
	 * mapper.
	 *
	 * @param <T>        the type of the mapping result
	 * @param serializer the {@link JsonSerializer} to convert elements
	 * @param comparator the {@link Comparator} for the {@link ImmutableSortedSet}
	 * @return a {@link Optional} of the result {@link ImmutableSortedSet}
	 */
	public default <T> Optional<ImmutableSortedSet<T>> getAsOptionalImmutableSortedSet(JsonSerializer<T> serializer,
			Comparator<? super T> comparator) {
		return this.mapToOptional(t -> t.getAsImmutableSortedSet(serializer, comparator));
	}

	/**
	 * Checks if the current value is present.
	 * 
	 * @return true if the current value is present; else false
	 */
	public boolean isPresent();

	/**
	 * Gets the current element of the path.
	 * 
	 * @return the {@link JsonArray}
	 */
	public JsonArray getOrNull();

}