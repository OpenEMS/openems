package io.openems.common.jsonrpc.serialization;

import java.util.Objects;

public interface StringParser<T> {

	/**
	 * Parses the provided string to the type of <code>T</code>.
	 * 
	 * @param value the value to parse
	 * @return the parsed result object
	 */
	public T parse(String value);

	/**
	 * Provides example values of the possible outcome of this parser.
	 * 
	 * @return the {@link ExampleValues} of this parser
	 */
	public ExampleValues<T> getExample();

	public record ExampleValues<T>(String raw, T value) {

		public ExampleValues {
			Objects.requireNonNull(raw);
			Objects.requireNonNull(value);
		}

	}

}
