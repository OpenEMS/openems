package io.openems.edge.predictor.profileclusteringmodel.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class QueryWindowTest {

	@Test
	public void testQueryWindow_ShouldCreateValidInstance() {
		var queryWindow = new QueryWindow(2, 5);
		assertEquals(2, queryWindow.minWindowDays());
		assertEquals(5, queryWindow.maxWindowDays());
	}

	@Test
	public void testQueryWindow_ShouldCreateValidInstance_WhenSingleParameter() {
		var queryWindow = new QueryWindow(3);
		assertEquals(3, queryWindow.minWindowDays());
		assertEquals(3, queryWindow.maxWindowDays());
	}

	@Test
	public void testQueryWindow_ShouldThrowException_WhenInvalidMinValue() {
		assertThrows(IllegalArgumentException.class, () -> {
			new QueryWindow(0, 5);
		});
	}

	@Test
	public void testQueryWindow_ShouldThrowException_WhenInvalidMaxValue() {
		assertThrows(IllegalArgumentException.class, () -> {
			new QueryWindow(3, 0);
		});
	}

	@Test
	public void testQueryWindow_ShouldThrowException_WhenMinGreaterThanMax() {
		assertThrows(IllegalArgumentException.class, () -> {
			new QueryWindow(6, 2);
		});
	}

	@Test
	public void testMinQuarters_ShouldReturnCorrectValue() {
		var queryWindow = new QueryWindow(1, 2);
		assertEquals(1 * 24 * 4, queryWindow.minQuarters());
	}

	@Test
	public void testMaxQuarters_ShouldReturnCorrectValue() {
		var queryWindow = new QueryWindow(1, 2);
		assertEquals(2 * 24 * 4, queryWindow.maxQuarters());
	}
}
