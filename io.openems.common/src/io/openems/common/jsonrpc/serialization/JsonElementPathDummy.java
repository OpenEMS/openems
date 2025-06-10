package io.openems.common.jsonrpc.serialization;

import static io.openems.common.utils.JsonUtils.toJsonArray;
import static java.util.stream.Collectors.toMap;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import io.openems.common.exceptions.OpenemsRuntimeException;
import io.openems.common.utils.JsonUtils;

public abstract sealed class JsonElementPathDummy implements JsonPathDummy {

	private List<JsonPathDummy> dummyPaths;

	@SuppressWarnings("unchecked")
	protected <T extends JsonPathDummy> T withDummyPath(T path) {
		if (this.dummyPaths != null) {
			if (this.dummyPaths.size() != 1 || !this.dummyPaths.get(0).getClass().equals(path.getClass())) {
				throw new OpenemsRuntimeException("Path already set");
			}
			return (T) this.dummyPaths.get(0);
		}
		this.dummyPaths = List.of(path);
		return path;
	}

	protected void withDummyPaths(List<JsonPathDummy> paths) {
		if (this.dummyPaths != null) {
			throw new OpenemsRuntimeException("Path already set");
		}
		this.dummyPaths = paths;
	}

	@Override
	public JsonElement buildPath() {
		if (this.dummyPaths == null) {
			return JsonUtils.buildJsonObject() //
					.addProperty("type", "element") //
					.build();
		}
		if (this.dummyPaths.size() == 1) {
			return this.dummyPaths.get(0).buildPath();
		}
		return JsonUtils.buildJsonObject() //
				.addProperty("type", "multiple") //
				.add("validTypes", this.dummyPaths.stream() //
						.map(JsonPathDummy::buildPath) //
						.collect(toJsonArray())) //
				.build();
	}

	public static final class JsonElementPathDummyNonNull extends JsonElementPathDummy
			implements JsonElementPath, JsonPathDummy {

		@Override
		public JsonElement get() {
			return new JsonPrimitive(false);
		}

		@Override
		public JsonArrayPath getAsJsonArrayPath() {
			return this.withDummyPath(new JsonArrayPathDummy.JsonArrayPathDummyNonNull());
		}

		@Override
		public JsonObjectPath getAsJsonObjectPath() {
			return this.withDummyPath(new JsonObjectPathDummy.JsonObjectPathDummyNonNull());
		}

		@Override
		public JsonPrimitivePath getAsJsonPrimitivePath() {
			return this.withDummyPath(new JsonPrimitivePathDummy.JsonPrimitivePathDummyNonNull());
		}

		@Override
		public <T> T multiple(List<Case<T>> cases) {
			final var anyResult = new AtomicReference<T>();
			this.withDummyPaths(cases.stream().<JsonPathDummy>map(c -> {
				final var dummyPath = new JsonElementPathDummyNonNull();
				final var result = c.valueMapper().apply(dummyPath);
				if (result != null) {
					anyResult.set(result);
				}
				return dummyPath;
			}).toList());
			return anyResult.get();
		}

		@Override
		public boolean isJsonPrimitive() {
			return false;
		}

		@Override
		public boolean isJsonObject() {
			return false;
		}

		@Override
		public boolean isNumber() {
			return false;
		}

		@Override
		public <I, T> T polymorphic(//
				Map<String, I> itemsByKey, //
				Function<JsonElementPath, StringPath<String>> objectToKeyPath, //
				BiFunction<JsonElementPath, Entry<String, I>, T> itemMapper //
		) {
			final var paths = itemsByKey.entrySet().stream() //
					.map(t -> {
						final var path = new JsonElementPathDummy.JsonElementPathDummyNonNull();
						// TODO requires exact string t.getKey()
						objectToKeyPath.apply(path);
						itemMapper.apply(path, t);
						return Map.<T, JsonPathDummy>entry(itemMapper.apply(path, t), path);
					}) //
					.collect(toMap(Entry::getKey, Entry::getValue));

			this.withDummyPaths(paths.values().stream().toList());

			return paths.keySet().stream().findFirst().get();
		}

	}

	public static final class JsonElementPathDummyNullable extends JsonElementPathDummy
			implements JsonElementPathNullable, JsonPathDummy {

		@Override
		public <T> T mapIfPresent(Function<JsonElementPath, T> mapper) {
			return mapper.apply(this.withDummyPath(new JsonElementPathDummyNonNull()));
		}

		@Override
		public JsonObjectPathNullable getAsJsonObjectPathNullable() {
			return this.withDummyPath(new JsonObjectPathDummy.JsonObjectPathDummyNullable());
		}

		@Override
		public JsonArrayPathNullable getAsJsonArrayPathNullable() {
			return this.withDummyPath(new JsonArrayPathDummy.JsonArrayPathDummyNullable());
		}

		@Override
		public JsonPrimitivePathNullable getAsJsonPrimitivePathNullable() {
			return this.withDummyPath(new JsonPrimitivePathDummy.JsonPrimitivePathDummyNullable());
		}

		@Override
		public boolean isPresent() {
			return false;
		}

		@Override
		public JsonElement getOrNull() {
			return null;
		}

	}

}
