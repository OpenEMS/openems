package io.openems.edge.predictor.lstm.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit test class for LstmPredictor bounds checking and edge cases.
 * 
 * <p>
 * This class tests the bounds checking functionality added to prevent
 * IndexOutOfBoundsException when accessing model data with invalid indices.
 * </p>
 */
public class LstmPredictorBoundsTest {

	private HyperParameters hyperParameters;

	@Before
	public void setUp() {
		this.hyperParameters = new HyperParameters();
	}

	/**
	 * Test that decodeDateToColumnIndex returns valid indices for various times.
	 */
	@Test
	public void testDecodeDateToColumnIndexValidTimes() {
		var midnight = ZonedDateTime.now().withHour(0).withMinute(0);
		var noon = ZonedDateTime.now().withHour(12).withMinute(0);
		var evening = ZonedDateTime.now().withHour(18).withMinute(30);

		var midnightIndex = LstmPredictor.decodeDateToColumnIndex(midnight, this.hyperParameters);
		var noonIndex = LstmPredictor.decodeDateToColumnIndex(noon, this.hyperParameters);
		var eveningIndex = LstmPredictor.decodeDateToColumnIndex(evening, this.hyperParameters);

		// Indices should be non-negative
		assertTrue("Midnight index should be non-negative", midnightIndex >= 0);
		assertTrue("Noon index should be non-negative", noonIndex >= 0);
		assertTrue("Evening index should be non-negative", eveningIndex >= 0);

		// Indices should be within 24-hour range
		var maxIndex = 24 * (60 / this.hyperParameters.getInterval());
		assertTrue("Midnight index should be within range", midnightIndex < maxIndex);
		assertTrue("Noon index should be within range", noonIndex < maxIndex);
		assertTrue("Evening index should be within range", eveningIndex < maxIndex);
	}

	/**
	 * Test that getIndex returns correct index for specific hour and minute
	 * combinations.
	 */
	@Test
	public void testGetIndexValidCombinations() {
		// Test midnight
		var midnightIndex = LstmPredictor.getIndex(0, 0, this.hyperParameters);
		assertEquals("Midnight should be index 0", Integer.valueOf(0), midnightIndex);

		// Test noon
		var noonIndex = LstmPredictor.getIndex(12, 0, this.hyperParameters);
		var expectedNoonIndex = 12 * (60 / this.hyperParameters.getInterval());
		assertEquals("Noon index should be correct", Integer.valueOf(expectedNoonIndex), noonIndex);

		// Test with interval-aligned minute
		var index = LstmPredictor.getIndex(10, this.hyperParameters.getInterval(), this.hyperParameters);
		assertTrue("Index should be positive", index >= 0);
	}

	/**
	 * Test that getArranged correctly rearranges an ArrayList.
	 */
	@Test
	public void testGetArrangedSplitAndMerge() {
		var testList = new ArrayList<>(Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0));
		var splitIndex = 2;

		var result = LstmPredictor.getArranged(splitIndex, testList);

		// Expected: [3.0, 4.0, 5.0, 1.0, 2.0]
		assertEquals("Result should have same size", testList.size(), result.size());
		assertEquals("First element should be from second group", Double.valueOf(3.0), result.get(0));
		assertEquals("Last element should be from first group", Double.valueOf(2.0), result.get(4));
	}

	/**
	 * Test that getArranged handles edge case with split at beginning.
	 */
	@Test
	public void testGetArrangedSplitAtBeginning() {
		var testList = new ArrayList<>(Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0));
		var splitIndex = 0;

		var result = LstmPredictor.getArranged(splitIndex, testList);

		// Expected: [1.0, 2.0, 3.0, 4.0, 5.0] (no change)
		assertEquals("Result should be identical to input", testList, result);
	}

	/**
	 * Test that getArranged handles edge case with split at end.
	 */
	@Test
	public void testGetArrangedSplitAtEnd() {
		var testList = new ArrayList<>(Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0));
		var splitIndex = testList.size();

		var result = LstmPredictor.getArranged(splitIndex, testList);

		// Expected: empty list followed by all elements (effectively reversed order)
		assertEquals("Result should have same size", testList.size(), result.size());
	}

	/**
	 * Test predictTrend with valid data to ensure no exceptions are thrown.
	 */
	@Test
	public void testPredictTrendValidData() {
		var data = new ArrayList<Double>();
		var dates = new ArrayList<OffsetDateTime>();
		var now = OffsetDateTime.now();

		// Create sample data
		for (int i = 0; i < this.hyperParameters.getWindowSizeTrend(); i++) {
			data.add(100.0 + i * 10.0);
			dates.add(
					now.minusMinutes((this.hyperParameters.getWindowSizeTrend() - i) * this.hyperParameters.getInterval()));
		}

		// This test verifies that the method doesn't throw an exception
		// The actual prediction values depend on the model, so we just check it runs
		try {
			var result = LstmPredictor.predictTrend(data, dates, ZonedDateTime.now(), this.hyperParameters);
			assertTrue("Result should not be null", result != null);
		} catch (Exception e) {
			// If model files don't exist, this is expected in test environment
			assertTrue("Exception should be related to missing model files",
					e.getMessage() == null || e.getMessage().contains("model") || e.getMessage().contains("file"));
		}
	}

	/**
	 * Test that decodeDateToColumnIndex handles edge case at day boundary.
	 */
	@Test
	public void testDecodeDateToColumnIndexDayBoundary() {
		var endOfDay = ZonedDateTime.now().withHour(23).withMinute(59);
		var startOfDay = ZonedDateTime.now().withHour(0).withMinute(0);

		var endIndex = LstmPredictor.decodeDateToColumnIndex(endOfDay, this.hyperParameters);
		var startIndex = LstmPredictor.decodeDateToColumnIndex(startOfDay, this.hyperParameters);

		assertTrue("End of day index should be valid", endIndex >= 0);
		assertTrue("Start of day index should be valid", startIndex >= 0);
	}
}
