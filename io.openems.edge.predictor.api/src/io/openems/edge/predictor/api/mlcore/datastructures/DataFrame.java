package io.openems.edge.predictor.api.mlcore.datastructures;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.openems.edge.predictor.api.mlcore.transformer.DataFrameTransformer;

public class DataFrame<I> implements DataStructure<I> {

	private final List<I> index;
	private final List<String> columnNames;
	private final List<List<Double>> values;
	private final Map<I, Integer> indexToRowPos;
	private final Map<String, Integer> columnNameToColumnPos;

	public DataFrame() {
		this.index = new ArrayList<>();
		this.columnNames = new ArrayList<>();
		this.values = new ArrayList<>();
		this.indexToRowPos = new HashMap<>();
		this.columnNameToColumnPos = new HashMap<>();
	}

	public DataFrame(List<I> index, List<String> columnNames, List<List<Double>> values) {
		Objects.requireNonNull(index, "Index must not be null");
		Objects.requireNonNull(columnNames, "Column names must not be null");
		Objects.requireNonNull(values, "Values must not be null");

		if (index.size() != values.size()) {
			throw new IllegalArgumentException("Index size must match number of rows");
		}

		for (var row : values) {
			if (row.size() != columnNames.size()) {
				throw new IllegalArgumentException("Each row must have the same number of elements as columns");
			}
		}

		this.checkForDuplicates(index, "index");
		this.checkForDuplicates(columnNames, "column name");

		this.index = new ArrayList<>(index);
		this.columnNames = new ArrayList<>(columnNames);
		this.values = values.stream()//
				.map(ArrayList::new)//
				.collect(Collectors.toList());

		this.indexToRowPos = new HashMap<>();
		this.columnNameToColumnPos = new HashMap<>();
		this.rebuildIndices();
	}

	/**
	 * Applies the given {@link DataFrameTransformer} to this {@code DataFrame} and
	 * returns a new transformed {@code DataFrame}.
	 *
	 * @param transformer the transformer to apply
	 * @return a new {@code DataFrame} resulting from applying the transformer
	 */
	public DataFrame<I> applyTransformer(DataFrameTransformer<I> transformer) {
		return transformer.transform(this);
	}

	// --- factory methods --- //

	/**
	 * Creates a DataFrame from a map of column names to Series.
	 *
	 * @param seriesMap map of column names to Series
	 * @param <I>       type of the row index
	 * @return DataFrame constructed from the series map
	 * @throws IllegalArgumentException if series have different indices
	 */
	public static <I> DataFrame<I> fromSeriesMap(Map<String, Series<I>> seriesMap) {
		if (seriesMap == null || seriesMap.isEmpty()) {
			return new DataFrame<>();
		}

		List<I> index = null;
		var columnNames = new ArrayList<String>(seriesMap.keySet());
		var values = new ArrayList<List<Double>>();

		for (var colName : columnNames) {
			var series = seriesMap.get(colName);
			if (index == null) {
				index = series.getIndex();
				for (int i = 0; i < index.size(); i++) {
					values.add(new ArrayList<>());
				}
			} else {
				if (!series.getIndex().equals(index)) {
					throw new IllegalArgumentException("All series must have the same index");
				}
			}

			var seriesValues = series.getValues();
			for (int i = 0; i < seriesValues.size(); i++) {
				values.get(i).add(seriesValues.get(i));
			}
		}

		return new DataFrame<>(index, columnNames, values);
	}

	// --- accessors / getters --- //

	@Override
	public List<I> getIndex() {
		return Collections.unmodifiableList(this.index);
	}

	/**
	 * Returns a list of column names.
	 *
	 * @return copy of the column names list
	 */
	public List<String> getColumnNames() {
		return new ArrayList<>(this.columnNames);
	}

	/**
	 * Returns an unmodifiable view of the values matrix.
	 *
	 * @return unmodifiable list of rows with unmodifiable row values
	 */
	public List<List<Double>> getValues() {
		return this.values.stream()//
				.map(Collections::unmodifiableList)//
				.toList();
	}

	/**
	 * Returns the number of rows in the DataFrame.
	 *
	 * @return number of rows
	 */
	public int rowCount() {
		return this.index.size();
	}

	/**
	 * Returns the number of columns in the DataFrame.
	 *
	 * @return number of columns
	 */
	public int columnCount() {
		return this.columnNames.size();
	}

	/**
	 * Returns the series for the specified column name.
	 *
	 * @param columnName name of the column
	 * @return series of values for the column
	 * @throws IllegalArgumentException if column does not exist
	 */
	public Series<I> getColumn(String columnName) {
		var colPos = this.columnNameToColumnPos.get(columnName);
		if (colPos == null) {
			throw new IllegalArgumentException("Column not found: " + columnName);
		}
		return this.getColumnAt(colPos);
	}

