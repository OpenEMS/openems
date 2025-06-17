package io.openems.common.jsonrpc.serialization;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

import com.google.gson.JsonPrimitive;

import io.openems.common.jsonrpc.serialization.StringPathParser.StringParserChannelAddress;
import io.openems.common.jsonrpc.serialization.StringPathParser.StringParserEnum;
import io.openems.common.jsonrpc.serialization.StringPathParser.StringParserLocalDate;
import io.openems.common.jsonrpc.serialization.StringPathParser.StringParserLocalTime;
import io.openems.common.jsonrpc.serialization.StringPathParser.StringParserSemanticVersion;
import io.openems.common.jsonrpc.serialization.StringPathParser.StringParserString;
import io.openems.common.jsonrpc.serialization.StringPathParser.StringParserUuid;
import io.openems.common.jsonrpc.serialization.StringPathParser.StringParserZonedDateTime;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.SemanticVersion;

public interface JsonPrimitivePathNullable extends JsonPath {

	/**
	 * Gets the current {@link JsonPrimitivePathNullable} as a
	 * {@link StringPathNullable}.
	 * 
	 * @param <T>    the actual type of the string value
	 * @param parser the parser to parse the string
	 * @return the current element as a {@link StringPathNullable}
	 */
	public <T> StringPathNullable<T> getAsStringPathNullable(StringParser<T> parser);

	/**
	 * Gets the current {@link JsonPrimitivePathNullable} parsed if present else
	 * null.
	 * 
	 * @param <T>    the type of the result object
	 * @param parser the parser to use to parse the string value
	 * @return the parsed object or null if not present
	 */
	public default <T> T getAsStringParsedOrNull(StringParser<T> parser) {
		return this.getAsStringPathNullable(parser).getOrNull();
	}

	/**
	 * Gets the current {@link JsonPrimitivePathNullable} as a {@link Optional}.
	 * 
	 * @param <T>    the type of the result object
	 * @param parser the parser to use to parse the string value
	 * @return a {@link Optional} of the parsed string or empty if not present
	 */
	public default <T> Optional<T> getAsOptionalStringParsed(StringParser<T> parser) {
		return this.getAsStringPathNullable(parser).getOptional();
	}

	/**
	 * Gets the current {@link JsonPrimitivePathNullable} as a
	 * {@link StringPathNullable} which just contains its raw string as the parsed
	 * value.
	 * 
	 * @return the current element as a {@link StringPathNullable}
	 */
	public default StringPathNullable<String> getAsStringPathNullableString() {
		return this.getAsStringPathNullable(new StringParserString());
	}

	/**
	 * Gets the current {@link JsonPrimitivePathNullable} as a
	 * {@link StringPathNullable} which contains a {@link ChannelAddress} as its
	 * parsed value.
	 * 
	 * @return the current element as a {@link StringPathNullable}
	 */
	public default StringPathNullable<ChannelAddress> getAsStringPathNullableChannelAddress() {
		return this.getAsStringPathNullable(new StringParserChannelAddress());
	}

	/**
	 * Gets the current {@link JsonPrimitivePathNullable} as a
	 * {@link StringPathNullable} which contains a {@link UUID} as its parsed value.
	 * 
	 * @return the current element as a {@link StringPathNullable}
	 */
	public default StringPathNullable<UUID> getAsStringPathNullableUuid() {
		return this.getAsStringPathNullable(new StringParserUuid());
	}

	/**
	 * Gets the current {@link JsonPrimitivePathNullable} as a
	 * {@link StringPathNullable} which contains a {@link SemanticVersion} as its
	 * parsed value.
	 * 
	 * @return the current element as a {@link StringPathNullable}
	 */
	public default StringPathNullable<SemanticVersion> getAsStringPathNullableSemanticVersion() {
		return this.getAsStringPathNullable(new StringParserSemanticVersion());
	}

	/**
	 * Gets the current {@link JsonPrimitivePathNullable} as a
	 * {@link StringPathNullable} which contains a {@link Enum} as its parsed value.
	 * 
	 * @param <T>       the type of the enum value
	 * @param enumClass the type class of the enum
	 * @return the current element as a {@link StringPathNullable}
	 */
	public default <T extends Enum<T>> StringPathNullable<T> getAsStringPathNullableEnum(Class<T> enumClass) {
		return this.getAsStringPathNullable(new StringParserEnum<>(enumClass));
	}

