package io.openems.common.jsonrpc.serialization;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collector;

import com.google.gson.JsonObject;

public final class JsonObjectPathActual {

	public static class JsonObjectPathActualNonNull implements JsonObjectPath {

		private final JsonObject element;

		public JsonObjectPathActualNonNull(JsonObject object) {
			this.element = Objects.requireNonNull(object);
		}

		@Override
		public JsonElementPath getJsonElementPath(String member) {
			return new JsonElementPathActual.JsonElementPathActualNonNull(Objects.requireNonNull(this.get().get(member),
					"Member [" + member + "] was not part of " + this.element));
		}

		@Override
		public JsonElementPathNullable getNullableJsonElementPath(String member) {
			return new JsonElementPathActual.JsonElementPathActualNullable(this.get().get(member));
		}

		@Override
		public <S, A, R> R collect(//
				StringParser<S> keyParser, //
				Collector<Entry<StringPath<S>, JsonElementPath>, A, R> collector //
		) {
			return this.element.entrySet().stream() //
					.map(t -> Map.<StringPath<S>, JsonElementPath>entry(
							new StringPathActual.StringPathActualNonNull<S>(t.getKey(), keyParser::parse),
							new JsonElementPathActual.JsonElementPathActualNonNull(t.getValue()))) //
					.collect(collector);
		}

		@Override
		public JsonObject get() {
			return this.element;
		}

	}

	public static class JsonObjectPathActualNullable implements JsonObjectPathNullable {

		private final JsonObject element;

		public JsonObjectPathActualNullable(JsonObject object) {
			this.element = object;
		}

		@Override
		public <T> T mapIfPresent(Function<JsonObjectPath, T> mapping) {
			if (this.element == null) {
				return null;
			}
			return mapping.apply(new JsonObjectPathActualNonNull(this.element));
		}

		@Override
		public boolean isPresent() {
			return this.element != null;
		}

		@Override
		public JsonObject getOrNull() {
			return this.element;
		}

	}

	private JsonObjectPathActual() {
	}

}