	/**
	 * Returns the series for the specified column position.
	 *
	 * @param columnPosition position of the column
	 * @return series of values for the column
	 * @throws IndexOutOfBoundsException if position is invalid
	 */
	public Series<I> getColumnAt(int columnPosition) {
		if (columnPosition < 0 || columnPosition >= this.columnNames.size()) {
			throw new IndexOutOfBoundsException("Invalid column position: " + columnPosition);
		}
		var columnValues = new ArrayList<Double>(this.values.size());
		for (var row : this.values) {
			columnValues.add(row.get(columnPosition));
		}
		return new Series<>(this.index, columnValues);
	}

	/**
	 * Returns a copy of the row values for the given row index.
	 *
	 * @param rowIndex index of the row
	 * @return list of values in the row
	 * @throws IllegalArgumentException if row index is not found
	 */
	public List<Double> getRow(I rowIndex) {
		var rowPos = this.indexToRowPos.get(rowIndex);
		if (rowPos == null) {
			throw new IllegalArgumentException("Row index not found: " + rowIndex);
		}
		return this.getRowAt(rowPos);
	}

	/**
	 * Returns a copy of the row values at the specified position.
	 *
	 * @param rowPosition position of the row
	 * @return list of values in the row
	 * @throws IndexOutOfBoundsException if position is out of range
	 */
	public List<Double> getRowAt(int rowPosition) {
		if (rowPosition < 0 || rowPosition >= this.values.size()) {
			throw new IndexOutOfBoundsException("Invalid row position: " + rowPosition);
		}
		return new ArrayList<>(this.values.get(rowPosition));
	}

	/**
	 * Returns the value at the specified row index and column name.
	 *
	 * @param rowIndex   row index key
	 * @param columnName name of the column
	 * @return value at the specified position
	 * @throws IllegalArgumentException if row index or column name is not found
	 */
	public Double getValue(I rowIndex, String columnName) {
		var rowPosition = this.indexToRowPos.get(rowIndex);
		if (rowPosition == null) {
			throw new IllegalArgumentException("Row index not found: " + rowIndex);
		}
		var colPosition = this.columnNameToColumnPos.get(columnName);
		if (colPosition == null) {
			throw new IllegalArgumentException("Column name not found: " + columnName);
		}
		return this.getValueAt(rowPosition, colPosition);
	}

	/**
	 * Returns the value at the specified row and column positions.
	 *
	 * @param rowPosition    position of the row
	 * @param columnPosition position of the column
	 * @return value at the specified position
	 * @throws IndexOutOfBoundsException if row or column position is out of range
	 */
	public Double getValueAt(int rowPosition, int columnPosition) {
		if (rowPosition < 0 || rowPosition >= this.values.size()) {
			throw new IndexOutOfBoundsException("Invalid row position: " + rowPosition);
		}
		if (columnPosition < 0 || columnPosition >= this.columnNames.size()) {
			throw new IndexOutOfBoundsException("Invalid column position: " + columnPosition);
		}
		return this.values.get(rowPosition).get(columnPosition);
	}

	// --- mutators / setters ---

	/**
	 * Adds a new row with the specified index and values.
	 *
	 * @param rowIndex  index key of the new row
	 * @param rowValues list of values for the new row
	 * @throws NullPointerException     if rowIndex or rowValues is null
	 * @throws IllegalArgumentException if row index exists or row size mismatches
	 *                                  columns
	 */
	public void addRow(I rowIndex, List<Double> rowValues) {
		Objects.requireNonNull(rowIndex, "Row index must not be null");
		Objects.requireNonNull(rowValues, "Row values must not be null");

		if (this.indexToRowPos.containsKey(rowIndex)) {
			throw new IllegalArgumentException("Row with index '" + rowIndex + "' already exists");
		}

		if (rowValues.size() != this.columnNames.size()) {
			throw new IllegalArgumentException(
					"Row size must match number of columns (" + this.columnNames.size() + ")");
		}

		this.index.add(rowIndex);
		this.values.add(new ArrayList<>(rowValues));
		this.indexToRowPos.put(rowIndex, this.index.size() - 1);
	}

