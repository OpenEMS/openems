package io.openems.common.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table.Cell;

public class CollectorUtilsTest {

	@Test
	public void testToDoubleMap() {
		final var map = ImmutableTable.<String, Integer, Double>builder() //
				.put("row", 1, 0.5) //
				.build();

		final var collectedMap = map.cellSet().stream() //
				.collect(CollectorUtils.toDoubleMap(//
						Cell::getRowKey, //
						Cell::getColumnKey, //
						Cell::getValue) //
				);

		assertEquals(0.5, collectedMap.get("row").get(1), 0);
	}

}