	/**
	 * Gets the current {@link JsonPrimitivePathNullable} as a
	 * {@link StringPathNullable} which contains a {@link ZonedDateTime} as its
	 * parsed value.
	 * 
	 * @param formatter the {@link DateTimeFormatter} used to parse the string
	 * @return the current element as a {@link StringPathNullable}
	 */
	public default StringPathNullable<ZonedDateTime> getAsStringPathNullableZonedDateTime(DateTimeFormatter formatter) {
		return this.getAsStringPathNullable(new StringParserZonedDateTime(formatter));
	}

	/**
	 * Gets the current {@link JsonPrimitivePathNullable} as a
	 * {@link StringPathNullable} which contains a {@link ZonedDateTime} as its
	 * parsed value.
	 * 
	 * @return the current element as a {@link StringPathNullable}
	 */
	public default StringPathNullable<ZonedDateTime> getAsStringPathNullableZonedDateTime() {
		return this.getAsStringPathNullable(new StringParserZonedDateTime());
	}

	/**
	 * Gets the current {@link JsonPrimitivePathNullable} as a
	 * {@link StringPathNullable} which contains a {@link LocalDate} as its parsed
	 * value.
	 * 
	 * @param formatter the {@link DateTimeFormatter} used to parse the string
	 * @return the current element as a {@link StringPathNullable}
	 */
	public default StringPathNullable<LocalDate> getAsStringPathNullableLocalDate(DateTimeFormatter formatter) {
		return this.getAsStringPathNullable(new StringParserLocalDate(formatter));
	}

	/**
	 * Gets the current {@link JsonPrimitivePathNullable} as a
	 * {@link StringPathNullable} which contains a {@link LocalDate} as its parsed
	 * value.
	 * 
	 * @return the current element as a {@link StringPathNullable}
	 */
	public default StringPathNullable<LocalDate> getAsStringPathNullableLocalDate() {
		return this.getAsStringPathNullable(new StringParserLocalDate());
	}

	/**
	 * Gets the current {@link JsonPrimitivePathNullable} as a
	 * {@link StringPathNullable} which contains a {@link LocalTime} as its parsed
	 * value.
	 * 
	 * @param formatter the {@link DateTimeFormatter} used to parse the string
	 * @return the current element as a {@link StringPathNullable}
	 */
	public default StringPathNullable<LocalTime> getAsStringPathNullableLocalTime(DateTimeFormatter formatter) {
		return this.getAsStringPathNullable(new StringParserLocalTime(formatter));
	}

	/**
	 * Gets the current {@link JsonPrimitivePathNullable} as a
	 * {@link StringPathNullable} which contains a {@link LocalTime} as its parsed
	 * value.
	 * 
	 * @return the current element as a {@link StringPathNullable}
	 */
	public default StringPathNullable<LocalTime> getAsStringPathNullableLocalTime() {
		return this.getAsStringPathNullable(new StringParserLocalTime());
	}

	/**
	 * Gets the current {@link JsonPrimitivePathNullable} as a {@link String} or
	 * <code>null</code>.
	 * 
	 * @return the current element as a {@link String} or null if not existing
	 */
	public default String getAsStringOrNull() {
		return this.getAsStringPathNullableString().getOrNull();
	}

	/**
	 * Gets the current {@link JsonPrimitivePathNullable} as a {@link Optional} of
	 * {@link String}.
	 * 
	 * @return the current element as a {@link Optional} of {@link String}
	 */
	public default Optional<String> getAsOptionalString() {
		return this.getAsStringPathNullableString().getOptional();
	}

	/**
	 * Gets the current {@link JsonPrimitivePathNullable} as a
	 * {@link ChannelAddress} or <code>null</code>.
	 * 
	 * @return the current element as a {@link ChannelAddress} or null if not
	 *         existing
	 */
	public default ChannelAddress getAsChannelAddressOrNull() {
		return this.getAsStringPathNullableChannelAddress().getOrNull();
	}

