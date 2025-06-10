package io.openems.common.jsonrpc.serialization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.function.Function;

import org.junit.Test;

import com.google.gson.JsonObject;

import io.openems.common.utils.JsonUtils;

public class StringPathActualTest {

	@Test
	public void testFromJsonObjectPathActual() {
		final JsonObjectPath json = new JsonObjectPathActual.JsonObjectPathActualNonNull(JsonUtils.buildJsonObject() //
				.addProperty("value", "someString") //
				.addProperty("nonStringValuePrimitve", 99) //
				.add("nonStringValueObject", new JsonObject()) //
				.build());

		assertNotNull(json.getStringPath("value"));
		assertThrows(RuntimeException.class, () -> {
			json.getStringPath("nonStringValueObject");
		});

		assertEquals("someString", json.getString("value"));
		assertEquals("someString", json.getStringOrNull("value"));
		assertNull(json.getStringOrNull("null"));
		assertThrows(RuntimeException.class, () -> {
			json.getString("nonStringValueObject");
		});
	}

	@Test
	public void testStringPathActualNonNull() {
		assertThrows(RuntimeException.class, () -> {
			new StringPathActual.StringPathActualNonNull<String>(null, Function.identity());
		});
		assertThrows(RuntimeException.class, () -> {
			new StringPathActual.StringPathActualNonNull<String>("", null);
		});

		final var stringPath = new StringPathActual.StringPathActualNonNull<String>("string", Function.identity());

		assertEquals("string", stringPath.get());
		assertEquals("string", stringPath.getRaw());
	}

	@Test
	public void testStringPathActualNullableNonNull() {
		assertThrows(RuntimeException.class, () -> {
			new StringPathActual.StringPathActualNullable<String>("", null);
		});

		final var stringPath = new StringPathActual.StringPathActualNullable<String>("string", Function.identity());

		assertEquals("string", stringPath.getOrNull());
		assertEquals("string", stringPath.getRawOrNull());
		assertEquals("string", stringPath.getOptional().get());
	}

	@Test
	public void testStringPathActualNullableNull() {
		assertThrows(RuntimeException.class, () -> {
			new StringPathActual.StringPathActualNullable<String>(null, null);
		});

		final var stringPath = new StringPathActual.StringPathActualNullable<String>(null, Function.identity());

		assertNull(stringPath.getOrNull());
		assertNull(stringPath.getRawOrNull());
		assertTrue(stringPath.getOptional().isEmpty());
	}

}
