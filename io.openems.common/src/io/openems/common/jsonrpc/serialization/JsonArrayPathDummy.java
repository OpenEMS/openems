package io.openems.common.jsonrpc.serialization;

import java.util.function.Function;
import java.util.stream.Collector;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import io.openems.common.utils.JsonUtils;

public final class JsonArrayPathDummy {

	public static final class JsonArrayPathDummyNonNull implements JsonArrayPath, JsonPathDummy {

		private JsonPathDummy elementType;

		@Override
		public <A, R> R collect(Collector<JsonElementPath, A, R> collector) {
			final var supplier = collector.supplier().get();
			final var path = new JsonElementPathDummy.JsonElementPathDummyNonNull();
			collector.accumulator().accept(supplier, path);
			this.elementType = path;

			return collector.finisher().apply(supplier);
		}

		@Override
		public JsonArray get() {
			return new JsonArray();
		}

		@Override
		public JsonElement buildPath() {
			return JsonUtils.buildJsonObject() //
					.addProperty("type", "array") //
					.addProperty("optional", false) //
					.onlyIf(this.elementType != null, t -> t.add("elementType", this.elementType.buildPath())) //
					.build();
		}

	}

	public static final class JsonArrayPathDummyNullable implements JsonArrayPathNullable, JsonPathDummy {

		private JsonArrayPathDummyNonNull dummyPath;

		@Override
		public <T> T mapIfPresent(Function<JsonArrayPath, T> mapper) {
			mapper.apply(this.dummyPath = new JsonArrayPathDummyNonNull());
			return null;
		}

		@Override
		public boolean isPresent() {
			return false;
		}

		@Override
		public JsonArray getOrNull() {
			return null;
		}

		@Override
		public JsonElement buildPath() {
			return JsonUtils.buildJsonObject() //
					.addProperty("type", "array") //
					.addProperty("optional", true) //
					.onlyIf(this.dummyPath != null && this.dummyPath.elementType != null,
							t -> t.add("elementType", this.dummyPath.elementType.buildPath())) //
					.build();
		}

	}

	private JsonArrayPathDummy() {
	}

}
