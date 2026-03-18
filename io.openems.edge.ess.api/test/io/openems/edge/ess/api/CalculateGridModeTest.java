package io.openems.edge.ess.api;

import static io.openems.edge.ess.api.CalculateGridMode.aggregateGridModes;
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import io.openems.edge.common.sum.GridMode;

public class CalculateGridModeTest {

	@Test
	public void testAggregateGridModes() {
		// Empty
		assertEquals(GridMode.UNDEFINED, //
				aggregateGridModes(List.of()));

		// All ON_GRID
		assertEquals(GridMode.ON_GRID, //
				aggregateGridModes(List.of(//
						GridMode.ON_GRID, //
						GridMode.ON_GRID)));

		// All OFF_GRID
		assertEquals(GridMode.OFF_GRID, //
				aggregateGridModes(List.of(//
						GridMode.OFF_GRID)));

		// At least one OFF_GRID_GENSET
		assertEquals(GridMode.OFF_GRID_GENSET, //
				aggregateGridModes(List.of(//
						GridMode.OFF_GRID_GENSET, //
						GridMode.OFF_GRID, //
						GridMode.UNDEFINED)));

		// UNDEFINED
		assertEquals(GridMode.UNDEFINED, //
				aggregateGridModes(List.of(//
						GridMode.ON_GRID, //
						GridMode.OFF_GRID)));
	}

}
