package io.openems.common.jsonrpc.serialization;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.google.gson.JsonArray;

import io.openems.common.utils.JsonUtils;

public class JsonSerializerTest {

	record SampleRecord(String sampleString) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link JsonSerializerTest}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<SampleRecord> serializer() {
			return jsonObjectSerializer(SampleRecord.class, json -> {
				return new SampleRecord(//
						json.getString("sampleString") //
				);
			}, obj -> {
				return JsonUtils.buildJsonObject() //
						.addProperty("sampleString", obj.sampleString()) //
						.build();
			});
		}
	}

	public final JsonSerializer<SampleRecord> serializer = SampleRecord.serializer();

	@Test
	public void testSimpleObjectSerialize() {
		final var expectedString = "expectedString";
		final var serializedObj = this.serializer.serialize(new SampleRecord(expectedString));
		assertEquals(JsonUtils.buildJsonObject() //
				.addProperty("sampleString", expectedString) //
				.build(), serializedObj);
	}

	@Test
	public void testSimpleObjectDeserialize() {
		final var expectedString = "expectedString";
		final var parsedObj = this.serializer.deserialize(JsonUtils.buildJsonObject() //
				.addProperty("sampleString", expectedString) //
				.build());
		assertEquals(expectedString, parsedObj.sampleString());
	}

	@Test(expected = RuntimeException.class)
	public void testObjectDeserializeOfDifferentType() {
		this.serializer.deserialize(new JsonArray());
	}

	@Test
	public void testDescriptor() {
		final var objectDescriptor = this.serializer.descriptor();
		final var jsonDescription = objectDescriptor.toJson();

		assertEquals(JsonUtils.buildJsonObject() //
				.addProperty("type", "object") //
				.addProperty("optional", false) //
				.add("properties", JsonUtils.buildJsonObject() //
						.add("sampleString", JsonUtils.buildJsonObject() //
								.addProperty("type", "string") //
								.addProperty("optional", false) //
								.build())
						.build()) //
				.build(), jsonDescription);
	}

}
