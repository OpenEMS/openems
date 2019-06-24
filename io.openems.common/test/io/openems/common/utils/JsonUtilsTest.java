package io.openems.common.utils;

import static org.junit.Assert.assertEquals;

import java.util.Optional;

import org.junit.Test;

import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

public class JsonUtilsTest {

	@Test
	public void testGetAsJsonElement() {
		assertEquals(JsonUtils.getAsJsonElement(null), JsonNull.INSTANCE);
		assertEquals(JsonUtils.getAsJsonElement(Optional.ofNullable(null)), JsonNull.INSTANCE);

		assertEquals(JsonUtils.getAsJsonElement("asdf"), new JsonPrimitive("asdf"));
		assertEquals(JsonUtils.getAsJsonElement(Optional.of("asdf")), new JsonPrimitive("asdf"));

		assertEquals(JsonUtils.getAsJsonElement(1234), new JsonPrimitive(1234));

		assertEquals(JsonUtils.getAsJsonElement(new Integer[] { 1, 2 }),
				JsonUtils.buildJsonArray().add(1).add(2).build());
		assertEquals(JsonUtils.getAsJsonElement(new Long[] { 1l, 2l }),
				JsonUtils.buildJsonArray().add(1).add(2).build());

		assertEquals(JsonUtils.getAsJsonElement(new boolean[] { true, false }),
				JsonUtils.buildJsonArray().add(true).add(false).build());
		assertEquals(JsonUtils.getAsJsonElement(new short[] { 1, 2 }),
				JsonUtils.buildJsonArray().add(1).add(2).build());
		assertEquals(JsonUtils.getAsJsonElement(new int[] { 1, 2 }), JsonUtils.buildJsonArray().add(1).add(2).build());
		assertEquals(JsonUtils.getAsJsonElement(new long[] { 1, 2 }), JsonUtils.buildJsonArray().add(1).add(2).build());
		assertEquals(JsonUtils.getAsJsonElement(new float[] { 1, 2 }),
				JsonUtils.buildJsonArray().add(1).add(2).build());
		assertEquals(JsonUtils.getAsJsonElement(new double[] { 1, 2 }),
				JsonUtils.buildJsonArray().add(1).add(2).build());

	}

}