	/**
	 * Adds a new empty row with the specified index.
	 *
	 * @param rowIndex index key of the new row
	 * @throws NullPointerException     if rowIndex is null
	 * @throws IllegalArgumentException if row index already exists
	 */
	public void addEmptyRow(I rowIndex) {
		Objects.requireNonNull(rowIndex, "Row index must not be null");

		if (this.indexToRowPos.containsKey(rowIndex)) {
			throw new IllegalArgumentException("Row with index '" + rowIndex + "' already exists");
		}

		this.index.add(rowIndex);
		var row = new ArrayList<Double>(this.columnCount());
		for (int i = 0; i < this.columnCount(); i++) {
			row.add(Double.NaN);
		}
		this.values.add(row);
		this.indexToRowPos.put(rowIndex, this.index.size() - 1);
	}

	/**
	 * Sets or adds a column with the given name and series.
	 *
	 * @param columnName name of the column
	 * @param series     series with values matching the DataFrame index
	 * @throws NullPointerException     if columnName or series is null
	 * @throws IllegalArgumentException if series index does not match DataFrame
	 *                                  index
	 */
	public void setColumn(String columnName, Series<I> series) {
		Objects.requireNonNull(columnName, "Column name must not be null");
		Objects.requireNonNull(series, "Series must not be null");

		if (!series.getIndex().equals(this.index)) {
			throw new IllegalArgumentException("Series index must match DataFrame index");
		}

		var colPos = this.columnNameToColumnPos.get(columnName);
		if (colPos == null) {
			this.columnNames.add(columnName);
			this.columnNameToColumnPos.put(columnName, this.columnNames.size() - 1);
			for (int i = 0; i < this.values.size(); i++) {
				this.values.get(i).add(series.getValues().get(i));
			}
			return;
		}

		for (int i = 0; i < this.values.size(); i++) {
			this.values.get(i).set(colPos, series.getValues().get(i));
		}
	}

	/**
	 * Sets the value at the specified row index and column name.
	 *
	 * @param rowIndex   row index key
	 * @param columnName name of the column
	 * @param value      value to set
	 * @throws NullPointerException     if rowIndex or columnName is null
	 * @throws IllegalArgumentException if row index or column name is not found
	 */
	public void setValue(I rowIndex, String columnName, Double value) {
		Objects.requireNonNull(rowIndex, "Row index must not be null");
		Objects.requireNonNull(columnName, "Column name must not be null");

		var rowPos = this.indexToRowPos.get(rowIndex);
		if (rowPos == null) {
			throw new IllegalArgumentException("Row index not found: " + rowIndex);
		}

		var colPos = this.columnNameToColumnPos.get(columnName);
		if (colPos == null) {
			throw new IllegalArgumentException("Column name not found: " + columnName);
		}

		this.values.get(rowPos).set(colPos, value);
	}

	/**
	 * Sets the value at the specified row and column positions.
	 *
	 * @param rowPosition    position of the row
	 * @param columnPosition position of the column
	 * @param value          value to set
	 * @throws IndexOutOfBoundsException if row or column position is out of range
	 */
	public void setValueAt(int rowPosition, int columnPosition, Double value) {
		if (rowPosition < 0 || rowPosition >= this.values.size()) {
			throw new IndexOutOfBoundsException("Row position out of bounds: " + rowPosition);
		}

		if (columnPosition < 0 || columnPosition >= this.columnNames.size()) {
			throw new IndexOutOfBoundsException("Column position out of bounds: " + columnPosition);
		}

		this.values.get(rowPosition).set(columnPosition, value);
	}

	/**
	 * Removes the row with the specified index.
	 *
	 * @param rowIndex index key of the row to remove
	 * @throws NullPointerException     if rowIndex is null
	 * @throws IllegalArgumentException if row index is not found
	 */
	public void removeRow(I rowIndex) {
		Objects.requireNonNull(rowIndex, "Row index must not be null");
		var rowPos = this.indexToRowPos.get(rowIndex);
		if (rowPos == null) {
			throw new IllegalArgumentException("Row index not found: " + rowIndex);
		}
		this.index.remove((int) rowPos);
		this.values.remove((int) rowPos);
		this.rebuildIndices();
	}

	/**
	 * Removes the column with the specified name.
	 *
	 * @param columnName name of the column to remove
	 * @throws NullPointerException     if columnName is null
	 * @throws IllegalArgumentException if column name is not found
	 */
	public void removeColumn(String columnName) {
		Objects.requireNonNull(columnName, "Column name must not be null");
		var colPos = this.columnNameToColumnPos.get(columnName);
		if (colPos == null) {
			throw new IllegalArgumentException("Column name not found: " + columnName);
		}
		this.columnNames.remove((int) colPos);
		for (var row : this.values) {
			row.remove((int) colPos);
		}
		this.rebuildIndices();
	}

	// --- utility ---

