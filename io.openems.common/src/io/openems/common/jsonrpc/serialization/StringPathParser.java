package io.openems.common.jsonrpc.serialization;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

import io.openems.common.exceptions.OpenemsRuntimeException;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.SemanticVersion;

public final class StringPathParser {

	public static class StringParserString implements StringParser<String> {

		@Override
		public String parse(String value) {
			return value;
		}

		@Override
		public ExampleValues<String> getExample() {
			return new ExampleValues<>("string", "string");
		}

	}

	public static class StringParserUuid implements StringParser<UUID> {

		@Override
		public UUID parse(String value) {
			return UUID.fromString(value);
		}

		@Override
		public ExampleValues<UUID> getExample() {
			final var id = UUID.randomUUID();
			return new ExampleValues<>(id.toString(), id);
		}

	}

	public static class StringParserEnum<T extends Enum<T>> implements StringParser<T> {

		private final Class<T> enumClass;

		public StringParserEnum(Class<T> enumClass) {
			super();
			this.enumClass = enumClass;
		}

		@Override
		public T parse(String value) {
			// TODO always toUppercase?
			return Enum.valueOf(this.enumClass, value.toUpperCase(Locale.ROOT));
		}

		@Override
		public ExampleValues<T> getExample() {
			final var value = this.enumClass.getEnumConstants().length > 0//
					? this.enumClass.getEnumConstants()[0]
					: null;
			return new ExampleValues<>(value == null ? null : value.name(), value);
		}
	}

	public static class StringParserChannelAddress implements StringParser<ChannelAddress> {

		@Override
		public ChannelAddress parse(String value) {
			final var parts = value.split("/");
			if (parts.length != 2) {
				throw new OpenemsRuntimeException("Parse error");
			}
			return new ChannelAddress(parts[0], parts[1]);
		}

		@Override
		public ExampleValues<ChannelAddress> getExample() {
			final var exampleAddress = new ChannelAddress("component0", "channel");
			return new ExampleValues<>(exampleAddress.toString(), exampleAddress);
		}

	}

	public static class StringParserZonedDateTime implements StringParser<ZonedDateTime> {

		private final DateTimeFormatter formatter;

		public StringParserZonedDateTime(DateTimeFormatter formatter) {
			super();
			this.formatter = formatter;
		}

		public StringParserZonedDateTime() {
			this(DateTimeFormatter.ISO_ZONED_DATE_TIME);
		}

		@Override
		public ZonedDateTime parse(String value) {
			return ZonedDateTime.parse(value, this.formatter);
		}

		@Override
		public ExampleValues<ZonedDateTime> getExample() {
			final var timestamp = ZonedDateTime.now();
			return new ExampleValues<>(timestamp.format(this.formatter), timestamp);
		}

	}

	public static class StringParserLocalDate implements StringParser<LocalDate> {

		private final DateTimeFormatter formatter;

		public StringParserLocalDate(DateTimeFormatter formatter) {
			super();
			this.formatter = formatter;
		}

		public StringParserLocalDate() {
			this(DateTimeFormatter.ISO_LOCAL_DATE);
		}

		@Override
		public LocalDate parse(String value) {
			return LocalDate.parse(value, this.formatter);
		}

		@Override
		public ExampleValues<LocalDate> getExample() {
			final var timestamp = LocalDate.now();
			return new ExampleValues<>(timestamp.format(this.formatter), timestamp);
		}

	}

	public static class StringParserSemanticVersion implements StringParser<SemanticVersion> {

		@Override
		public SemanticVersion parse(String value) {
			try {
				return SemanticVersion.fromString(value);
			} catch (Exception e) {
				throw new OpenemsRuntimeException("Parse error", e);
			}
		}

		@Override
		public ExampleValues<SemanticVersion> getExample() {
			final var version = SemanticVersion.ZERO;
			return new ExampleValues<>(version.toString(), version);
		}

	}

	private StringPathParser() {
	}

}
