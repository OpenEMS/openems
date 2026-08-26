package io.openems.common.jsonrpc.serialization;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import io.openems.common.utils.JsonUtils;

public final class JsonPrimitivePathDummy {

	/**
	 * Builds a simple path for primitives.
	 * 
	 * @param type     the type of the path
	 * @param optional true if the path is optional else false
	 * @return the created path
	 */
	public static JsonElement buildPath(//
			final String type, //
			final boolean optional //
	) {
		return JsonUtils.buildJsonObject() //
				.addProperty("type", type) //
				.addProperty("optional", optional) //
				.build();
	}

	public static final class JsonPrimitivePathDummyNonNull implements JsonPrimitivePath, JsonPathDummy {

		private JsonPathDummy actualType;

		@Override
		public <T> StringPath<T> getAsStringPath(StringParser<T> parser) {
			final var example = parser.getExample();
			return this.withPath(new StringPathDummy.StringPathDummyNonNull<>(example.raw(), example.value()));
		}

		@Override
		public NumberPath getAsNumberPath() {
			return this.withPath(new NumberPathDummy.NumberPathDummyNonNull());
		}

		@Override
		public BooleanPath getAsBooleanPath() {
			return this.withPath(new BooleanPathDummy.BooleanPathDummyNonNull());
		}

		@Override
		public JsonPrimitive get() {
			return new JsonPrimitive(0);
		}

		@Override
		public JsonElement buildPath() {
			if (this.actualType != null) {
				return this.actualType.buildPath();
			}
			return JsonPrimitivePathDummy.buildPath("primitive", false);
		}

		private <T extends JsonPathDummy> T withPath(T type) {
			this.actualType = type;
			return type;
		}

	}

	public static final class JsonPrimitivePathDummyNullable implements JsonPrimitivePathNullable, JsonPathDummy {

		private JsonPathDummy actualType;

		@Override
		public BooleanPathNullable getAsBooleanPathNullable() {
			return this.withPath(new BooleanPathDummy.BooleanPathDummyNullable());
		}

		@Override
		public NumberPathNullable getAsNumberPathNullable() {
			return this.withPath(new NumberPathDummy.NumberPathDummyNullable());
		}

		@Override
		public <T> StringPathNullable<T> getAsStringPathNullable(StringParser<T> parser) {
			return this.withPath(new StringPathDummy.StringPathDummyNullable<>());
		}

		@Override
		public boolean isPresent() {
			return false;
		}

		@Override
		public JsonPrimitive getOrNull() {
			return null;
		}

		@Override
		public JsonElement buildPath() {
			if (this.actualType != null) {
				return this.actualType.buildPath();
			}
			return JsonPrimitivePathDummy.buildPath("primitive", true);
		}

		private <T extends JsonPathDummy> T withPath(T type) {
			this.actualType = type;
			return type;
		}

	}

	private JsonPrimitivePathDummy() {
	}

}
