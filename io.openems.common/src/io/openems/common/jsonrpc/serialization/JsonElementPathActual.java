package io.openems.common.jsonrpc.serialization;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsRuntimeException;
import io.openems.common.utils.JsonUtils;

public final class JsonElementPathActual {

	public static final class JsonElementPathActualNonNull implements JsonElementPath {

		private final JsonElement element;

		public JsonElementPathActualNonNull(JsonElement element) {
			this.element = Objects.requireNonNull(element);
		}

		@Override
		public JsonElement get() {
			return this.element;
		}

		@Override
		public JsonArrayPath getAsJsonArrayPath() {
			return new JsonArrayPathActual.JsonArrayPathActualNonNull(this.element.getAsJsonArray());
		}

		@Override
		public JsonObjectPath getAsJsonObjectPath() {
			return new JsonObjectPathActual.JsonObjectPathActualNonNull(this.element.getAsJsonObject());
		}

		@Override
		public JsonPrimitivePath getAsJsonPrimitivePath() {
			return new JsonPrimitivePathActual.JsonPrimitivePathActualNonNull(this.element.getAsJsonPrimitive());
		}

		@Override
		public <T> T multiple(List<Case<T>> cases) {
			return cases.stream() //
					.filter(t -> t.isApplicable().test(this)) //
					.map(t -> t.valueMapper().apply(this)) //
					.findAny().orElse(null);
		}

		@Override
		public <I, T> T polymorphic(//
				final Map<String, I> itemsByKey, //
				final Function<JsonElementPath, StringPath<String>> objectToKeyPath, //
				final BiFunction<JsonElementPath, Entry<String, I>, T> itemMapper //
		) {
			final var stringIdentifier = objectToKeyPath.apply(this).get();
			final var mapper = itemsByKey.get(stringIdentifier);

			if (mapper == null) {
				throw new OpenemsRuntimeException(
						"No serializer defined for polymorphic type '" + stringIdentifier + "'.");
			}

			return itemMapper.apply(this, Map.entry(stringIdentifier, mapper));
		}

		@Override
		public boolean isJsonPrimitive() {
			return this.element.isJsonPrimitive();
		}

		@Override
		public boolean isJsonObject() {
			return this.element.isJsonObject();
		}

		@Override
		public boolean isNumber() {
			return JsonUtils.isNumber(this.element);
		}

	}

	public static final class JsonElementPathActualNullable implements JsonElementPathNullable {

		private final JsonElement element;

		public JsonElementPathActualNullable(JsonElement element) {
			this.element = element == null || element.isJsonNull() ? null : element;
		}

		@Override
		public <T> T mapIfPresent(Function<JsonElementPath, T> mapper) {
			return this.element == null ? null : mapper.apply(new JsonElementPathActualNonNull(this.element));
		}

		@Override
		public JsonObjectPathNullable getAsJsonObjectPathNullable() {
			return new JsonObjectPathActual.JsonObjectPathActualNullable(
					this.element == null ? null : this.element.getAsJsonObject());
		}

		@Override
		public JsonArrayPathNullable getAsJsonArrayPathNullable() {
			return new JsonArrayPathActual.JsonArrayPathActualNullable(
					this.element == null ? null : this.element.getAsJsonArray());
		}

		@Override
		public JsonPrimitivePathNullable getAsJsonPrimitivePathNullable() {
			return new JsonPrimitivePathActual.JsonPrimitivePathActualNullable(
					this.element == null ? null : this.element.getAsJsonPrimitive());
		}

		@Override
		public boolean isPresent() {
			return this.element != null;
		}

		@Override
		public JsonElement getOrNull() {
			return this.element;
		}

	}

	private JsonElementPathActual() {
	}

}
