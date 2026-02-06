package io.openems.edge.predictor.api.mlcore.datastructures;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

import org.junit.Before;
import org.junit.Test;

public class DataFrameTest {

	private DataFrame<Integer> dataframe;
	private List<Integer> index;
	private List<String> columnNames;
	private List<List<Double>> values;

	@Before
	public void setUp() {
		this.index = List.of(0, 1, 2);
		this.columnNames = List.of("column1", "column2");
		this.values = List.of(//
				List.of(1.0, 2.0), //
				List.of(3.0, 4.0), //
				List.of(5.0, 6.0));
		this.dataframe = new DataFrame<>(this.index, this.columnNames, this.values);
	}

	@Test
	public void testConstructor_ShouldInitializeEmpty_WhenNoArguments() {
		var emptyDf = new DataFrame<Integer>();
		assertEquals(0, emptyDf.rowCount());
		assertEquals(0, emptyDf.columnCount());
		assertTrue(emptyDf.getIndex().isEmpty());
		assertTrue(emptyDf.getColumnNames().isEmpty());
		assertTrue(emptyDf.getValues().isEmpty());
	}

	@Test
	public void testConstructor_ShouldThrow_WhenIndexSizeMismatchesValues() {
		var invalidIndex = Arrays.asList(1, 2);
		assertThrows(//
				IllegalArgumentException.class, //
				() -> new DataFrame<>(invalidIndex, this.columnNames, this.values));
	}

	@Test
	public void testConstructor_ShouldThrow_WhenRowSizeMismatchesColumns() {
		var invalidValues = List.of(//
				List.of(1.0), //
				List.of(3.0, 4.0), //
				List.of(5.0, 6.0));
		assertThrows(//
				IllegalArgumentException.class, //
				() -> new DataFrame<>(this.index, this.columnNames, invalidValues));
	}

	@Test
	public void testConstructor_ShouldThrow_WhenDuplicateIndex() {
		var duplicateIndex = List.of(0, 0, 2);
		assertThrows(//
				IllegalArgumentException.class, //
				() -> new DataFrame<>(duplicateIndex, this.columnNames, this.values));
	}

	@Test
	public void testConstructor_ShouldThrow_WhenDuplicateColumnNames() {
		var duplicateColumns = Arrays.asList("column1", "column1");
		assertThrows(//
				IllegalArgumentException.class, //
				() -> new DataFrame<>(this.index, duplicateColumns, this.values));
	}

	// --- factory methods --- //

	@Test
	public void testFromSeriesMap_ShouldCreateDataFrame_WhenValidSeriesMap() {
		var seriesMap = new HashMap<String, Series<Integer>>();
		seriesMap.put("column1", this.dataframe.getColumn("column1"));
		seriesMap.put("column2", this.dataframe.getColumn("column2"));

		var result = DataFrame.fromSeriesMap(seriesMap);
		assertEquals(this.index, result.getIndex());
		assertEquals(this.columnNames, result.getColumnNames());
		assertEquals(this.values, result.getValues());
	}

	@Test
	public void testFromSeriesMap_ShouldThrow_WhenSeriesHaveDifferentIndices() {
		var seriesMap = new HashMap<String, Series<Integer>>();
		seriesMap.put("column1", new Series<>(List.of(0, 1, 3), List.of(1.0, 3.0, 5.0)));
		seriesMap.put("column2", this.dataframe.getColumn("column2"));

		assertThrows(//
				IllegalArgumentException.class, //
				() -> DataFrame.fromSeriesMap(seriesMap));
	}

	// --- accessors / getters --- //

	@Test
	public void testGetIndex_ShouldReturnUnmodifiableList() {
		assertEquals(this.index, this.dataframe.getIndex());
		assertThrows(//
				UnsupportedOperationException.class, //
				() -> this.dataframe.getIndex().add(4));
	}

	@Test
	public void testGetColumnNames_ShouldReturnCopy() {
		var result = this.dataframe.getColumnNames();
		assertEquals(this.columnNames, result);
		result.add("column3");
		assertEquals(2, this.dataframe.getColumnNames().size());
	}

	@Test
	public void testGetValues_ShouldReturnUnmodifiableView() {
		var result = this.dataframe.getValues();
		assertEquals(this.values, result);
		assertThrows(//
				UnsupportedOperationException.class, //
				() -> result.add(new ArrayList<>()));
		assertThrows(//
				UnsupportedOperationException.class, //
				() -> result.get(0).add(7.0));
	}

