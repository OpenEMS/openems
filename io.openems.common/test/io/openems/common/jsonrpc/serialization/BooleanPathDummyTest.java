package io.openems.common.jsonrpc.serialization;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BooleanPathDummyTest {

	@Test
	public void testFromJsonObjectPathDummy() {
		final JsonObjectPath json = new JsonObjectPathDummy.JsonObjectPathDummyNonNull();

		assertNotNull(json.getBoolean("value"));
		assertNull(json.getBooleanNullable("otherValue"));
	}

	@Test
	public void testFromJsonElementPathActualSuccess() {
		final JsonElementPath path = new JsonElementPathDummy.JsonElementPathDummyNonNull();

		assertNotNull(path.getAsBoolean());
		assertNotNull(path.getAsBooleanPath());
	}

	@Test
	public void testNonNull() {
		final var booleanPath = new BooleanPathDummy.BooleanPathDummyNonNull();

		assertFalse(booleanPath.get());
		assertNotNull(booleanPath.buildPath());
	}

	@Test
	public void testNullable() {
		final var booleanPath = new BooleanPathDummy.BooleanPathDummyNullable();

		assertNull(booleanPath.getOrNull());
		assertTrue(booleanPath.getOrDefault(true));
		assertTrue(booleanPath.getOptional().isEmpty());
		assertNotNull(booleanPath.buildPath());
	}

}
