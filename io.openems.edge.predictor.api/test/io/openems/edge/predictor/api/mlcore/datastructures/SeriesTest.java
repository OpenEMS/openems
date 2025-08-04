package io.openems.edge.predictor.api.mlcore.datastructures;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.OptionalDouble;

import org.junit.Before;
import org.junit.Test;

public class SeriesTest {

	private Series<Integer> series;
	private List<Integer> index;
	private List<Double> values;

	@Before
	public void setUp() {
		this.index = Arrays.asList(0, 1, 2);
		this.values = Arrays.asList(1.0, 2.0, 3.0);
		this.series = new Series<>(this.index, this.values);
	}

	@Test
	public void testConstructor_ShouldInitializeSeries_WhenValidInput() {
		assertEquals(this.index, this.series.getIndex());
		assertEquals(this.values, this.series.getValues());
		assertEquals(3, this.series.size());
	}

	@Test
	public void testConstructor_ShouldThrow_WhenIndexAndValuesSizeMismatch() {
		var invalidValues = Arrays.asList(1.0, 2.0);
		assertThrows(//
				IllegalArgumentException.class, //
				() -> new Series<>(this.index, invalidValues));
	}

	@Test
	public void testConstructor_ShouldThrow_WhenDuplicateIndex() {
		var duplicateIndex = Arrays.asList(0, 1, 1);
		assertThrows(//
				IllegalArgumentException.class, //
				() -> new Series<>(duplicateIndex, this.values));
	}

	// --- accessors / getters --- //

	@Test
	public void testGetIndex_ShouldReturnUnmodifiableList() {
		var result = this.series.getIndex();
		assertEquals(this.index, result);
		assertThrows(//
				UnsupportedOperationException.class, //
				() -> result.add(4));
	}

	@Test
	public void testGetValues_ShouldReturnUnmodifiableList() {
		var result = this.series.getValues();
		assertEquals(this.values, result);
		assertThrows(//
				UnsupportedOperationException.class, //
				() -> result.add(4.0));
	}

	@Test
	public void testSize_ShouldReturnCorrectSize() {
		assertEquals(3, this.series.size());
	}

	@Test
	public void testGet_ShouldReturnValue_WhenIndexExists() {
		assertEquals(3.0, this.series.get(2), 0.0);
	}

	@Test
	public void testGet_ShouldThrow_WhenIndexNotFound() {
		assertThrows(//
				IllegalArgumentException.class, //
				() -> this.series.get(4));
	}

	@Test
	public void testGetAt_ShouldReturnValue_WhenPositionValid() {
		assertEquals(2.0, this.series.getAt(1), 0.0);
	}

	@Test
	public void testGetAt_ShouldThrow_WhenPositionInvalid() {
		assertThrows(//
				IndexOutOfBoundsException.class, //
				() -> this.series.getAt(3));
	}

	// --- mutators / setters ---

	@Test
	public void testSetValue_ShouldUpdateValue_WhenIndexExists() {
		this.series.setValue(2, 10.0);
		assertEquals(10.0, this.series.get(2), 0.0);
	}

	@Test
	public void testSetValue_ShouldThrow_WhenIndexNotFound() {
		assertThrows(//
				IndexOutOfBoundsException.class, //
				() -> this.series.setValueAt(4, 10.0));
	}

	@Test
	public void testSetValueAt_ShouldUpdateValue_WhenPositionValid() {
		this.series.setValueAt(1, 10.0);
		assertEquals(10.0, this.series.getAt(1), 0.0);
	}

	@Test
	public void testSetValueAt_ShouldThrow_WhenPositionInvalid() {
		assertThrows(//
				IndexOutOfBoundsException.class, //
				() -> this.series.setValueAt(3, 10.0));
	}

	@Test
	public void testRemove_ShouldRemoveElement_WhenIndexExists() {
		this.series.remove(2);
		assertEquals(2, this.series.size());
		assertFalse(this.series.getIndex().contains(2));
	}

	@Test
	public void testRemove_ShouldThrow_WhenIndexNotFound() {
		assertThrows(//
				IllegalArgumentException.class, //
				() -> this.series.remove(4));
	}