	/**
	 * Gets the current {@link JsonPrimitivePathNullable} as a {@link Optional} of
	 * {@link ChannelAddress}.
	 * 
	 * @return the current element as a {@link Optional} of {@link ChannelAddress}
	 */
	public default Optional<ChannelAddress> getAsOptionalChannelAddress() {
		return this.getAsStringPathNullableChannelAddress().getOptional();
	}

	/**
	 * Gets the current {@link JsonPrimitivePathNullable} as a {@link UUID} or
	 * <code>null</code>.
	 * 
	 * @return the current element as a {@link UUID} or null if not existing
	 */
	public default UUID getAsUuidOrNull() {
		return this.getAsStringPathNullableUuid().getOrNull();
	}

	/**
	 * Gets the current {@link JsonPrimitivePathNullable} as a {@link Optional} of
	 * {@link UUID}.
	 * 
	 * @return the current element as a {@link Optional} of {@link UUID}
	 */
	public default Optional<UUID> getAsOptionalUuid() {
		return this.getAsStringPathNullableUuid().getOptional();
	}

	/**
	 * Gets the current {@link JsonPrimitivePathNullable} as a
	 * {@link SemanticVersion} or <code>null</code>.
	 * 
	 * @return the current element as a {@link SemanticVersion} or null if not
	 *         existing
	 */
	public default SemanticVersion getAsSemanticVersionOrNull() {
		return this.getAsStringPathNullableSemanticVersion().getOrNull();
	}

	/**
	 * Gets the current {@link JsonPrimitivePathNullable} as a {@link Optional} of
	 * {@link SemanticVersion}.
	 * 
	 * @return the current element as a {@link Optional} of {@link SemanticVersion}
	 */
	public default Optional<SemanticVersion> getAsOptionalSemanticVersion() {
		return this.getAsStringPathNullableSemanticVersion().getOptional();
	}

	/**
	 * Gets the current {@link JsonPrimitivePathNullable} as a {@link Enum} or
	 * <code>null</code>.
	 * 
	 * @param <T>       the type of the enum value
	 * @param enumClass the type class of the enum
	 * @return the current element as a {@link Enum} or null if not existing
	 */
	public default <T extends Enum<T>> T getAsEnumOrNull(Class<T> enumClass) {
		return this.getAsStringPathNullableEnum(enumClass).getOrNull();
	}

	/**
	 * Gets the current {@link JsonPrimitivePathNullable} as a {@link Optional} of
	 * {@link Enum}.
	 * 
	 * @param <T>       the type of the enum value
	 * @param enumClass the type class of the enum
	 * @return the current element as a {@link Optional} of {@link Enum}
	 */
	public default <T extends Enum<T>> Optional<T> getAsOptionalEnum(Class<T> enumClass) {
		return this.getAsStringPathNullableEnum(enumClass).getOptional();
	}

	/**
	 * Gets the current {@link JsonPrimitivePathNullable} as a {@link ZonedDateTime}
	 * or <code>null</code>.
	 * 
	 * @param formatter the {@link DateTimeFormatter} used to parse the string
	 * @return the current element as a {@link Enum} or null if not existing
	 */
	public default ZonedDateTime getAsZonedDateTimeOrNull(DateTimeFormatter formatter) {
		return this.getAsStringPathNullableZonedDateTime(formatter).getOrNull();
	}

	/**
	 * Gets the current {@link JsonPrimitivePathNullable} as a {@link ZonedDateTime}
	 * or <code>null</code>.
	 * 
	 * @return the current element as a {@link ZonedDateTime} or null if not
	 *         existing
	 */
	public default ZonedDateTime getAsZonedDateTimeOrNull() {
		return this.getAsStringPathNullableZonedDateTime().getOrNull();
	}

	/**
	 * Gets the current {@link JsonPrimitivePathNullable} as a {@link Optional} of
	 * {@link ZonedDateTime}.
	 * 
	 * @param formatter the {@link DateTimeFormatter} used to parse the string
	 * @return the current element as a {@link Optional} of {@link ZonedDateTime}
	 */
	public default Optional<ZonedDateTime> getAsOptionalZonedDateTime(DateTimeFormatter formatter) {
		return this.getAsStringPathNullableZonedDateTime(formatter).getOptional();
	}

