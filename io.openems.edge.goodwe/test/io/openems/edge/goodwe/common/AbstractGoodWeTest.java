package io.openems.edge.goodwe.common;

import static io.openems.edge.goodwe.common.AbstractGoodWe.mapGridMode;
import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.Test;

import io.openems.edge.common.sum.GridMode;
import io.openems.edge.goodwe.common.enums.GoodWeType;

public class AbstractGoodWeTest {

	@Test
	public void testMapGridModeNull() {
		assertEquals(GridMode.UNDEFINED, mapGridMode(null, 0, false));
		assertEquals(GridMode.ON_GRID, mapGridMode(null, 1, false));
		assertEquals(GridMode.OFF_GRID, mapGridMode(null, 2, false));
		assertEquals(GridMode.UNDEFINED, mapGridMode(null, null, false));
	}

	@Test
	public void testMapGridModeDefault() {
		for (var goodWeType : Stream.of(GoodWeType.values()) //
				.filter(t -> !Set.of(GoodWeType.FENECON_50K, GoodWeType.FENECON_100K).contains(t)) //
				.toList()) {
			assertEquals(GridMode.UNDEFINED, mapGridMode(goodWeType, 0, false));
			assertEquals(GridMode.ON_GRID, mapGridMode(goodWeType, 1, false));
			assertEquals(GridMode.OFF_GRID, mapGridMode(goodWeType, 2, false));
			assertEquals(GridMode.UNDEFINED, mapGridMode(goodWeType, null, false));
		}
	}

	@Test
	public void testMapGridModeFenecon50k() {
		for (var goodWeType : List.of(GoodWeType.FENECON_50K, GoodWeType.FENECON_100K)) {
			assertEquals(GridMode.OFF_GRID, mapGridMode(goodWeType, 0, false));
			assertEquals(GridMode.ON_GRID, mapGridMode(goodWeType, 1, false));
			assertEquals(GridMode.UNDEFINED, mapGridMode(goodWeType, 2, false));
			assertEquals(GridMode.UNDEFINED, mapGridMode(goodWeType, null, false));
		}
	}

	@Test
	public void testMapGridModeFenecon100kGenset() {
		for (var goodWeType : List.of(GoodWeType.FENECON_50K, GoodWeType.FENECON_100K)) {
			assertEquals(GridMode.OFF_GRID_GENSET, mapGridMode(goodWeType, 0, true));
			assertEquals(GridMode.OFF_GRID_GENSET, mapGridMode(goodWeType, 1, true));
			assertEquals(GridMode.OFF_GRID_GENSET, mapGridMode(goodWeType, 2, true));
			assertEquals(GridMode.UNDEFINED, mapGridMode(goodWeType, null, true));
		}
	}

}