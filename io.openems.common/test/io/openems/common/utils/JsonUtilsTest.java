package io.openems.common.utils;

import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;

import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;

public class JsonUtilsTest {

	@Test
	public void testGetAsJsonElement() {
		Assert.assertEquals(JsonUtils.getAsJsonElement(null), JsonNull.INSTANCE);
		Assert.assertEquals(JsonUtils.getAsJsonElement(Optional.ofNullable(null)), JsonNull.INSTANCE);

		Assert.assertEquals(JsonUtils.getAsJsonElement("asdf"), new JsonPrimitive("asdf"));
		Assert.assertEquals(JsonUtils.getAsJsonElement(Optional.of("asdf")), new JsonPrimitive("asdf"));

		Assert.assertEquals(JsonUtils.getAsJsonElement(1234), new JsonPrimitive(1234));

		Assert.assertEquals(JsonUtils.getAsJsonElement(new Integer[] { 1, 2 }),
				JsonUtils.buildJsonArray().add(1).add(2).build());
		Assert.assertEquals(JsonUtils.getAsJsonElement(new Long[] { 1L, 2L }),
				JsonUtils.buildJsonArray().add(1).add(2).build());

		Assert.assertEquals(JsonUtils.getAsJsonElement(new boolean[] { true, false }),
				JsonUtils.buildJsonArray().add(true).add(false).build());
		Assert.assertEquals(JsonUtils.getAsJsonElement(new short[] { 1, 2 }),
				JsonUtils.buildJsonArray().add(1).add(2).build());
		Assert.assertEquals(JsonUtils.getAsJsonElement(new int[] { 1, 2 }),
				JsonUtils.buildJsonArray().add(1).add(2).build());
		Assert.assertEquals(JsonUtils.getAsJsonElement(new long[] { 1, 2 }),
				JsonUtils.buildJsonArray().add(1).add(2).build());
		Assert.assertEquals(JsonUtils.getAsJsonElement(new float[] { 1, 2 }),
				JsonUtils.buildJsonArray().add(1).add(2).build());
		Assert.assertEquals(JsonUtils.getAsJsonElement(new double[] { 1, 2 }),
				JsonUtils.buildJsonArray().add(1).add(2).build());

	}

	@Test
	public void testGetAsBoolean() throws OpenemsNamedException {
		Assert.assertEquals(true, JsonUtils.getAsBoolean(new JsonPrimitive(true)));
		Assert.assertEquals(false, JsonUtils.getAsBoolean(new JsonPrimitive(false)));
		Assert.assertEquals(true, JsonUtils.getAsBoolean(new JsonPrimitive("TrUe")));
		Assert.assertEquals(false, JsonUtils.getAsBoolean(new JsonPrimitive("fAlSe")));
		try {
			JsonUtils.getAsBoolean(new JsonPrimitive("foo.bar"));
			Assert.fail();
		} catch (OpenemsNamedException e) {
			// ok
		}
	}

	@Test
	public void testGetAsInt() throws OpenemsNamedException {
		var arr = JsonUtils.buildJsonArray() //
				.add(10) //
				.add(20) //
				.add(30) //
				.build();

		try {
			JsonUtils.getAsInt(arr, -1);
			Assert.fail();
		} catch (OpenemsNamedException e) {
			// ok
		}
		Assert.assertEquals(10, JsonUtils.getAsInt(arr, 0));
		Assert.assertEquals(20, JsonUtils.getAsInt(arr, 1));
		Assert.assertEquals(30, JsonUtils.getAsInt(arr, 2));
		try {
			JsonUtils.getAsInt(arr, 3);
			Assert.fail();
		} catch (OpenemsNamedException e) {
			// ok
		}

	}

}