	/**
	 * Gets the current {@link JsonPrimitivePathNullable} as a {@link Optional} of
	 * {@link ZonedDateTime}.
	 * 
	 * @return the current element as a {@link Optional} of {@link ZonedDateTime}
	 */
	public default Optional<ZonedDateTime> getAsOptionalZonedDateTime() {
		return this.getAsStringPathNullableZonedDateTime().getOptional();
	}

	/**
	 * Gets the current {@link JsonPrimitivePathNullable} as a {@link LocalDate} or
	 * <code>null</code>.
	 * 
	 * @param formatter the {@link DateTimeFormatter} used to parse the string
	 * @return the current element as a {@link LocalDate} or null if not existing
	 */
	public default LocalDate getAsLocalDateOrNull(DateTimeFormatter formatter) {
		return this.getAsStringPathNullableLocalDate(formatter).getOrNull();
	}

	/**
	 * Gets the current {@link JsonPrimitivePathNullable} as a {@link LocalDate} or
	 * <code>null</code>.
	 * 
	 * @return the current element as a {@link LocalDate} or null if not existing
	 */
	public default LocalDate getAsLocalDateOrNull() {
		return this.getAsStringPathNullableLocalDate().getOrNull();
	}

	/**
	 * Gets the current {@link JsonPrimitivePathNullable} as a {@link Optional} of
	 * {@link LocalDate}.
	 * 
	 * @param formatter the {@link DateTimeFormatter} used to parse the string
	 * @return the current element as a {@link Optional} of {@link LocalDate}
	 */
	public default Optional<LocalDate> getAsOptionalLocalDate(DateTimeFormatter formatter) {
		return this.getAsStringPathNullableLocalDate(formatter).getOptional();
	}

	/**
	 * Gets the current {@link JsonPrimitivePathNullable} as a {@link Optional} of
	 * {@link LocalDate}.
	 * 
	 * @return the current element as a {@link Optional} of {@link LocalDate}
	 */
	public default Optional<LocalDate> getAsOptionalLocalDate() {
		return this.getAsStringPathNullableLocalDate().getOptional();
	}

	/**
	 * Gets the current {@link JsonPrimitivePathNullable} as a
	 * {@link NumberPathNullable}.
	 * 
	 * @return the current element as a {@link NumberPathNullable}
	 */
	public NumberPathNullable getAsNumberPathNullable();

	/**
	 * Gets the current value as a double or returns the provided default value if
	 * the current element is not present.
	 * 
	 * @param defaultValue the default value to provide in case the current value is
	 *                     not present
	 * @return the current value as a double or the default value if the current
	 *         value is not present
	 * @see NumberPathNullable#getAsDoubleOrDefault(double)
	 */
	public default double getAsDoubleOrDefault(double defaultValue) {
		return this.getAsNumberPathNullable().getAsDoubleOrDefault(defaultValue);
	}

	/**
	 * Gets the current value as a float or returns the provided default value if
	 * the current element is not present.
	 * 
	 * @param defaultValue the default value to provide in case the current value is
	 *                     not present
	 * @return the current value as a float or the default value if the current
	 *         value is not present
	 * @see NumberPathNullable#getAsFloatOrDefault(double)
	 */
	public default float getAsFloatOrDefault(float defaultValue) {
		return this.getAsNumberPathNullable().getAsFloatOrDefault(defaultValue);
	}

	/**
	 * Gets the current value as a long or returns the provided default value if the
	 * current element is not present.
	 * 
	 * @param defaultValue the default value to provide in case the current value is
	 *                     not present
	 * @return the current value as a long or the default value if the current value
	 *         is not present
	 * @see NumberPathNullable#getAsLongOrDefault(double)
	 */
	public default long getAsLongOrDefault(long defaultValue) {
		return this.getAsNumberPathNullable().getAsLongOrDefault(defaultValue);
	}

	/**
	 * Gets the current value as a integer or returns the provided default value if
	 * the current element is not present.
	 * 
	 * @param defaultValue the default value to provide in case the current value is
	 *                     not present
	 * @return the current value as a integer or the default value if the current
	 *         value is not present
	 * @see NumberPathNullable#getAsIntOrDefault(double)
	 */
	public default int getAsIntOrDefault(int defaultValue) {
		return this.getAsNumberPathNullable().getAsIntOrDefault(defaultValue);
	}

