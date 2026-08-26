package io.openems.edge.controller.api.mqtt;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MqttFilterTest {

	@Test
	public void checkFilter() {
		assertFalse(MqttTopicFilter.validFilter("foo#"));
		assertFalse(MqttTopicFilter.validFilter("foo/#/bar"));
		assertFalse(MqttTopicFilter.validFilter("foo/+/#/bar"));
		assertFalse(MqttTopicFilter.validFilter("foo/bar#"));
		assertFalse(MqttTopicFilter.validFilter("foo/#bar"));
		assertFalse(MqttTopicFilter.validFilter(null));
		assertFalse(MqttTopicFilter.validFilter(""));
		assertFalse(MqttTopicFilter.validFilter("foo\\bar"));
		assertFalse(MqttTopicFilter.validFilter("foo/bar\\test"));
		assertTrue(MqttTopicFilter.validFilter("foo/#"));
		assertTrue(MqttTopicFilter.validFilter("foo/+"));
		assertTrue(MqttTopicFilter.validFilter("foo/bar"));
		assertTrue(MqttTopicFilter.validFilter("foo/+/bar"));

		assertThrows(IllegalArgumentException.class, () -> MqttTopicFilter.of("foo#"));
		assertThrows(IllegalArgumentException.class, () -> MqttTopicFilter.of(null));
	}

	@Test
	public void simpleFilterTest() {
		final var filter = MqttTopicFilter.of("foo/bar");

		assertTrue(filter.matches("foo/bar/"));
		assertTrue(filter.matches("foo/bar"));
		assertFalse(filter.matches("foo"));
		assertFalse(filter.matches("foo/bar/test/"));
		assertFalse(filter.matches("bar/foo/"));
	}

	@Test
	public void multiLevelWildcardTest() {
		final var filter = MqttTopicFilter.of("foo/#");

		assertTrue(filter.matches("foo/bar/"));
		assertTrue(filter.matches("foo/bar"));
		assertTrue(filter.matches("foo/bar/test/"));
		assertFalse(filter.matches("bar/foo/"));
	}

	@Test
	public void singleLevelWildcardTest() {
		final var filter = MqttTopicFilter.of("foo/+");

		assertTrue(filter.matches("foo/bar/"));
		assertTrue(filter.matches("foo/bar"));
		assertFalse(filter.matches("foo/bar/test/"));
		assertFalse(filter.matches("bar/foo/"));

		final var filter2 = MqttTopicFilter.of("foo/+/bar");

		assertFalse(filter2.matches("foo/bar/"));
		assertTrue(filter2.matches("foo/edge0/bar"));
		assertTrue(filter2.matches("foo/edge1/bar"));
		assertFalse(filter2.matches("bar/foo/"));
	}

}
