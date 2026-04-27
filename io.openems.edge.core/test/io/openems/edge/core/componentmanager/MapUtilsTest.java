package io.openems.edge.core.componentmanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Test;

public class MapUtilsTest {

	@Test
	public void getString() {
		final var map = Map.<String, Object>of("key1", "value1", "key2", 123L);

		final var value1 = MapUtils.getAsOptionalString(map, "key1");
		assertTrue(value1.isPresent());
		assertEquals(value1.get(), "value1");

		final var value2 = MapUtils.getAsOptionalString(map, "key2");
		assertTrue(value2.isPresent());
		assertEquals(value2.get(), "123");

		final var value3 = MapUtils.getAsOptionalString(map, "key3");
		assertTrue(value3.isEmpty());

		final var none = MapUtils.getAsOptionalString(null, "key1");
		assertTrue(none.isEmpty());
	}

	@Test
	public void getBoolean() {
		final var map = Map.<String, Object>of("key1", true, "key2", "true");

		final var value1 = MapUtils.getAsOptionalBoolean(map, "key1");
		assertTrue(value1.isPresent());
		assertEquals(value1.get(), true);

		final var value2 = MapUtils.getAsOptionalBoolean(map, "key2");
		assertTrue(value2.isPresent());
		assertEquals(value2.get(), true);

		final var value3 = MapUtils.getAsOptionalBoolean(map, "key3");
		assertTrue(value3.isEmpty());

		final var none = MapUtils.getAsOptionalBoolean(null, "key1");
		assertTrue(none.isEmpty());
	}

	@Test
	public void getLong() {
		final var map = Map.<String, Object>of("key1", 123, "key2", 123L, "key3", "123");
		final var testValue = Long.valueOf(123L);

		final var value1 = MapUtils.getAsOptionalLong(map, "key1");
		assertTrue(value1.isPresent());
		assertEquals(value1.get(), testValue);

		final var value2 = MapUtils.getAsOptionalLong(map, "key2");
		assertTrue(value2.isPresent());
		assertEquals(value2.get(), testValue);

		final var value3 = MapUtils.getAsOptionalLong(map, "key3");
		assertTrue(value3.isPresent());
		assertEquals(value3.get(), testValue);

		final var value4 = MapUtils.getAsOptionalLong(map, "key4");
		assertTrue(value4.isEmpty());

		final var none = MapUtils.getAsOptionalLong(null, "key1");
		assertTrue(none.isEmpty());
	}

}
