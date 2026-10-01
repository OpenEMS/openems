package io.openems.common.utils;

import static io.openems.common.utils.MapUtils.mapKey;
import static io.openems.common.utils.MapUtils.mapValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

class MapUtilsTest {

	@Test
	void testMapKey() {
		final var result = mapKey(Map.of(//
				"key1", 1, //
				"key2", 2 //
		), s -> s + "0");

		final var expected = Map.of(//
				"key10", 1, //
				"key20", 2 //
		);

		assertEquals(expected, result);
	}

	@Test
	void testMapValue() {
		final var result = mapValue(Map.of(//
				"key1", 1, //
				"key2", 2 //
		), i -> i * 10);

		final var expected = Map.of(//
				"key1", 10, //
				"key2", 20 //
		);

		assertEquals(expected, result);
	}

}