	@Test
	public void testRemoveAt_ShouldRemoveElement_WhenPositionValid() {
		this.series.removeAt(1);
		assertEquals(2, this.series.size());
		assertFalse(this.series.getIndex().contains(1));
	}

	@Test
	public void testRemoveAt_ShouldThrowException_WhenPositionInvalid() {
		assertThrows(//
				IndexOutOfBoundsException.class, //
				() -> this.series.removeAt(3));
	}

	@Test
	public void testAdd_ShouldAddElement_WhenIndexNotExists() {
		this.series.add(4, 4.0);
		assertEquals(4, this.series.size());
		assertEquals(4.0, this.series.get(4), 0.0);
	}

	@Test
	public void testAdd_ShouldThrow_WhenIndexAlreadyExists() {
		assertThrows(//
				IllegalArgumentException.class, //
				() -> this.series.add(1, 4.0));
	}

	// --- utility ---

	@Test
	public void testToMap_ShouldReturnCorrectMap() {
		var expected = new HashMap<Integer, Double>();
		expected.put(0, 1.0);
		expected.put(1, 2.0);
		expected.put(2, 3.0);
		assertEquals(expected, this.series.toMap());
	}

	@Test
	public void testDropNa_ShouldRemoveNaNValues() {
		var seriesWithoutNaN = new Series<>(//
				List.of(0, 1, 2, 3), //
				Arrays.asList(1.0, Double.NaN, 3.0, null));
		seriesWithoutNaN.dropNa();
		assertEquals(2, seriesWithoutNaN.size());
		assertEquals(List.of(0, 2), seriesWithoutNaN.getIndex());
		assertEquals(List.of(1.0, 3.0), seriesWithoutNaN.getValues());
	}

	@Test
	public void testSortByIndex_ShouldSortSeries_WhenComparatorProvided() {
		var unsortedSeries = new Series<>(//
				List.of(2, 0, 1), //
				List.of(3.0, 1.0, 2.0));
		unsortedSeries.sortByIndex(Comparator.naturalOrder());
		assertEquals(this.index, unsortedSeries.getIndex());
		assertEquals(this.values, unsortedSeries.getValues());
	}

	@Test
	public void testMin_ShouldReturnMinimum_WhenValuesPresent() {
		assertEquals(OptionalDouble.of(1.0), this.series.min());
	}

	@Test
	public void testMax_ShouldReturnMaximum_WhenValuesPresent() {
		assertEquals(OptionalDouble.of(3.0), this.series.max());
	}

	@Test
	public void testApply_ShouldTransformValues() {
		this.series.apply(x -> x * 2);
		var expectedValues = List.of(2.0, 4.0, 6.0);
		assertEquals(expectedValues, this.series.getValues());
	}

	@Test
	public void testApply_ShouldHandleNullValues() {
		var seriesWithNull = new Series<>(//
				this.index, //
				Arrays.asList(1.0, null, 3.0));
		seriesWithNull.apply(x -> x * 2);
		assertEquals(Arrays.asList(2.0, null, 6.0), seriesWithNull.getValues());
	}

	@Test
	public void testCopy_ShouldCreateDeepCopy_WhenCalled() {
		var copy = this.series.copy();
		assertEquals(this.series, copy);
		copy.setValueAt(1, 10.0);
		assertNotEquals(this.series.get(1), copy.get(1));
	}

	// --- equals and hashCode ---

	@Test
	public void testEquals_ShouldReturnTrue_WhenSeriesIdentical() {
		var other = new Series<>(//
				this.index, //
				this.values);
		assertTrue(this.series.equals(other));
	}

	@Test
	public void testEquals_ShouldReturnFalse_WhenSeriesDifferent() {
		var other = new Series<>(//
				this.index, //
				List.of(1.0, 2.0, 4.0));
		assertFalse(this.series.equals(other));
	}

	@Test
	public void testHashCode_ShouldBeEqual_WhenSeriesIdentical() {
		var other = new Series<>(//
				this.index, //
				this.values);
		assertEquals(this.series.hashCode(), other.hashCode());
	}
}
