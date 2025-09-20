package io.openems.common.jsonrpc.serialization;

import static io.openems.common.utils.FunctionUtils.lazySingleton;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public final class StringPathActual {

	public static final class StringPathActualNonNull<T> implements StringPath<T> {

		private final Function<String, T> parser;
		private final String element;
		private final Supplier<T> parsedValue;

		public StringPathActualNonNull(String element, Function<String, T> parser) {
			super();
			this.parser = Objects.requireNonNull(parser);
			this.element = Objects.requireNonNull(element);

			this.parsedValue = lazySingleton(() -> this.parser.apply(this.element));
		}

		@Override
		public String getRaw() {
			return this.element;
		}

		@Override
		public T get() {
			return this.parsedValue.get();
		}

	}

	public static final class StringPathActualNullable<T> implements StringPathNullable<T> {

		private final Function<String, T> parser;
		private final String element;
		private final Supplier<T> parsedValue;

		public StringPathActualNullable(String element, Function<String, T> parser) {
			super();
			this.parser = Objects.requireNonNull(parser);
			this.element = element;

			this.parsedValue = this.element == null //
					? () -> null //
					: lazySingleton(() -> this.parser.apply(this.element));
		}

		@Override
		public String getRawOrNull() {
			return this.element;
		}

		@Override
		public T getOrNull() {
			return this.parsedValue.get();
		}

	}

	private StringPathActual() {
	}

}
