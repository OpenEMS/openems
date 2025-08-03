package io.openems.edge.predictor.api.mlcore.datastructures;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Function;
import java.util.stream.IntStream;

import io.openems.edge.predictor.api.mlcore.transformer.SeriesTransformer;

public class Series<I> implements DataStructure<I> {

	private final List<I> index;
	private final List<Double> values;
	private final Map<I, Integer> indexToPos;

	public Series(List<I> index, List<Double> values) {
		Objects.requireNonNull(index, "Index must not be null");
		Objects.requireNonNull(values, "Values must not be null");

		if (index.size() != values.size()) {
			throw new IllegalArgumentException("Index and values must have the same length");
		}

		this.index = new ArrayList<>(index);
		this.values = new ArrayList<>(values);
		this.indexToPos = new HashMap<>();
		for (int i = 0; i < this.index.size(); i++) {
			I idx = this.index.get(i);
			if (this.indexToPos.putIfAbsent(idx, i) != null) {
				throw new IllegalArgumentException("Duplicate index found: " + idx);
			}
		}
	}

	/**
	 * Applies the given {@link SeriesTransformer} to this {@code Series} and
	 * returns a new transformed {@code Series}.
	 *
	 * @param transformer the transformer to apply
	 * @return a new {@code Series} resulting from applying the transformer
	 */
	public Series<I> applyTransformer(SeriesTransformer<I> transformer) {
		return transformer.transform(this);
	}

	// --- accessors / getters --- //

	@Override
	public List<I> getIndex() {
		return Collections.unmodifiableList(this.index);
	}

	/**
	 * Returns an unmodifiable list of the values.
	 *
	 * @return unmodifiable list of values
	 */
	public List<Double> getValues() {
		return Collections.unmodifiableList(this.values);
	}

	/**
	 * Returns the number of elements in the series.
	 *
	 * @return size of the series
	 */
	public int size() {
		return this.index.size();
	}

	/**
	 * Returns the value associated with the given index.
	 *
	 * @param idx index key
	 * @return value at the specified index
	 * @throws IllegalArgumentException if index is not found
	 */
	public Double get(I idx) {
		var pos = this.indexToPos.get(idx);
		if (pos == null) {
			throw new IllegalArgumentException("Index not found: " + idx);
		}
		return this.values.get(pos);
	}

	/**
	 * Retrieves the value at the specified position.
	 *
	 * @param position index of the value to return
	 * @return value at the given position
	 * @throws IndexOutOfBoundsException if position is out of range
	 */
	public Double getAt(int position) {
		return this.values.get(position);
	}

	// --- mutators / setters ---

	/**
	 * Sets the value at the specified index.
	 *
	 * @param idx   index key
	 * @param value value to set
	 * @throws IllegalArgumentException if index is not found
	 */
	public void setValue(I idx, Double value) {
		var pos = this.indexToPos.get(idx);
		if (pos == null) {
			throw new IllegalArgumentException("Index not found: " + idx);
		}
		this.values.set(pos, value);
	}

	/**
	 * Sets the value at the specified position.
	 *
	 * @param position position in the values list
	 * @param value    value to set
	 * @throws IndexOutOfBoundsException if position is out of range
	 */
	public void setValueAt(int position, Double value) {
		this.values.set(position, value);
	}

	/**
	 * Removes the element with the specified index.
	 *
	 * @param idx index key to remove
	 * @throws IllegalArgumentException if index is not found
	 */
	public void remove(I idx) {
		var pos = this.indexToPos.get(idx);
		if (pos == null) {
			throw new IllegalArgumentException("Index not found: " + idx);
		}
		this.removeAt(pos);
	}

	/**
	 * Removes the element at the specified position.
	 *
	 * @param position position of the element to remove
	 * @throws IndexOutOfBoundsException if position is out of range
	 */
	public void removeAt(int position) {
		this.index.remove(position);
		this.values.remove(position);
		this.indexToPos.clear();
		for (int i = 0; i < this.size(); i++) {
			this.indexToPos.put(this.index.get(i), i);
		}
	}

	/**
	 * Adds a new index-value pair.
	 *
	 * @param idx   index key to add
	 * @param value value associated with the index
	 * @throws IllegalArgumentException if index already exists
	 */
	public void add(I idx, Double value) {
		if (this.indexToPos.containsKey(idx)) {
			throw new IllegalArgumentException("Index already exists: " + idx);
		}
		this.index.add(idx);
		this.values.add(value);
		this.indexToPos.put(idx, this.size() - 1);
	}

	// --- utility ---

	/**
	 * Converts the series to a LinkedHashMap of index-value pairs.
	 *
	 * @return map of indices to values
	 */
	public Map<I, Double> toMap() {
		var map = new LinkedHashMap<I, Double>();
		for (int i = 0; i < this.size(); i++) {
			map.put(this.index.get(i), this.values.get(i));
		}
		return map;
	}

	@Override
	public void dropNa() {
		var newIndex = new ArrayList<I>();
		var newValues = new ArrayList<Double>();
		for (int i = 0; i < this.size(); i++) {
			var value = this.values.get(i);
			if (value != null && !value.isNaN()) {
				newIndex.add(this.index.get(i));
				newValues.add(value);
			}
		}

		this.update(newIndex, newValues);
	}

	@Override
	public void sortByIndex(Comparator<I> comparator) {
		var positions = IntStream.range(0, this.size())//
				.boxed()//
				.sorted(Comparator.comparing(this.index::get, comparator))//
				.toList();

		var newIndex = new ArrayList<I>(this.size());
		var newValues = new ArrayList<Double>(this.size());

		for (int pos : positions) {
			newIndex.add(this.index.get(pos));
			newValues.add(this.values.get(pos));
		}

		this.update(newIndex, newValues);
	}

	@Override
	public OptionalDouble min() {
		return this.values.stream()//
				.filter(Objects::nonNull)//
				.mapToDouble(Double::doubleValue)//
				.min();
	}

	@Override
	public OptionalDouble max() {
		return this.values.stream()//
				.filter(Objects::nonNull)//
				.mapToDouble(Double::doubleValue)//
				.max();
	}

	@Override
	public void apply(Function<Double, Double> function) {
		var newValues = new ArrayList<Double>();

		for (int i = 0; i < this.size(); i++) {
			newValues.add(//
					Optional.ofNullable(this.values.get(i))//
							.map(function)//
							.orElse(null));
		}

		this.update(List.copyOf(this.index), newValues);
	}

	@Override
	public Series<I> copy() {
		return new Series<>(this.index, this.values);
	}

	// --- private helpers --- //

	private void update(List<I> newIndex, List<Double> newValues) {
		this.index.clear();
		this.index.addAll(newIndex);

		this.values.clear();
		this.values.addAll(newValues);

		this.indexToPos.clear();
		for (int i = 0; i < this.size(); i++) {
			this.indexToPos.put(this.index.get(i), i);
		}
	}

	// --- equals and hashCode ---

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Series<?> other)) {
			return false;
		}
		return Objects.equals(this.index, other.index) //
				&& Objects.equals(this.values, other.values);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.index, this.values);
	}
}
