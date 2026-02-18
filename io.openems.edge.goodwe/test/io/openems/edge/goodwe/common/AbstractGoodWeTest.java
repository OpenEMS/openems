package io.openems.edge.goodwe.common;

import static io.openems.edge.goodwe.common.AbstractGoodWe.mapGridMode;
import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.stream.Stream;

import org.junit.Test;

import io.openems.edge.common.sum.GridMode;
import io.openems.edge.goodwe.common.enums.GoodWeType;

public class AbstractGoodWeTest {

	@Test
	public void testMapGridModeNull() {
		assertEquals(GridMode.UNDEFINED, mapGridMode(null, 0));
		assertEquals(GridMode.ON_GRID, mapGridMode(null, 1));
		assertEquals(GridMode.OFF_GRID, mapGridMode(null, 2));
		assertEquals(GridMode.UNDEFINED, mapGridMode(null, null));
	}

	@Test
	public void testMapGridModeDefault() {
		for (var goodWeType : Stream.of(GoodWeType.values()) //
				.filter(t -> t != GoodWeType.FENECON_50K) //
				.toList()) {
			assertEquals(GridMode.UNDEFINED, mapGridMode(goodWeType, 0));
			assertEquals(GridMode.ON_GRID, mapGridMode(goodWeType, 1));
			assertEquals(GridMode.OFF_GRID, mapGridMode(goodWeType, 2));
			assertEquals(GridMode.UNDEFINED, mapGridMode(goodWeType, null));
		}
	}

	@Test
	public void testMapGridModeFenecon50k() {
		for (var goodWeType : List.of(GoodWeType.FENECON_50K)) {
			assertEquals(GridMode.OFF_GRID, mapGridMode(goodWeType, 0));
			assertEquals(GridMode.ON_GRID, mapGridMode(goodWeType, 1));
			assertEquals(GridMode.UNDEFINED, mapGridMode(goodWeType, 2));
			assertEquals(GridMode.UNDEFINED, mapGridMode(goodWeType, null));
		}
	}

}