	/**
	 * Gets the current value as a short or returns the provided default value if
	 * the current element is not present.
	 * 
	 * @param defaultValue the default value to provide in case the current value is
	 *                     not present
	 * @return the current value as a short or the default value if the current
	 *         value is not present
	 * @see NumberPathNullable#getAsShortOrDefault(double)
	 */
	public default short getAsShortOrDefault(short defaultValue) {
		return this.getAsNumberPathNullable().getAsShortOrDefault(defaultValue);
	}

	/**
	 * Gets the current value as a byte or returns the provided default value if the
	 * current element is not present.
	 * 
	 * @param defaultValue the default value to provide in case the current value is
	 *                     not present
	 * @return the current value as a byte or the default value if the current value
	 *         is not present
	 * @see NumberPathNullable#getAsByteOrDefault(double)
	 */
	public default byte getAsByteOrDefault(byte defaultValue) {
		return this.getAsNumberPathNullable().getAsByteOrDefault(defaultValue);
	}

	/**
	 * Gets the current value as a {@link Optional} of double.
	 * 
	 * @return the current value as a {@link Optional} of double
	 * @see NumberPathNullable#getAsOptionalDouble()
	 */
	public default Optional<Double> getAsOptionalDouble() {
		return this.getAsNumberPathNullable().getAsOptionalDouble();
	}

	/**
	 * Gets the current value as a {@link Optional} of float.
	 * 
	 * @return the current value as a {@link Optional} of float
	 * @see NumberPathNullable#getAsOptionalFloat()
	 */
	public default Optional<Float> getAsOptionalFloat() {
		return this.getAsNumberPathNullable().getAsOptionalFloat();
	}

	/**
	 * Gets the current value as a {@link Optional} of long.
	 * 
	 * @return the current value as a {@link Optional} of long
	 * @see NumberPathNullable#getAsOptionalLong()
	 */
	public default Optional<Long> getAsOptionalLong() {
		return this.getAsNumberPathNullable().getAsOptionalLong();
	}

	/**
	 * Gets the current value as a {@link Optional} of integer.
	 * 
	 * @return the current value as a {@link Optional} of integer
	 * @see NumberPathNullable#getAsOptionalInt()
	 */
	public default Optional<Integer> getAsOptionalInt() {
		return this.getAsNumberPathNullable().getAsOptionalInt();
	}

	/**
	 * Gets the current value as a {@link Optional} of short.
	 * 
	 * @return the current value as a {@link Optional} of short
	 * @see NumberPathNullable#getAsOptionalShort()
	 */
	public default Optional<Short> getAsOptionalShort() {
		return this.getAsNumberPathNullable().getAsOptionalShort();
	}

	/**
	 * Gets the current value as a {@link Optional} of byte.
	 * 
	 * @return the current value as a {@link Optional} of byte
	 * @see NumberPathNullable#getAsOptionalByte()
	 */
	public default Optional<Byte> getAsOptionalByte() {
		return this.getAsNumberPathNullable().getAsOptionalByte();
	}

	/**
	 * Gets the current {@link JsonPrimitivePathNullable} as a
	 * {@link BooleanPathNullable}.
	 * 
	 * @return the current element as a {@link BooleanPathNullable}
	 */
	public BooleanPathNullable getAsBooleanPathNullable();

	/**
	 * Gets the current {@link JsonPrimitivePathNullable} as a primitive
	 * {@link Boolean} if present otherwise returns the provided default value.
	 * 
	 * @param defaultValue the default value to provide if the current values is not
	 *                     present
	 * @return the current value as a boolean if present; else the default value
	 */
	public default boolean getAsBooleanOrDefault(boolean defaultValue) {
		return this.getAsBooleanPathNullable().getOrDefault(defaultValue);
	}

	/**
	 * Checks if the current value is present.
	 * 
	 * @return true if the current value is present; else false
	 */
	public boolean isPresent();

	/**
	 * Gets the JsonPrimitive value of the current path.
	 * 
	 * @return the value; or null if not present
	 */
	public JsonPrimitive getOrNull();

}