	@Test
	public void testRowCount_ShouldReturnCorrectCount() {
		assertEquals(3, this.dataframe.rowCount());
	}

	@Test
	public void testColumnCount_ShouldReturnCorrectCount() {
		assertEquals(2, this.dataframe.columnCount());
	}

	@Test
	public void testGetColumn_ShouldReturnSeries_WhenColumnExists() {
		var expected = new Series<>(this.index, List.of(1.0, 3.0, 5.0));
		assertEquals(expected, this.dataframe.getColumn("column1"));
	}

	@Test
	public void testGetColumn_ShouldThrow_WhenColumnNotFound() {
		assertThrows(//
				IllegalArgumentException.class, //
				() -> this.dataframe.getColumn("column3"));
	}

	@Test
	public void testGetColumnAt_ShouldReturnSeries_WhenPositionValid() {
		var expected = new Series<>(this.index, List.of(2.0, 4.0, 6.0));
		assertEquals(expected, this.dataframe.getColumnAt(1));
	}

	@Test
	public void testGetColumnAt_ShouldThrow_WhenPositionInvalid() {
		assertThrows(//
				IndexOutOfBoundsException.class, //
				() -> this.dataframe.getColumnAt(2));
	}

	@Test
	public void testGetRow_ShouldReturnRow_WhenIndexExists() {
		assertEquals(//
				Arrays.asList(3.0, 4.0), //
				this.dataframe.getRow(1));
	}

	@Test
	public void testGetRow_ShouldThrow_WhenIndexNotFound() {
		assertThrows(//
				IllegalArgumentException.class, //
				() -> this.dataframe.getRow(3));
	}

	@Test
	public void testGetRow_ShouldReturnRow_WhenPositionValid() {
		var result = this.dataframe.getRowAt(1);
		assertEquals(List.of(3.0, 4.0), result);
	}

	@Test
	public void testGetRowAt_ShouldThrow_WhenPositionInvalid() {
		assertThrows(//
				IndexOutOfBoundsException.class, //
				() -> this.dataframe.getRowAt(3));
	}

	@Test
	public void testGetValue_ShouldReturnValue_WhenValid() {
		assertEquals(4.0, this.dataframe.getValue(1, "column2"), 0.0);
	}

	@Test
	public void testGetValue_ShouldThrow_WhenRowIndexNotFound() {
		assertThrows(//
				IllegalArgumentException.class, //
				() -> this.dataframe.getValue(3, "column2"));
	}

	@Test
	public void testGetValue_ShouldThrow_WhenColumnNameNotFound() {
		assertThrows(//
				IllegalArgumentException.class, //
				() -> this.dataframe.getValue(1, "column3"));
	}

	@Test
	public void testGetValueAt_ShouldReturnValue_WhenValid() {
		assertEquals(5.0, this.dataframe.getValueAt(2, 0), 0.0);
	}

	@Test
	public void testGetValueAt_ShouldThrow_WhenRowPositionInvalid() {
		assertThrows(//
				IndexOutOfBoundsException.class, //
				() -> this.dataframe.getValueAt(3, 0));
	}

	@Test
	public void testGetValueAt_ShouldThrow_WhenColumnPositionInvalid() {
		assertThrows(//
				IndexOutOfBoundsException.class, //
				() -> this.dataframe.getValueAt(0, 2));
	}

	// --- mutators / setters ---

	@Test
	public void testAddRow_ShouldAddRow_WhenValid() {
		this.dataframe.addRow(3, Arrays.asList(7.0, 8.0));
		assertEquals(4, this.dataframe.rowCount());
		assertEquals(Arrays.asList(7.0, 8.0), this.dataframe.getRow(3));
	}

	@Test
	public void testAddRow_ShouldThrow_WhenIndexExists() {
		assertThrows(//
				IllegalArgumentException.class, //
				() -> this.dataframe.addRow(0, Arrays.asList(7.0, 8.0)));
	}

	@Test
	public void testAddRow_ShouldThrow_WhenRowSizeMismatches() {
		assertThrows(//
				IllegalArgumentException.class, //
				() -> this.dataframe.addRow(3, Arrays.asList(7.0)));
	}

	@Test
	public void testAddEmptyRow_ShouldAddRowWithNaN_WhenValidIndex() {
		this.dataframe.addEmptyRow(3);
		assertEquals(4, this.dataframe.rowCount());
		assertEquals(Arrays.asList(Double.NaN, Double.NaN), this.dataframe.getRow(3));
	}

