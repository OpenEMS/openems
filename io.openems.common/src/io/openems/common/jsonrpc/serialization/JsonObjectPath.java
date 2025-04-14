package io.openems.common.jsonrpc.serialization;

import static java.util.stream.Collectors.mapping;

import java.lang.reflect.Array;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Collector;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.openems.common.jsonrpc.serialization.StringPathParser.StringParserString;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.SemanticVersion;

public interface JsonObjectPath extends JsonPath {

	/**
	 * Gets the element associated with the member name from this object.
	 * 
	 * @param member the name of the member
	 * @return the {@link JsonElementPath} of the member value
	 */
	public JsonElementPath getJsonElementPath(String member);

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link JsonElement}.
	 * 
	 * @param member the name of the member
	 * @return the {@link JsonElement} of the member value
	 */
	public default JsonElement getJsonElement(String member) {
		return this.getJsonElementPath(member).get();
	}

	/**
	 * Gets the primitive element associated with the member name from this object.
	 * 
	 * @param member the name of the member
	 * @return the {@link JsonPrimitivePath} of the member value
	 */
	public default JsonPrimitivePath getJsonPrimitivePath(String member) {
		return this.getJsonElementPath(member).getAsJsonPrimitivePath();
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link JsonPrimitive}.
	 * 
	 * @param member the name of the member
	 * @return the {@link JsonPrimitive} of the member value
	 */
	public default JsonPrimitive getJsonPrimitive(String member) {
		return this.getJsonPrimitivePath(member).get();
	}

	/**
	 * Gets the nullable element associated with the member name from this object.
	 * 
	 * @param member the name of the member
	 * @return the {@link JsonElementPathNullable} of the member value
	 */
	public JsonElementPathNullable getNullableJsonElementPath(String member);

	/**
	 * Gets the null-able primitive associated with the member name from this
	 * object.
	 * 
	 * @param member the name of the member
	 * @return the {@link JsonPrimitivePathNullable} of the member value
	 */
	public default JsonPrimitivePathNullable getNullableJsonPrimitivePath(String member) {
		return this.getNullableJsonElementPath(member).getAsJsonPrimitivePathNullable();
	}

	/**
	 * Gets the null-able object associated with the member name from this object.
	 * 
	 * @param member the name of the member
	 * @return the {@link JsonObjectPathNullable} of the member value
	 */
	public default JsonObjectPathNullable getNullableJsonObjectPath(String member) {
		return this.getNullableJsonElementPath(member).getAsJsonObjectPathNullable();
	}

	/**
	 * Collects the current object entries into a result object with the provided
	 * {@link Collector}.
	 * 
	 * @param <S>       the type of the {@link StringPath}
	 * @param <A>       the mutable accumulation type of the reduction operation
	 *                  (often hidden as an implementation detail)
	 * @param <R>       the result type of the reduction operation
	 * @param keyParser the key string parser
	 * @param collector the {@link Collector} to defined the reduction operations
	 * @return the result of the reduction
	 */
	public <S, A, R> R collect(//
			StringParser<S> keyParser, //
			Collector<Entry<StringPath<S>, JsonElementPath>, A, R> collector //
	);

	/**
	 * Collects the current object entries into a result object with the provided
	 * {@link Collector}.
	 * 
	 * @param <A>       the mutable accumulation type of the reduction operation
	 *                  (often hidden as an implementation detail)
	 * @param <R>       the result type of the reduction operation
	 * @param collector the {@link Collector} to defined the reduction operations
	 * @return the result of the reduction
	 */
	public default <A, R> R collectStringKeys(Collector<Entry<String, JsonElementPath>, A, R> collector) {
		return this.collect(new StringParserString(),
				mapping(t -> Map.entry(t.getKey().get(), t.getValue()), collector));
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link StringPath}.
	 * 
	 * @param member the name of the member
	 * @return the {@link StringPath} of the member value
	 */
	public default StringPath<String> getStringPath(String member) {
		return this.getJsonElementPath(member).getAsStringPathString();
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link StringPath}.
	 * 
	 * @param <T>    the type of the result string path
	 * @param member the name of the member
	 * @param parser the parser to use to parse the string value
	 * @return the {@link StringPath} of the member value
	 */
	public default <T> StringPath<T> getStringPath(String member, StringParser<T> parser) {
		return this.getJsonElementPath(member).getAsStringPath(parser);
	}

	/**
	 * Gets the element associate with the member name from this object as a string
	 * parsed to the result object.
	 * 
	 * @param <T>    the type of the result object
	 * @param member the name of the member
	 * @param parser the {@link StringParser} to parse the raw string value
	 * @return the parsed string
	 */
	public default <T> T getStringParsed(String member, StringParser<T> parser) {
		return this.getJsonElementPath(member).getAsStringPath(parser).get();
	}

	/**
	 * Gets the element associate with the member name from this object as a string
	 * parsed to the result object or null if not present.
	 * 
	 * @param <T>    the type of the result object
	 * @param member the name of the member
	 * @param parser the {@link StringParser} to parse the raw string value
	 * @return the parsed string or null if not present
	 */
	public default <T> T getStringParsedOrNull(String member, StringParser<T> parser) {
		return this.getNullableJsonPrimitivePath(member).getAsStringParsedOrNull(parser);
	}

	/**
	 * Gets the element associate with the member name from this object as a
	 * {@link Optional} of the string parsed to the result object or empty if not
	 * present.
	 * 
	 * @param <T>    the type of the result object
	 * @param member the name of the member
	 * @param parser the {@link StringParser} to parse the raw string value
	 * @return the {@link Optional} of the parsed string
	 */
	public default <T> Optional<T> getOptionalStringParsed(String member, StringParser<T> parser) {
		return this.getNullableJsonPrimitivePath(member).getAsOptionalStringParsed(parser);
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link StringPathNullable}.
	 * 
	 * @param member the name of the member
	 * @return the {@link StringPathNullable} of the member value
	 */
	public default StringPathNullable<String> getNullableStringPathString(String member) {
		return this.getNullableJsonElementPath(member).getAsStringPathNullableString();
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link StringPathNullable}.
	 * 
	 * @param member the name of the member
	 * @return the {@link StringPathNullable} of the member value
	 */
	public default StringPathNullable<ChannelAddress> getNullableStringPathChannelAddress(String member) {
		return this.getNullableJsonElementPath(member).getAsStringPathNullableChannelAddress();
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link StringPathNullable}.
	 * 
	 * @param <T>       the type of the enum value
	 * @param member    the name of the member
	 * @param enumClass the type class of the enum
	 * @return the {@link StringPathNullable} of the member value
	 */
	public default <T extends Enum<T>> StringPathNullable<T> getNullableStringPathEnum(//
			String member, //
			Class<T> enumClass //
	) {
		return this.getNullableJsonElementPath(member).getAsStringPathNullableEnum(enumClass);
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link StringPathNullable}.
	 * 
	 * @param member the name of the member
	 * @return the {@link StringPathNullable} of the member value
	 */
	public default StringPathNullable<LocalDate> getNullableStringPathLocalDate(String member) {
		return this.getNullableJsonElementPath(member).getAsStringPathNullableLocalDate();
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link StringPathNullable}.
	 * 
	 * @param member    the name of the member
	 * @param formatter the {@link DateTimeFormatter} used to parse the string
	 * @return the {@link StringPathNullable} of the member value
	 */
	public default StringPathNullable<LocalDate> getNullableStringPathLocalDate(String member,
			DateTimeFormatter formatter) {
		return this.getNullableJsonElementPath(member).getAsStringPathNullableLocalDate(formatter);
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link StringPathNullable}.
	 * 
	 * @param member the name of the member
	 * @return the {@link StringPathNullable} of the member value
	 */
	public default StringPathNullable<SemanticVersion> getNullableStringPathSemanticVersion(String member) {
		return this.getNullableJsonElementPath(member).getAsStringPathNullableSemanticVersion();
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link StringPathNullable}.
	 * 
	 * @param <T>    the actual type of the string value
	 * @param member the name of the member
	 * @param parser the parser to parse the string
	 * @return the {@link StringPathNullable} of the member value
	 */
	public default <T> StringPathNullable<T> getNullableStringPath(String member, StringParser<T> parser) {
		return this.getNullableJsonElementPath(member).getAsStringPathNullable(parser);
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link StringPathNullable}.
	 * 
	 * @param member the name of the member
	 * @return the {@link StringPathNullable} of the member value
	 */
	public default StringPathNullable<UUID> getNullableStringPathUuid(String member) {
		return this.getNullableJsonElementPath(member).getAsStringPathNullableUuid();
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link StringPathNullable}.
	 * 
	 * @param member the name of the member
	 * @return the {@link StringPathNullable} of the member value
	 */
	public default StringPathNullable<ZonedDateTime> getNullableStringPathZonedDateTime(String member) {
		return this.getNullableJsonElementPath(member).getAsStringPathNullableZonedDateTime();
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link StringPathNullable}.
	 * 
	 * @param member    the name of the member
	 * @param formatter the {@link DateTimeFormatter} used to parse the string
	 * @return the {@link StringPathNullable} of the member value
	 */
	public default StringPathNullable<ZonedDateTime> getNullableStringPathZonedDateTime(String member,
			DateTimeFormatter formatter) {
		return this.getNullableJsonElementPath(member).getAsStringPathNullableZonedDateTime(formatter);
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link StringPath} of type {@link UUID}.
	 * 
	 * @param member the name of the member
	 * @return the {@link StringPath} of type {@link UUID} of the member value
	 */
	public default StringPath<UUID> getStringPathUuid(String member) {
		return this.getJsonElementPath(member).getAsStringPathUuid();
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link StringPath} of type {@link SemanticVersion}.
	 * 
	 * @param member the name of the member
	 * @return the {@link StringPath} of type {@link SemanticVersion} of the member
	 *         value
	 */
	public default StringPath<SemanticVersion> getStringPathSemanticVersion(String member) {
		return this.getJsonElementPath(member).getAsStringPathSemanticVersion();
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link StringPath} of type {@link Enum}.
	 * 
	 * @param <T>       the type of the {@link Enum} value
	 * @param member    the name of the member
	 * @param enumClass the class type of the {@link Enum}
	 * @return the {@link StringPath} of type {@link Enum} of the member value
	 */
	public default <T extends Enum<T>> StringPath<T> getStringPathEnum(String member, Class<T> enumClass) {
		return this.getJsonElementPath(member).getAsStringPathEnum(enumClass);
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link StringPath} of type {@link ZonedDateTime}.
	 * 
	 * @param member    the name of the member
	 * @param formatter the {@link DateTimeFormatter} to use to parse the string
	 * @return the {@link StringPath} of type {@link ZonedDateTime} of the member
	 *         value
	 */
	public default StringPath<ZonedDateTime> getStringPathZonedDateTime(String member, DateTimeFormatter formatter) {
		return this.getJsonElementPath(member).getAsStringPathZonedDateTime(formatter);
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link StringPath} of type {@link ZonedDateTime}.
	 * 
	 * @param member the name of the member
	 * @return the {@link StringPath} of type {@link ZonedDateTime} of the member
	 *         value
	 */
	public default StringPath<ZonedDateTime> getStringPathZonedDateTime(String member) {
		return this.getJsonElementPath(member).getAsStringPathZonedDateTime();
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link StringPath} of type {@link LocalDate}.
	 * 
	 * @param member    the name of the member
	 * @param formatter the {@link DateTimeFormatter} to use to parse the string
	 * @return the {@link StringPath} of type {@link LocalDate} of the member value
	 */
	public default StringPath<LocalDate> getStringPathLocalDate(String member, DateTimeFormatter formatter) {
		return this.getJsonElementPath(member).getAsStringPathLocalDate(formatter);
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link StringPath} of type {@link LocalDate}.
	 * 
	 * @param member the name of the member
	 * @return the {@link StringPath} of type {@link LocalDate} of the member value
	 */
	public default StringPath<LocalDate> getStringPathLocalDate(String member) {
		return this.getJsonElementPath(member).getAsStringPathLocalDate();
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link NumberPath}.
	 * 
	 * @param member the name of the member
	 * @return the {@link NumberPath} of the member value
	 */
	public default NumberPath getNumberPath(String member) {
		return this.getJsonElementPath(member).getAsNumberPath();
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link NumberPathNullable}.
	 * 
	 * @param member the name of the member
	 * @return the {@link NumberPathNullable} of the member value
	 */
	public default NumberPathNullable getNullableNumberPath(String member) {
		return this.getNullableJsonElementPath(member).getAsNumberPathNullable();
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link String}.
	 * 
	 * @param member the name of the member
	 * @return the {@link String} of the member value
	 */
	public default String getString(String member) {
		return this.getJsonElementPath(member).getAsString();
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link String} or null if not present.
	 * 
	 * @param member the name of the member
	 * @return the {@link String} of the member value or null if not present
	 */
	public default String getStringOrNull(String member) {
		return this.getNullableStringPathString(member).getOrNull();
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link Optional} of type {@link String}.
	 * 
	 * @param member the name of the member
	 * @return the {@link Optional} of type {@link String} of the member value
	 */
	public default Optional<String> getOptionalString(String member) {
		return this.getNullableStringPathString(member).getOptional();
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link UUID}.
	 * 
	 * @param member the name of the member
	 * @return the {@link UUID} of the member value
	 */
	public default UUID getUuid(String member) {
		return this.getStringPathUuid(member).get();
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link UUID} or null if not present.
	 * 
	 * @param member the name of the member
	 * @return the {@link UUID} of the member value or null if not present
	 */
	public default UUID getUuidOrNull(String member) {
		return this.getNullableStringPathUuid(member).getOrNull();
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link Optional} of type {@link UUID}.
	 * 
	 * @param member the name of the member
	 * @return the {@link Optional} of type {@link UUID} of the member value
	 */
	public default Optional<UUID> getOptionalUuid(String member) {
		return this.getNullableStringPathUuid(member).getOptional();
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link SemanticVersion}.
	 * 
	 * @param member the name of the member
	 * @return the {@link SemanticVersion} of the member value
	 */
	public default SemanticVersion getSemanticVersion(String member) {
		return this.getStringPathSemanticVersion(member).get();
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link SemanticVersion} or null if not present.
	 * 
	 * @param member the name of the member
	 * @return the {@link SemanticVersion} of the member value or null if not
	 *         present
	 */
	public default SemanticVersion getSemanticVersionOrNull(String member) {
		return this.getNullableStringPathSemanticVersion(member).getOrNull();
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link Optional} of type {@link SemanticVersion}.
	 * 
	 * @param member the name of the member
	 * @return the {@link Optional} of type {@link SemanticVersion} of the member
	 *         value
	 */
	public default Optional<SemanticVersion> getOptionalSemanticVersion(String member) {
		return this.getNullableStringPathSemanticVersion(member).getOptional();
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link Enum}.
	 * 
	 * @param <T>       the type of the {@link Enum} value
	 * @param member    the name of the member
	 * @param enumClass the class type of the {@link Enum}
	 * @return the {@link Enum} of the member value
	 */
	public default <T extends Enum<T>> T getEnum(String member, Class<T> enumClass) {
		return this.getStringPathEnum(member, enumClass).get();
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link Enum} or null if not present.
	 * 
	 * @param <T>       the type of the enum value
	 * @param member    the name of the member
	 * @param enumClass the type class of the enum
	 * @return the {@link Enum} of the member value or null if not present
	 */
	public default <T extends Enum<T>> T getEnumOrNull(String member, Class<T> enumClass) {
		return this.getNullableStringPathEnum(member, enumClass).getOrNull();
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link Optional} of type {@link Enum}.
	 * 
	 * @param <T>       the type of the enum value
	 * @param member    the name of the member
	 * @param enumClass the type class of the enum
	 * @return the {@link Optional} of type {@link Enum} of the member value
	 */
	public default <T extends Enum<T>> Optional<T> getOptionalEnum(String member, Class<T> enumClass) {
		return this.getNullableStringPathEnum(member, enumClass).getOptional();
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link ZonedDateTime}.
	 * 
	 * @param member the name of the member
	 * @return the {@link ZonedDateTime} of the member value
	 */
	public default ZonedDateTime getZonedDateTime(String member) {
		return this.getStringPathZonedDateTime(member).get();
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link ZonedDateTime}.
	 * 
	 * @param member    the name of the member
	 * @param formatter the {@link DateTimeFormatter} to use to parse the string
	 * @return the {@link ZonedDateTime} of the member value
	 */
	public default ZonedDateTime getZonedDateTime(String member, DateTimeFormatter formatter) {
		return this.getStringPathZonedDateTime(member, formatter).get();
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link ZonedDateTime} or null if not present.
	 * 
	 * @param member the name of the member
	 * @return the {@link ZonedDateTime} of the member value or null if not present
	 */
	public default ZonedDateTime getZonedDateTimeOrNull(String member) {
		return this.getNullableStringPathZonedDateTime(member).getOrNull();
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link ZonedDateTime} or null if not present.
	 * 
	 * @param member    the name of the member
	 * @param formatter the {@link DateTimeFormatter} used to parse the string
	 * @return the {@link ZonedDateTime} of the member value or null if not present
	 */
	public default ZonedDateTime getZonedDateTimeOrNull(String member, DateTimeFormatter formatter) {
		return this.getNullableStringPathZonedDateTime(member, formatter).getOrNull();
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link Optional} of type {@link String}.
	 * 
	 * @param member the name of the member
	 * @return the {@link Optional} of type {@link String} of the member value
	 */
	public default Optional<ZonedDateTime> getOptionalZonedDateTime(String member) {
		return this.getNullableStringPathZonedDateTime(member).getOptional();
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link Optional} of type {@link String}.
	 * 
	 * @param member    the name of the member
	 * @param formatter the {@link DateTimeFormatter} used to parse the string
	 * @return the {@link Optional} of type {@link String} of the member value
	 */
	public default Optional<ZonedDateTime> getOptionalZonedDateTime(String member, DateTimeFormatter formatter) {
		return this.getNullableStringPathZonedDateTime(member, formatter).getOptional();
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link LocalDate}.
	 * 
	 * @param member the name of the member
	 * @return the {@link LocalDate} of the member value
	 */
	public default LocalDate getLocalDate(String member) {
		return this.getStringPathLocalDate(member).get();
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link LocalDate}.
	 * 
	 * @param member    the name of the member
	 * @param formatter the {@link DateTimeFormatter} to use to parse the string
	 * @return the {@link LocalDate} of the member value
	 */
	public default LocalDate getLocalDate(String member, DateTimeFormatter formatter) {
		return this.getStringPathLocalDate(member, formatter).get();
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link LocalDate} or null if not present.
	 * 
	 * @param member the name of the member
	 * @return the {@link LocalDate} of the member value or null if not present
	 */
	public default LocalDate getLocalDateOrNull(String member) {
		return this.getNullableStringPathLocalDate(member).getOrNull();
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link LocalDate} or null if not present.
	 * 
	 * @param member    the name of the member
	 * @param formatter the {@link DateTimeFormatter} used to parse the string
	 * @return the {@link LocalDate} of the member value or null if not present
	 */
	public default LocalDate getLocalDateOrNull(String member, DateTimeFormatter formatter) {
		return this.getNullableStringPathLocalDate(member, formatter).getOrNull();
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link Optional} of type {@link LocalDate}.
	 * 
	 * @param member the name of the member
	 * @return the {@link Optional} of type {@link LocalDate} of the member value
	 */
	public default Optional<LocalDate> getOptionalLocalDate(String member) {
		return this.getNullableStringPathLocalDate(member).getOptional();
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link Optional} of type {@link LocalDate}.
	 * 
	 * @param member    the name of the member
	 * @param formatter the {@link DateTimeFormatter} used to parse the string
	 * @return the {@link Optional} of type {@link LocalDate} of the member value
	 */
	public default Optional<LocalDate> getOptionalLocalDate(String member, DateTimeFormatter formatter) {
		return this.getNullableStringPathLocalDate(member, formatter).getOptional();
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * primitive {@link Double}.
	 * 
	 * @param member the name of the member
	 * @return the primitive {@link Double} of the member value
	 */
	public default double getDouble(String member) {
		return this.getNumberPath(member).getAsDouble();
	}

	/**
	 * Gets the element associated with the member name as a double or returns the
	 * provided default value if the current element is not present.
	 * 
	 * @param member       the name of the member
	 * @param defaultValue the default value to provide in case the current value is
	 *                     not present
	 * @return the primitive {@link Double} of the member value or the default value
	 *         if absent
	 */
	public default double getDoubleOrDefault(String member, double defaultValue) {
		return this.getNullableNumberPath(member).getAsDoubleOrDefault(defaultValue);
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link Optional} of double.
	 * 
	 * @param member the name of the member
	 * @return the {@link Optional} of the member value
	 */
	public default Optional<Double> getOptionalDouble(String member) {
		return this.getNullableNumberPath(member).getAsOptionalDouble();
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * primitive {@link Float}.
	 * 
	 * @param member the name of the member
	 * @return the primitive {@link Float} of the member value
	 */
	public default float getFloat(String member) {
		return this.getNumberPath(member).getAsFloat();
	}

	/**
	 * Gets the element associated with the member name as a float or returns the
	 * provided default value if the current element is not present.
	 * 
	 * @param member       the name of the member
	 * @param defaultValue the default value to provide in case the current value is
	 *                     not present
	 * @return the primitive {@link Float} of the member value or the default value
	 *         if absent
	 */
	public default float getFloatOrDefault(String member, float defaultValue) {
		return this.getNullableNumberPath(member).getAsFloatOrDefault(defaultValue);
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link Optional} of float.
	 * 
	 * @param member the name of the member
	 * @return the {@link Optional} of the member value
	 */
	public default Optional<Float> getOptionalFloat(String member) {
		return this.getNullableNumberPath(member).getAsOptionalFloat();
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * primitive {@link Long}.
	 * 
	 * @param member the name of the member
	 * @return the primitive {@link Long} of the member value
	 */
	public default long getLong(String member) {
		return this.getNumberPath(member).getAsLong();
	}

	/**
	 * Gets the element associated with the member name as a long or returns the
	 * provided default value if the current element is not present.
	 * 
	 * @param member       the name of the member
	 * @param defaultValue the default value to provide in case the current value is
	 *                     not present
	 * @return the primitive {@link Long} of the member value or the default value
	 *         if absent
	 */
	public default long getLongOrDefault(String member, long defaultValue) {
		return this.getNullableNumberPath(member).getAsLongOrDefault(defaultValue);
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link Optional} of long.
	 * 
	 * @param member the name of the member
	 * @return the {@link Optional} of the member value
	 */
	public default Optional<Long> getOptionalLong(String member) {
		return this.getNullableNumberPath(member).getAsOptionalLong();
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * primitive {@link Integer}.
	 * 
	 * @param member the name of the member
	 * @return the primitive {@link Integer} of the member value
	 */
	public default int getInt(String member) {
		return this.getNumberPath(member).getAsInt();
	}

	/**
	 * Gets the element associated with the member name as a integer or returns the
	 * provided default value if the current element is not present.
	 * 
	 * @param member       the name of the member
	 * @param defaultValue the default value to provide in case the current value is
	 *                     not present
	 * @return the primitive {@link Integer} of the member value or the default
	 *         value if absent
	 */
	public default int getIntOrDefault(String member, int defaultValue) {
		return this.getNullableNumberPath(member).getAsIntOrDefault(defaultValue);
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link Optional} of integer.
	 * 
	 * @param member the name of the member
	 * @return the {@link Optional} of the member value
	 */
	public default Optional<Integer> getOptionalInt(String member) {
		return this.getNullableNumberPath(member).getAsOptionalInt();
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * primitive {@link Short}.
	 * 
	 * @param member the name of the member
	 * @return the primitive {@link Short} of the member value
	 */
	public default short getShort(String member) {
		return this.getNumberPath(member).getAsShort();
	}

	/**
	 * Gets the element associated with the member name as a short or returns the
	 * provided default value if the current element is not present.
	 * 
	 * @param member       the name of the member
	 * @param defaultValue the default value to provide in case the current value is
	 *                     not present
	 * @return the primitive {@link Short} of the member value or the default value
	 *         if absent
	 */
	public default short getShortOrDefault(String member, short defaultValue) {
		return this.getNullableNumberPath(member).getAsShortOrDefault(defaultValue);
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link Optional} of short.
	 * 
	 * @param member the name of the member
	 * @return the {@link Optional} of the member value
	 */
	public default Optional<Short> getOptionalShort(String member) {
		return this.getNullableNumberPath(member).getAsOptionalShort();
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * primitive {@link Byte}.
	 * 
	 * @param member the name of the member
	 * @return the primitive {@link Byte} of the member value
	 */
	public default byte getByte(String member) {
		return this.getNumberPath(member).getAsByte();
	}

	/**
	 * Gets the element associated with the member name as a byte or returns the
	 * provided default value if the current element is not present.
	 * 
	 * @param member       the name of the member
	 * @param defaultValue the default value to provide in case the current value is
	 *                     not present
	 * @return the primitive {@link Byte} of the member value or the default value
	 *         if absent
	 */
	public default byte getByteOrDefault(String member, byte defaultValue) {
		return this.getNullableNumberPath(member).getAsByteOrDefault(defaultValue);
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link Optional} of byte.
	 * 
	 * @param member the name of the member
	 * @return the {@link Optional} of the member value
	 */
	public default Optional<Byte> getOptionalByte(String member) {
		return this.getNullableNumberPath(member).getAsOptionalByte();
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link BooleanPath}.
	 * 
	 * @param member the name of the member
	 * @return the {@link BooleanPath} of the member value
	 */
	public default BooleanPath getBooleanPath(String member) {
		return this.getJsonElementPath(member).getAsBooleanPath();
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * primitive {@link Boolean}.
	 * 
	 * @param member the name of the member
	 * @return the primitive {@link Boolean} of the member value
	 */
	public default boolean getBoolean(String member) {
		return this.getBooleanPath(member).get();
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link BooleanPathNullable}.
	 * 
	 * @param member the name of the member
	 * @return the {@link BooleanPathNullable} of the member value
	 */
	public default BooleanPathNullable getBooleanPathNullable(String member) {
		return this.getNullableJsonElementPath(member).getAsBooleanPathNullable();
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link Boolean}.
	 * 
	 * @param member the name of the member
	 * @return the {@link Boolean} of the member value or null if not present
	 */
	public default Boolean getBooleanNullable(String member) {
		return this.getBooleanPathNullable(member).getOrNull();
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link Optional} of type {@link Boolean}.
	 * 
	 * @param member the name of the member
	 * @return the {@link Optional} of type {@link Boolean} of the member value
	 */
	public default Optional<Boolean> getOptionalBoolean(String member) {
		return this.getBooleanPathNullable(member).getOptional();
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link JsonObjectPath}.
	 * 
	 * @param member the name of the member
	 * @return the {@link JsonObjectPath} of the member value
	 */
	public default JsonObjectPath getJsonObjectPath(String member) {
		return this.getJsonElementPath(member).getAsJsonObjectPath();
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link JsonObject}.
	 * 
	 * @param member the name of the member
	 * @return the {@link JsonObject} of the member value
	 */
	public default JsonObject getJsonObject(String member) {
		return this.getJsonObjectPath(member).get();
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link JsonArrayPath}.
	 * 
	 * @param member the name of the member
	 * @return the {@link JsonArrayPath} of the member value
	 */
	public default JsonArrayPath getJsonArrayPath(String member) {
		return this.getJsonElementPath(member).getAsJsonArrayPath();
	}

	/**
	 * Gets the nullable array element associated with the member name from this
	 * object.
	 * 
	 * @param member the name of the member
	 * @return the {@link JsonArrayPathNullable} of the member value
	 */
	public default JsonArrayPathNullable getNullableJsonArrayPath(String member) {
		return this.getNullableJsonElementPath(member).getAsJsonArrayPathNullable();
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link JsonArray}.
	 * 
	 * @param member the name of the member
	 * @return the {@link JsonArray} of the member value
	 */
	public default JsonArray getJsonArray(String member) {
		return this.getJsonArrayPath(member).get();
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link JsonArray} or null if not present.
	 * 
	 * @param member the name of the member
	 * @return the {@link JsonArray} of the member value or null if not present
	 */
	public default JsonArray getJsonArrayOrNull(String member) {
		return this.getNullableJsonArrayPath(member).getOrNull();
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link List}.
	 * 
	 * @param <T>    the type of the elements in the list
	 * @param member the name of the member
	 * @param mapper the mapper to deserialize the elements
	 * @return the {@link List} of the member value
	 */
	public default <T> List<T> getList(String member, Function<JsonElementPath, T> mapper) {
		return this.getJsonArrayPath(member).getAsList(mapper);
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link List}.
	 * 
	 * @param <T>        the type of the elements in the list
	 * @param member     the name of the member
	 * @param serializer the {@link JsonSerializer} to deserialize the elements
	 * @return the {@link List} of the member value
	 */
	public default <T> List<T> getList(String member, JsonSerializer<T> serializer) {
		return this.getJsonArrayPath(member).getAsList(serializer);
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link Optional} of {@link List}.
	 * 
	 * @param <T>    the type of the elements in the list
	 * @param member the name of the member
	 * @param mapper the mapper to deserialize the elements
	 * @return the {@link Optional} of {@link List} of the member value or
	 *         {@link Optional#empty()} if not present
	 */
	public default <T> Optional<List<T>> getOptionalList(String member, Function<JsonElementPath, T> mapper) {
		return this.getNullableJsonArrayPath(member).getAsOptionalList(mapper);
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link Optional} of {@link List}.
	 * 
	 * @param <T>        the type of the elements in the list
	 * @param member     the name of the member
	 * @param serializer the {@link JsonSerializer} to deserialize the elements
	 * @return the {@link Optional} of {@link List} of the member value or
	 *         {@link Optional#empty()} if not present
	 */
	public default <T> Optional<List<T>> getOptionalList(String member, JsonSerializer<T> serializer) {
		return this.getNullableJsonArrayPath(member).getAsOptionalList(serializer);
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link Array}.
	 * 
	 * @param <T>       the type of the elements in the array
	 * @param member    the name of the member
	 * @param generator a function which produces a new array of the desired type
	 *                  and the provided length
	 * @param mapper    the mapper to deserialize the elements
	 * @return the {@link Array} of the member value
	 */
	public default <T> T[] getArray(String member, IntFunction<T[]> generator, Function<JsonElementPath, T> mapper) {
		return this.getJsonArrayPath(member).getAsArray(generator, mapper);
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link Array}.
	 * 
	 * @param <T>        the type of the elements in the array
	 * @param member     the name of the member
	 * @param generator  a function which produces a new array of the desired type
	 *                   and the provided length
	 * @param serializer the {@link JsonSerializer} to deserialize the elements
	 * @return the {@link Array} of the member value
	 */
	public default <T> T[] getArray(String member, IntFunction<T[]> generator, JsonSerializer<T> serializer) {
		return this.getJsonArrayPath(member).getAsArray(generator, serializer);
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link Optional} of {@link Array}.
	 * 
	 * @param <T>       the type of the elements in the array
	 * @param member    the name of the member
	 * @param generator a function which produces a new array of the desired type
	 *                  and the provided length
	 * @param mapper    the mapper to deserialize the elements
	 * @return the {@link Optional} of {@link Array} of the member value or
	 *         {@link Optional#empty()} if not present
	 */
	public default <T> Optional<T[]> getOptionalArray(String member, IntFunction<T[]> generator,
			Function<JsonElementPath, T> mapper) {
		return this.getNullableJsonArrayPath(member).getAsOptionalArray(generator, mapper);
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link Optional} of {@link Array}.
	 * 
	 * @param <T>        the type of the elements in the array
	 * @param member     the name of the member
	 * @param generator  a function which produces a new array of the desired type
	 *                   and the provided length
	 * @param serializer the {@link JsonSerializer} to deserialize the elements
	 * @return the {@link Optional} of {@link Array} of the member value or
	 *         {@link Optional#empty()} if not present
	 */
	public default <T> Optional<T[]> getOptionalArray(String member, IntFunction<T[]> generator,
			JsonSerializer<T> serializer) {
		return this.getNullableJsonArrayPath(member).getAsOptionalArray(generator, serializer);
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link Set}.
	 * 
	 * @param <T>    the type of the elements
	 * @param member the name of the member
	 * @param mapper the mapper to convert the elements
	 * @return the result {@link Set} containing all {@link JsonElement
	 *         JsonElements} converted by the provided mapper
	 */
	public default <T> Set<T> getSet(String member, Function<JsonElementPath, T> mapper) {
		return this.getJsonArrayPath(member).getAsSet(mapper);
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link Set}.
	 * 
	 * @param <T>        the type of the elements
	 * @param member     the name of the member
	 * @param serializer the {@link JsonSerializer} to deserialize the elements
	 * @return the {@link Set} of the member value
	 */
	public default <T> Set<T> getSet(String member, JsonSerializer<T> serializer) {
		return this.getJsonArrayPath(member).getAsSet(serializer::deserializePath);
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link Optional} of {@link Set}.
	 * 
	 * @param <T>    the type of the elements in the set
	 * @param member the name of the member
	 * @param mapper the mapper to deserialize the elements
	 * @return the {@link Optional} of {@link Set} of the member value or
	 *         {@link Optional#empty()} if not present
	 */
	public default <T> Optional<Set<T>> getOptionalSet(String member, Function<JsonElementPath, T> mapper) {
		return this.getNullableJsonArrayPath(member).getAsOptionalSet(mapper);
	}

	/**
	 * Gets the element associated with the member name from this object as a
	 * {@link Optional} of {@link Set}.
	 * 
	 * @param <T>        the type of the elements in the set
	 * @param member     the name of the member
	 * @param serializer the {@link JsonSerializer} to deserialize the elements
	 * @return the {@link Optional} of {@link Set} of the member value or
	 *         {@link Optional#empty()} if not present
	 */
	public default <T> Optional<Set<T>> getOptionalSet(String member, JsonSerializer<T> serializer) {
		return this.getNullableJsonArrayPath(member).getAsOptionalSet(serializer);
	}

	/**
	 * Gets the element associated with the member name from this object as the
	 * generic object.
	 * 
	 * @param <T>        the type of the element
	 * @param member     the name of the member
	 * @param serializer the {@link JsonSerializer} to deserialize the element
	 * @return the object of the member value
	 */
	public default <T> T getObject(String member, JsonSerializer<T> serializer) {
		return this.getJsonElementPath(member).getAsObject(serializer);
	}

	/**
	 * Gets the element associated with the member name from this object as the
	 * generic object or null if not present.
	 * 
	 * @param <T>        the type of the element
	 * @param member     the name of the member
	 * @param serializer the {@link JsonSerializer} to deserialize the element
	 * @return the object of the member value or null if the member is not present
	 */
	public default <T> T getObjectOrNull(String member, JsonSerializer<T> serializer) {
		return this.getNullableJsonElementPath(member).getAsObjectOrNull(serializer);
	}

	/**
	 * Gets the current element of the path.
	 * 
	 * @return the {@link JsonObject}
	 */
	public JsonObject get();

}