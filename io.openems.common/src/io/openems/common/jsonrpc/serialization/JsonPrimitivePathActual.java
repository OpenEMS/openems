package io.openems.common.jsonrpc.serialization;

import java.util.Objects;

import com.google.gson.JsonPrimitive;

import io.openems.common.exceptions.OpenemsRuntimeException;

public final class JsonPrimitivePathActual {

	public static class JsonPrimitivePathActualNonNull implements JsonPrimitivePath {

		private final JsonPrimitive element;

		public JsonPrimitivePathActualNonNull(JsonPrimitive element) {
			super();
			this.element = Objects.requireNonNull(element);
		}

		@Override
		public <T> StringPath<T> getAsStringPath(StringParser<T> parser) {
			return new StringPathActual.StringPathActualNonNull<>(parseToStrictString(this.element), parser::parse);
		}

		@Override
		public NumberPath getAsNumberPath() {
			return new NumberPathActual.NumberPathActualNonNull(parseToStrictNumber(this.element));
		}

		@Override
		public BooleanPath getAsBooleanPath() {
			return new BooleanPathActual.BooleanPathActualNonNull(parseToStrictBoolean(this.element));
		}

		@Override
		public JsonPrimitive get() {
			return this.element;
		}

	}

	public static class JsonPrimitivePathActualNullable implements JsonPrimitivePathNullable {

		private final JsonPrimitive element;

		public JsonPrimitivePathActualNullable(JsonPrimitive element) {
			super();
			this.element = element;
		}

		@Override
		public BooleanPathNullable getAsBooleanPathNullable() {
			return new BooleanPathActual.BooleanPathActualNullable(
					this.element == null ? null : parseToStrictBoolean(this.element));
		}

		@Override
		public NumberPathNullable getAsNumberPathNullable() {
			return new NumberPathActual.NumberPathActualNullable(
					this.element == null ? null : parseToStrictNumber(this.element));
		}

		@Override
		public <T> StringPathNullable<T> getAsStringPathNullable(StringParser<T> parser) {
			return new StringPathActual.StringPathActualNullable<>(
					this.element == null ? null : parseToStrictString(this.element), parser::parse);
		}

		@Override
		public boolean isPresent() {
			return this.element != null;
		}

		@Override
		public JsonPrimitive getOrNull() {
			return this.element;
		}

	}

	private JsonPrimitivePathActual() {
	}

	private static String parseToStrictString(JsonPrimitive element) {
		if (!element.isString()) {
			throw new OpenemsRuntimeException("Unable to parse \"" + element + "\" to String");
		}
		return element.getAsString();
	}

	private static boolean parseToStrictBoolean(JsonPrimitive element) {
		if (!element.isBoolean()) {
			throw new OpenemsRuntimeException("Unable to parse \"" + element + "\" to boolean");
		}
		return element.getAsBoolean();
	}

	private static Number parseToStrictNumber(JsonPrimitive element) {
		if (!element.isNumber()) {
			throw new OpenemsRuntimeException("Unable to parse \"" + element + "\" to Number");
		}
		return element.getAsNumber();
	}

}