	@Test
	public void testAddEmptyRow_ShouldThrow_WhenIndexExists() {
		assertThrows(//
				IllegalArgumentException.class, //
				() -> this.dataframe.addEmptyRow(0));
	}

	@Test
	public void testSetColumn_ShouldAddNewColumn_WhenColumnNotExists() {
		var newSeries = new Series<>(this.index, List.of(7.0, 8.0, 9.0));
		this.dataframe.setColumn("column3", newSeries);
		assertEquals(3, this.dataframe.columnCount());
		assertEquals(newSeries, this.dataframe.getColumn("column3"));
	}

	@Test
	public void testSetColumn_ShouldUpdateColumn_WhenColumnExists() {
		var newSeries = new Series<>(this.index, List.of(7.0, 8.0, 9.0));
		this.dataframe.setColumn("column1", newSeries);
		assertEquals(newSeries, this.dataframe.getColumn("column1"));
	}

	@Test
	public void testSetColumn_ShouldThrow_WhenSeriesIndexMismatches() {
		var invalidSeries = new Series<>(List.of(0, 1, 3), List.of(7.0, 8.0, 9.0));
		assertThrows(//
				IllegalArgumentException.class, //
				() -> this.dataframe.setColumn("column3", invalidSeries));
	}

	@Test
	public void testSetValue_ShouldUpdateValue_WhenValid() {
		this.dataframe.setValue(1, "column2", 10.0);
		assertEquals(10.0, this.dataframe.getValue(1, "column2"), 0.0);
	}

	@Test
	public void testSetValue_ShouldThrow_WhenRowIndexNotFound() {
		assertThrows(//
				IllegalArgumentException.class, //
				() -> this.dataframe.setValue(3, "column2", 10.0));
	}

	@Test
	public void testSetValue_ShouldThrow_WhenColumnNameNotFound() {
		assertThrows(//
				IllegalArgumentException.class, //
				() -> this.dataframe.setValue(1, "column3", 10.0));
	}

	@Test
	public void testSetValueAt_ShouldUpdateValue_WhenValid() {
		this.dataframe.setValueAt(0, 0, 10.0);
		assertEquals(10.0, this.dataframe.getValueAt(0, 0), 0.0);
	}

	@Test
	public void testSetValueAt_ShouldThrow_WhenRowPositionInvalid() {
		assertThrows(//
				IndexOutOfBoundsException.class, //
				() -> this.dataframe.setValueAt(3, 0, 10.0));
	}

	@Test
	public void testSetValueAt_ShouldThrow_WhenColumnPositionInvalid() {
		assertThrows(//
				IndexOutOfBoundsException.class, //
				() -> this.dataframe.setValueAt(0, 2, 10.0));
	}

	@Test
	public void testRemoveRow_ShouldRemoveRow_WhenIndexExists() {
		this.dataframe.removeRow(2);
		assertEquals(2, this.dataframe.rowCount());
		assertFalse(this.dataframe.getIndex().contains(2));
	}

	@Test
	public void testRemoveRow_ShouldThrow_WhenIndexNotFound() {
		assertThrows(//
				IllegalArgumentException.class, //
				() -> this.dataframe.removeRow(3));
	}

	@Test
	public void testRemoveColumn_ShouldRemoveColumn_WhenNameExists() {
		this.dataframe.removeColumn("column1");
		assertEquals(1, this.dataframe.columnCount());
		assertFalse(this.dataframe.getColumnNames().contains("column1"));
	}

	@Test
	public void testRemoveColumn_ShouldThrow_WhenNameNotFound() {
		assertThrows(//
				IllegalArgumentException.class, //
				() -> this.dataframe.removeColumn("column3"));
	}

	// --- utility ---

	@Test
	public void testToMap_ShouldReturnCorrectMap_WhenCalled() {
		var expected = new HashMap<Integer, Map<String, Double>>();
		expected.put(0, new HashMap<>());
		expected.get(0).put("column1", 1.0);
		expected.get(0).put("column2", 2.0);
		expected.put(1, new HashMap<>());
		expected.get(1).put("column1", 3.0);
		expected.get(1).put("column2", 4.0);
		expected.put(2, new HashMap<>());
		expected.get(2).put("column1", 5.0);
		expected.get(2).put("column2", 6.0);
		assertEquals(expected, this.dataframe.toMap());
	}

