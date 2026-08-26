package io.openems.common.jsonrpc.serialization;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.stringSerializer;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.openems.common.utils.JsonUtils;

public class JsonArrayPathActualTest {

	@Test
	public void testPredefinedCollectors() throws Exception {
		final JsonArrayPath jsonPath = new JsonArrayPathActual.JsonArrayPathActualNonNull(JsonUtils.buildJsonArray() //
				.add("someValue") //
				.build());

		final var list = jsonPath.getAsList(stringSerializer());
		assertEquals(1, list.size());
		assertEquals("someValue", list.get(0));

		final var array = jsonPath.getAsArray(String[]::new, stringSerializer());
		assertEquals(1, array.length);
		assertEquals("someValue", array[0]);

		final var set = jsonPath.getAsSet(stringSerializer());
		assertEquals(1, set.size());
		assertTrue(set.contains("someValue"));
	}

}