	/**
	 * Converts the DataFrame to a nested map of row indices to column-value maps.
	 *
	 * @return map of row indices to maps of column names and values
	 */
	public Map<I, Map<String, Double>> toMap() {
		var map = new LinkedHashMap<I, Map<String, Double>>();
		for (int i = 0; i < this.index.size(); i++) {
			var rowMap = new LinkedHashMap<String, Double>();
			var rowValues = this.values.get(i);
			for (int j = 0; j < this.columnNames.size(); j++) {
				rowMap.put(this.columnNames.get(j), rowValues.get(j));
			}
			map.put(this.index.get(i), rowMap);
		}
		return map;
	}

	@Override
	public void dropNa() {
		var newIndex = new ArrayList<I>();
		var newValues = new ArrayList<List<Double>>();

		for (int i = 0; i < this.values.size(); i++) {
			var row = this.values.get(i);
			boolean hasNaN = row.stream().anyMatch(v -> v == null || v.isNaN());
			if (!hasNaN) {
				newIndex.add(this.index.get(i));
				newValues.add(new ArrayList<>(row));
			}
		}

		this.update(newIndex, newValues);
	}

	@Override
	public void sortByIndex(Comparator<I> comparator) {
		var positions = IntStream.range(0, this.index.size())//
				.boxed()//
				.sorted(Comparator.comparing(this.index::get, comparator))//
				.toList();

		var newIndex = new ArrayList<I>(this.index.size());
		var newValues = new ArrayList<List<Double>>(this.values.size());

		for (int pos : positions) {
			newIndex.add(this.index.get(pos));
			newValues.add(new ArrayList<>(this.values.get(pos)));
		}

		this.update(newIndex, newValues);
	}

	@Override
	public OptionalDouble min() {
		return this.values.stream()//
				.flatMap(List::stream)//
				.mapToDouble(Double::doubleValue)//
				.min();
	}

	@Override
	public OptionalDouble max() {
		return this.values.stream()//
				.flatMap(List::stream)//
				.mapToDouble(Double::doubleValue)//
				.max();
	}

	@Override
	public void apply(Function<Double, Double> function) {
		var newValues = new ArrayList<List<Double>>();

		for (int rowIndex = 0; rowIndex < this.rowCount(); rowIndex++) {
			var oldRow = this.values.get(rowIndex);
			var newRow = new ArrayList<Double>();
			for (int colIndex = 0; colIndex < this.columnCount(); colIndex++) {
				Double oldVal = oldRow.get(colIndex);
				Double newVal = Optional.ofNullable(oldVal)//
						.map(function)//
						.orElse(null);
				newRow.add(newVal);
			}
			newValues.add(newRow);
		}

		this.update(List.copyOf(this.index), newValues);
	}

	@Override
	public DataFrame<I> copy() {
		return new DataFrame<>(this.index, this.columnNames, this.values);
	}

	/**
	 * Returns a new {@link DataFrame} containing the last {@code n} rows of this
	 * {@link DataFrame}.
	 *
	 * @param n the number of rows to include from the end of the {@link DataFrame};
	 *          must be non-negative
	 * @return a new {@link DataFrame} containing the last {@code n} rows
	 * @throws IllegalArgumentException if {@code n} is negative
	 */
	public DataFrame<I> tail(int n) {
		if (n < 0) {
			throw new IllegalArgumentException("n must be non-negative");
		}
		int size = this.index.size();
		if (n >= size) {
			return this.copy();
		}
		var newIndex = this.index.subList(size - n, size);
		var newValues = this.values.subList(size - n, size);
		return new DataFrame<I>(newIndex, this.columnNames, newValues);
	}

	// --- private helpers --- //

	private void checkForDuplicates(List<?> list, String name) {
		var seen = new HashSet<Object>();
		for (var item : list) {
			if (!seen.add(item)) {
				throw new IllegalArgumentException("Duplicate " + name + " found: " + item);
			}
		}
	}

	private void update(List<I> newIndex, List<List<Double>> newValues) {
		this.index.clear();
		this.index.addAll(newIndex);

		this.values.clear();
		this.values.addAll(newValues);

		this.rebuildIndices();
	}

	private void rebuildIndices() {
		this.indexToRowPos.clear();
		for (int i = 0; i < this.index.size(); i++) {
			this.indexToRowPos.put(this.index.get(i), i);
		}

		this.columnNameToColumnPos.clear();
		for (int i = 0; i < this.columnNames.size(); i++) {
			this.columnNameToColumnPos.put(this.columnNames.get(i), i);
		}
	}

	// --- equals and hashCode ---

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof DataFrame<?> other)) {
			return false;
		}
		return Objects.equals(this.index, other.index)//
				&& Objects.equals(this.columnNames, other.columnNames)//
				&& Objects.equals(this.values, other.values);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.index, this.columnNames, this.values);
	}
}
