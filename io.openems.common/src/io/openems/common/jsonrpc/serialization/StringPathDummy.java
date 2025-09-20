package io.openems.common.jsonrpc.serialization;

import com.google.gson.JsonElement;

public abstract class StringPathDummy implements JsonPathDummy {

	private final boolean nullable;

	private StringPathDummy(boolean nullable) {
		super();
		this.nullable = nullable;
	}

	public static final class StringPathDummyNonNull<T> extends StringPathDummy implements StringPath<T> {

		private final String rawValue;
		private final T value;

		public StringPathDummyNonNull(String rawValue, T value) {
			super(false);
			this.rawValue = rawValue;
			this.value = value;
		}

		@Override
		public String getRaw() {
			return this.rawValue;
		}

		@Override
		public T get() {
			return this.value;
		}

	}

	public static final class StringPathDummyNullable<T> extends StringPathDummy implements StringPathNullable<T> {

		public StringPathDummyNullable() {
			super(true);
		}

		@Override
		public String getRawOrNull() {
			return null;
		}

		@Override
		public T getOrNull() {
			return null;
		}

	}

	@Override
	public JsonElement buildPath() {
		return JsonPrimitivePathDummy.buildPath("string", this.nullable);
	}

}
