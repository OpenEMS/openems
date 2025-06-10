package io.openems.common.jsonrpc.serialization;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.google.gson.JsonPrimitive;

import io.openems.common.utils.JsonUtils;

public class BooleanPathActualTest {

	@Test
	public void testFromJsonObjectPathActual() {
		final JsonObjectPath json = new JsonObjectPathActual.JsonObjectPathActualNonNull(JsonUtils.buildJsonObject() //
				.addProperty("value", false) //
				.addProperty("nonBooleanValue", 99) //
				.build());

		assertFalse(json.getBoolean("value"));
		assertFalse(json.getBooleanNullable("value"));

		assertThrows(RuntimeException.class, () -> {
			json.getBoolean("someOtherValue");
		});
		assertNull(json.getBooleanNullable("someOtherValue"));

		assertThrows(RuntimeException.class, () -> {
			json.getBoolean("nonBooleanValue");
		});
		assertThrows(RuntimeException.class, () -> {
			json.getBooleanNullable("nonBooleanValue");
		});
	}

	@Test
	public void testFromJsonElementPathActualSuccess() {
		final JsonElementPath path = new JsonElementPathActual.JsonElementPathActualNonNull(new JsonPrimitive(true));

		assertTrue(path.getAsBoolean());
		assertTrue(path.getAsBooleanPath().get());
	}

	@Test
	public void testFromJsonElementPathActualFail() {
		final JsonElementPath path = new JsonElementPathActual.JsonElementPathActualNonNull(
				new JsonPrimitive("string"));

		assertThrows(RuntimeException.class, () -> {
			path.getAsBoolean();
		});
		assertThrows(RuntimeException.class, () -> {
			path.getAsBooleanPath();
		});
	}

	@Test
	public void testNonNull() {
		final BooleanPath booleanPath = new BooleanPathActual.BooleanPathActualNonNull(false);

		assertFalse(booleanPath.get());
	}

	@Test
	public void testNullableNonNull() {
		final BooleanPathNullable booleanPath = new BooleanPathActual.BooleanPathActualNullable(false);

		assertFalse(booleanPath.getOrNull());
		assertFalse(booleanPath.getOrDefault(true));
		assertFalse(booleanPath.getOptional().get());
	}

	@Test
	public void testNullableNull() {
		final BooleanPathNullable booleanPath = new BooleanPathActual.BooleanPathActualNullable(null);

		assertNull(booleanPath.getOrNull());
		assertTrue(booleanPath.getOrDefault(true));
		assertTrue(booleanPath.getOptional().isEmpty());
	}

}
