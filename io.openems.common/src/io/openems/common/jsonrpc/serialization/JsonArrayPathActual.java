package io.openems.common.jsonrpc.serialization;

import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collector;

import com.google.gson.JsonArray;

import io.openems.common.utils.JsonUtils;

public final class JsonArrayPathActual {

	public static final class JsonArrayPathActualNonNull implements JsonArrayPath {

		private final JsonArray element;

		public JsonArrayPathActualNonNull(JsonArray array) {
			this.element = Objects.requireNonNull(array);
		}

		@Override
		public <A, R> R collect(Collector<JsonElementPath, A, R> collector) {
			return JsonUtils.stream(this.element) //
					.map(JsonElementPathActual.JsonElementPathActualNonNull::new) //
					.collect(collector);
		}

		@Override
		public JsonArray get() {
			return this.element;
		}

	}

	public static final class JsonArrayPathActualNullable implements JsonArrayPathNullable {

		private final JsonArray element;

		public JsonArrayPathActualNullable(JsonArray array) {
			this.element = array;
		}

		@Override
		public <T> T mapIfPresent(Function<JsonArrayPath, T> mapper) {
			return this.element == null ? null : mapper.apply(new JsonArrayPathActualNonNull(this.element));
		}

		@Override
		public boolean isPresent() {
			return this.element != null;
		}

		@Override
		public JsonArray getOrNull() {
			return this.element;
		}

	}

	private JsonArrayPathActual() {
	}

}