	@Test
	public void testDropNa_ShouldRemoveRowsWithNaN_WhenPresent() {
		this.dataframe.addRow(3, List.of(7.0, Double.NaN));
		this.dataframe.dropNa();
		assertEquals(3, this.dataframe.rowCount());
		assertFalse(this.dataframe.getIndex().contains(4));
	}

	@Test
	public void testSortByIndex_ShouldSortRows() {
		var unsortedDf = new DataFrame<>(//
				List.of(2, 0, 1), //
				this.columnNames, //
				List.of(//
						List.of(5.0, 6.0), //
						List.of(1.0, 2.0), //
						List.of(3.0, 4.0)));
		unsortedDf.sortByIndex(Comparator.naturalOrder());
		assertEquals(this.index, unsortedDf.getIndex());
		assertEquals(this.values, unsortedDf.getValues());
	}

	@Test
	public void testMin_ShouldReturnMinimum() {
		assertEquals(OptionalDouble.of(1.0), this.dataframe.min());
	}

	@Test
	public void testMax_ShouldReturnMaximum() {
		assertEquals(OptionalDouble.of(6.0), this.dataframe.max());
	}

	@Test
	public void testApply_ShouldTransformValues_WhenFunctionProvided() {
		this.dataframe.apply(x -> x * 2);
		var expectedValues = List.of(//
				List.of(2.0, 4.0), //
				List.of(6.0, 8.0), //
				List.of(10.0, 12.0));
		assertEquals(expectedValues, this.dataframe.getValues());
		assertEquals(this.index, this.dataframe.getIndex());
		assertEquals(this.columnNames, this.dataframe.getColumnNames());
	}

	@Test
	public void testCopy_ShouldCreateDeepCopy_WhenCalled() {
		var copy = this.dataframe.copy();
		assertEquals(this.dataframe, copy);
		copy.setValue(0, "column1", 10.0);
		assertNotEquals(this.dataframe.getValue(0, "column1"), copy.getValue(0, "column1"));
	}

	@Test
	public void testInnerJoin_ShouldReturnOnlyCommonIndices() {
		var index2 = List.of(0, 2, 4);
		var columnNames2 = List.of("column3");
		var values2 = List.of(//
				List.of(7.0), //
				List.of(8.0), //
				List.of(9.0));
		var dataframe2 = new DataFrame<>(index2, columnNames2, values2);

		var newDataframe = this.dataframe.innerJoin(dataframe2);
		assertEquals(List.of(0, 2), newDataframe.getIndex());
		assertEquals(List.of("column1", "column2", "column3"), newDataframe.getColumnNames());
		assertEquals(List.of(//
				List.of(1.0, 2.0, 7.0), //
				List.of(5.0, 6.0, 8.0)), //
				newDataframe.getValues());
	}

	@Test
	public void testTail_ShouldReturnCorrectTail() {
		// Case 1
		var result1 = this.dataframe.tail(2);
		var expected1 = new DataFrame<>(//
				List.of(1, 2), //
				List.of("column1", "column2"), //
				List.of(//
						List.of(3.0, 4.0), //
						List.of(5.0, 6.0)));
		assertEquals(expected1, result1);

		// Case 2
		var result2 = this.dataframe.tail(4);
		assertEquals(this.dataframe, result2);
	}

	@Test
	public void testTail_SchouldThrowException_WhenNIsNegative() {
		var exception = assertThrows(IllegalArgumentException.class, () -> {
			this.dataframe.tail(-1);
		});
		assertEquals("n must be non-negative", exception.getMessage());
	}

	// --- equals and hashCode ---

	@Test
	public void testEquals_ShouldReturnTrue_WhenDataFramesIdentical() {
		var other = new DataFrame<>(this.index, this.columnNames, this.values);
		assertTrue(this.dataframe.equals(other));
	}

	@Test
	public void testEquals_ShouldReturnFalse_WhenDataFramesDifferent() {
		var other = new DataFrame<>(//
				this.index, //
				this.columnNames, //
				List.of(//
						List.of(1.0, 2.0), //
						List.of(3.0, 4.0), //
						List.of(5.0, 7.0)));
		assertFalse(this.dataframe.equals(other));
	}

	@Test
	public void testHashCode_ShouldBeEqual_WhenDataFramesIdentical() {
		var other = new DataFrame<>(this.index, this.columnNames, this.values);
		assertEquals(this.dataframe.hashCode(), other.hashCode());
	}
}
