package io.openems.common.jsonrpc.serialization;

import static io.openems.common.utils.JsonUtils.toJsonObject;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collector;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.utils.JsonUtils;

public final class JsonObjectPathDummy {

	public static final class JsonObjectPathDummyNonNull implements JsonObjectPath, JsonPathDummy {

		private final Map<String, JsonPathDummy> paths = new TreeMap<>();

		@Override
		public JsonElementPath getJsonElementPath(String member) {
			return this.withDummyPath(member, new JsonElementPathDummy.JsonElementPathDummyNonNull());
		}

		@Override
		public JsonElementPathNullable getNullableJsonElementPath(String member) {
			return this.withDummyPath(member, new JsonElementPathDummy.JsonElementPathDummyNullable());
		}

		@Override
		public <S, A, R> R collect(//
				StringParser<S> keyParser, //
				Collector<Entry<StringPath<S>, JsonElementPath>, A, R> collector //
		) {
			final var resultContainer = collector.supplier().get();
			final var dummyPath = new JsonElementPathDummy.JsonElementPathDummyNonNull();
			final var example = keyParser.getExample();
			collector.accumulator().accept(resultContainer,
					Map.entry(new StringPathDummy.StringPathDummyNonNull<>(example.raw(), example.value()), dummyPath));
			// TODO should be special case
			this.withDummyPath(example.raw(), dummyPath);
			return collector.finisher().apply(resultContainer);
		}

		@Override
		public JsonObject get() {
			return new JsonObject();
		}

		@Override
		public JsonElement buildPath() {
			return JsonUtils.buildJsonObject() //
					.addProperty("type", "object") //
					.addProperty("optional", false) //
					.add("properties", this.paths.entrySet().stream() //
							.collect(toJsonObject(Entry::getKey, input -> input.getValue().buildPath()))) //
					.build();
		}

		private final <T extends JsonPathDummy> T withDummyPath(String member, T path) {
			this.paths.put(member, path);
			return path;
		}

	}

	public static final class JsonObjectPathDummyNullable implements JsonObjectPathNullable, JsonPathDummy {

		private JsonObjectPathDummyNonNull dummyNonNullPath;

		@Override
		public <T> T mapIfPresent(Function<JsonObjectPath, T> mapping) {
			final var path = new JsonObjectPathDummyNonNull();
			mapping.apply(path);
			return null;
		}

		@Override
		public boolean isPresent() {
			return false;
		}

		@Override
		public JsonObject getOrNull() {
			return null;
		}

		@Override
		public JsonElement buildPath() {
			return JsonUtils.buildJsonObject() //
					.addProperty("type", "object") //
					.addProperty("optional", true) //
					.add("properties", this.dummyNonNullPath == null ? new JsonObject()
							: this.dummyNonNullPath.paths.entrySet().stream() //
									.collect(toJsonObject(Entry::getKey, input -> input.getValue().buildPath()))) //
					.build();
		}

	}

	private JsonObjectPathDummy() {
	}

}
