package io.openems.common.jsonrpc.serialization;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.enumSerializerFromObjectNullable;
import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;

import io.openems.common.utils.JsonUtils;

class JsonSerializerTest {

	enum SampleEnum {
		FIRST, SECOND
	}

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

	private final JsonSerializer<SampleRecord> serializer = SampleRecord.serializer();

	@Test
	void testSimpleObjectSerialize() {
		final var expectedString = "expectedString";
		final var serializedObj = this.serializer.serialize(new SampleRecord(expectedString));
		assertEquals(JsonUtils.buildJsonObject() //
				.addProperty("sampleString", expectedString) //
				.build(), serializedObj);
	}

	@Test
	void testSimpleObjectDeserialize() {
		final var expectedString = "expectedString";
		final var parsedObj = this.serializer.deserialize(JsonUtils.buildJsonObject() //
				.addProperty("sampleString", expectedString) //
				.build());
		assertEquals(expectedString, parsedObj.sampleString());
	}

	@Test
	void testObjectDeserializeOfDifferentType() {
		assertThrows(RuntimeException.class, () -> this.serializer.deserialize(new JsonArray()));
	}

	@Test
	void testDescriptor() {
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

	@Test
	void testEnumSerializerFromObjectSerialize() {
		final var enumSerializer = enumSerializerFromObjectNullable("sampleEnum", SampleEnum.class);

		assertEquals(JsonUtils.buildJsonObject() //
				.addProperty("sampleEnum", SampleEnum.FIRST.name()) //
				.build(), enumSerializer.serialize(SampleEnum.FIRST));
	}

	@Test
	void testEnumSerializerFromObjectDeserializeNullableWithoutMember() {
		final var enumSerializer = enumSerializerFromObjectNullable("sampleEnum", SampleEnum.class);

		assertNull(enumSerializer.deserializeNullable(JsonUtils.buildJsonObject() //
				.addProperty("otherProperty", "value") //
				.build()));
	}

	@Test
	void testEnumSerializerFromObjectDeserializeNullableWithNullObject() {
		final var enumSerializer = enumSerializerFromObjectNullable("sampleEnum", SampleEnum.class);

		assertNull(enumSerializer.deserializeNullable(null));
	}

	@Test
	void testEnumSerializerFromObjectDeserializeNullableWithNullValue() {
		final var enumSerializer = enumSerializerFromObjectNullable("sampleEnum", SampleEnum.class);

		assertNull(enumSerializer.deserializeNullable(JsonUtils.buildJsonObject() //
				.add("sampleEnum", JsonNull.INSTANCE) //
				.build()));
	}

	@Test
	void testEnumSerializerFromObjectDeserializeWithInvalidValue() {
		final var enumSerializer = enumSerializerFromObjectNullable("sampleEnum", SampleEnum.class);

		assertThrows(JsonParseException.class, () -> enumSerializer //
				.deserialize(JsonUtils.buildJsonObject() //
						.addProperty("sampleEnum", "INVALID") //
						.build()));
	}

}
