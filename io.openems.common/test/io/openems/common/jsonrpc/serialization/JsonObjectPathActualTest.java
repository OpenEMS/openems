package io.openems.common.jsonrpc.serialization;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

import io.openems.common.utils.JsonUtils;

public class JsonObjectPathActualTest {

	@Test
	public void testJsonObjectPathActualNonNull() throws Exception {
		assertThrows(RuntimeException.class, () -> {
			new JsonObjectPathActual.JsonObjectPathActualNonNull(null);
		});
		final var jsonPath = new JsonObjectPathActual.JsonObjectPathActualNonNull(JsonUtils.buildJsonObject() //
				.addProperty("value", false) //
				.build());

		assertNotNull(jsonPath.getJsonElementPath("value"));
		assertThrows(RuntimeException.class, () -> {
			jsonPath.getJsonElementPath("notExistingValue");
		});
		assertNotNull(jsonPath.getNullableJsonElementPath("value"));
		assertNotNull(jsonPath.getNullableJsonElementPath("notExistingValue"));
	}

	@Test
	public void testJsonObjectPathActualNullableNonNull() throws Exception {
		final var jsonPath = new JsonObjectPathActual.JsonObjectPathActualNullable(JsonUtils.buildJsonObject() //
				.addProperty("value", false) //
				.build());

		assertNotNull(jsonPath.getOrNull());
	}

	@Test
	public void testJsonObjectPathActualNullableNull() throws Exception {
		final var jsonPath = new JsonObjectPathActual.JsonObjectPathActualNullable(null);

		assertNull(jsonPath.getOrNull());
	}

}
