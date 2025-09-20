package io.openems.common.utils;

import static io.openems.common.utils.CollectorUtils.toNavigableMap;
import static io.openems.common.utils.CollectorUtils.toSortedMap;
import static org.junit.Assert.assertEquals;

import java.util.NavigableMap;
import java.util.SortedMap;
import java.util.stream.Stream;

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

	@Test
	public void testToSortedMap() {
		final SortedMap<String, String> sortedMap = Stream.of("abc-def", "hij-klm") //
				.map(t -> t.split("-")) //
				.collect(toSortedMap(t -> t[0], t -> t[1], (t, u) -> t));

		assertEquals("abc", sortedMap.firstKey());
		assertEquals("def", sortedMap.firstEntry().getValue());
		assertEquals("hij", sortedMap.lastKey());
		assertEquals("klm", sortedMap.lastEntry().getValue());
	}

	@Test
	public void testToNavigableMap() {
		final NavigableMap<String, String> navigableMap = Stream.of("abc-def", "hij-klm") //
				.map(t -> t.split("-")) //
				.collect(toNavigableMap(t -> t[0], t -> t[1], (t, u) -> t));

		assertEquals("abc", navigableMap.firstKey());
		assertEquals("def", navigableMap.firstEntry().getValue());
		assertEquals("hij", navigableMap.lastKey());
		assertEquals("klm", navigableMap.lastEntry().getValue());
	}

}
