package io.openems.common.jsonrpc.serialization;

import static java.util.stream.Collectors.toMap;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.openems.common.types.ChannelAddress;
import io.openems.common.types.SemanticVersion;

public interface JsonElementPath extends JsonPath {

	/**
	 * Gets the current element of the path.
	 * 
	 * @return the {@link JsonElement}
	 */
	public JsonElement get();

	/**
	 * Gets the current {@link JsonElementPath} as a {@link JsonObjectPath}.
	 * 
	 * @return the current element as a {@link JsonObjectPath}
	 */
	public JsonObjectPath getAsJsonObjectPath();

	/**
	 * Gets the current {@link JsonElementPath} as a {@link JsonArrayPath}.
	 * 
	 * @return the current element as a {@link JsonArrayPath}
	 */
	public JsonArrayPath getAsJsonArrayPath();

	/**
	 * Gets the current {@link JsonElementPath} as a {@link JsonPrimitivePath}.
	 * 
	 * @return the current element as a {@link JsonPrimitivePath}
	 */
	public JsonPrimitivePath getAsJsonPrimitivePath();

	/**
	 * Gets the current {@link JsonElementPath} as a {@link StringPath}.
	 * 
	 * @param <T>    the actual type of the string value
	 * @param parser the parser to parse the string
	 * @return the current element as a {@link StringPath}
	 */
	public default <T> StringPath<T> getAsStringPath(StringParser<T> parser) {
		return this.getAsJsonPrimitivePath().getAsStringPath(parser);
	}

	/**
	 * Gets the current {@link JsonElementPath} as a {@link StringPath} containing
	 * its raw string.
	 * 
	 * @return the current element as a {@link StringPath}
	 */
	public default StringPath<String> getAsStringPathString() {
		return this.getAsJsonPrimitivePath().getAsStringPathString();
	}

	/**
	 * Gets the current {@link JsonElementPath} as a {@link StringPath} containing a
	 * {@link ChannelAddress}.
	 * 
	 * @return the current element as a {@link StringPath}
	 */
	public default StringPath<ChannelAddress> getAsStringPathChannelAddress() {
		return this.getAsJsonPrimitivePath().getAsStringPathChannelAddress();
	}

	/**
	 * Gets the current {@link JsonElementPath} as a {@link StringPath} containing a
	 * {@link UUID}.
	 * 
	 * @return the current element as a {@link StringPath}
	 */
	public default StringPath<UUID> getAsStringPathUuid() {
		return this.getAsJsonPrimitivePath().getAsStringPathUuid();
	}

	/**
	 * Gets the current {@link JsonElementPath} as a {@link StringPath} containing a
	 * {@link SemanticVersion}.
	 * 
	 * @return the current element as a {@link StringPath}
	 */
	public default StringPath<SemanticVersion> getAsStringPathSemanticVersion() {
		return this.getAsJsonPrimitivePath().getAsStringPathSemanticVersion();
	}

	/**
	 * Gets the current {@link JsonElementPath} as a {@link StringPath} containing a
	 * {@link Enum}.
	 * 
	 * @param <T>       the type of the {@link Enum} value
	 * @param enumClass the class type of the {@link Enum}
	 * @return the current element as a {@link StringPath}
	 */
	public default <T extends Enum<T>> StringPath<T> getAsStringPathEnum(Class<T> enumClass) {
		return this.getAsJsonPrimitivePath().getAsStringPathEnum(enumClass);
	}

	/**
	 * Gets the current {@link JsonElementPath} as a {@link StringPath} containing a
	 * {@link ZonedDateTime}.
	 * 
	 * @param formatter the {@link DateTimeFormatter} to use to parse the string
	 * @return the current element as a {@link StringPath}
	 */
	public default StringPath<ZonedDateTime> getAsStringPathZonedDateTime(DateTimeFormatter formatter) {
		return this.getAsJsonPrimitivePath().getAsStringPathZonedDateTime(formatter);
	}

	/**
	 * Gets the current {@link JsonElementPath} as a {@link StringPath} containing a
	 * {@link ZonedDateTime}.
	 * 
	 * @return the current element as a {@link StringPath}
	 */
	public default StringPath<ZonedDateTime> getAsStringPathZonedDateTime() {
		return this.getAsJsonPrimitivePath().getAsStringPathZonedDateTime();
	}

	/**
	 * Gets the current {@link JsonElementPath} as a {@link StringPath} containing a
	 * {@link LocalDate}.
	 * 
	 * @param formatter the {@link DateTimeFormatter} to use to parse the string
	 * @return the current element as a {@link StringPath}
	 */
	public default StringPath<LocalDate> getAsStringPathLocalDate(DateTimeFormatter formatter) {
		return this.getAsJsonPrimitivePath().getAsStringPathLocalDate(formatter);
	}

	/**
	 * Gets the current {@link JsonElementPath} as a {@link StringPath} containing a
	 * {@link LocalDate}.
	 * 
	 * @return the current element as a {@link StringPath}
	 */
	public default StringPath<LocalDate> getAsStringPathLocalDate() {
		return this.getAsJsonPrimitivePath().getAsStringPathLocalDate();
	}

	/**
	 * Gets the current {@link JsonElementPath} as a {@link String}.
	 * 
	 * @return the current element as a {@link String}
	 */
	public default String getAsString() {
		return this.getAsStringPathString().get();
	}

	/**
	 * Gets the current {@link JsonElementPath} as a parsed {@link String} with the
	 * {@link StringParser}.
	 * 
	 * @param <T>    the actual type of the string value
	 * @param parser the parser to parse the string
	 * @return the current string parsed to the expected element
	 */
	public default <T> T getAsStringParsed(StringParser<T> parser) {
		return this.getAsStringPath(parser).get();
	}

	/**
	 * Gets the current {@link JsonElementPath} as a {@link ChannelAddress}.
	 * 
	 * @return the current element as a {@link ChannelAddress}
	 */
	public default ChannelAddress getAsChannelAddress() {
		return this.getAsStringPathChannelAddress().get();
	}

	/**
	 * Gets the current {@link JsonElementPath} as a {@link UUID}.
	 * 
	 * @return the current element as a {@link UUID}
	 */
	public default UUID getAsUuid() {
		return this.getAsStringPathUuid().get();
	}

	/**
	 * Gets the current {@link JsonElementPath} as a {@link SemanticVersion}.
	 * 
	 * @return the current element as a {@link SemanticVersion}
	 */
	public default SemanticVersion getAsSemanticVersion() {
		return this.getAsStringPathSemanticVersion().get();
	}

	/**
	 * Gets the current {@link JsonElementPath} as a {@link Enum}.
	 * 
	 * @param <T>       the type of the {@link Enum}
	 * @param enumClass the class type of the {@link Enum}
	 * @return the current element as a {@link Enum}
	 */
	public default <T extends Enum<T>> T getAsEnum(Class<T> enumClass) {
		return this.getAsStringPathEnum(enumClass).get();
	}

	/**
	 * Gets the current {@link JsonElementPath} as a {@link ZonedDateTime}.
	 * 
	 * @param formatter the {@link DateTimeFormatter} to use for parsing
	 * @return the current element as a {@link ZonedDateTime}
	 */
	public default ZonedDateTime getAsZonedDateTime(DateTimeFormatter formatter) {
		return this.getAsStringPathZonedDateTime(formatter).get();
	}

	/**
	 * Gets the current {@link JsonElementPath} as a {@link ZonedDateTime}.
	 * 
	 * @return the current element as a {@link ZonedDateTime}
	 */
	public default ZonedDateTime getAsZonedDateTime() {
		return this.getAsStringPathZonedDateTime().get();
	}

	/**
	 * Gets the current {@link JsonElementPath} as a {@link LocalDate}.
	 * 
	 * @param formatter the {@link DateTimeFormatter} to use for parsing
	 * @return the current element as a {@link LocalDate}
	 */
	public default LocalDate getAsLocalDate(DateTimeFormatter formatter) {
		return this.getAsStringPathLocalDate(formatter).get();
	}

	/**
	 * Gets the current {@link JsonElementPath} as a {@link LocalDate}.
	 * 
	 * @return the current element as a {@link LocalDate}
	 */
	public default LocalDate getAsLocalDate() {
		return this.getAsStringPathLocalDate().get();
	}

	/**
	 * Gets the current {@link JsonElementPath} as a {@link NumberPath}.
	 * 
	 * @return the current element as a {@link NumberPath}
	 */
	public default NumberPath getAsNumberPath() {
		return this.getAsJsonPrimitivePath().getAsNumberPath();
	}

	/**
	 * Gets the current {@link JsonPrimitivePath} as a {@link Number}.
	 * 
	 * @return the current element as a {@link Number}
	 */
	public default Number getAsNumber() {
		return this.getAsNumberPath().get();
	}

	/**
	 * Gets the current {@link JsonPrimitivePath} as a double.
	 * 
	 * @return the current element as a double
	 */
	public default double getAsDouble() {
		return this.getAsNumberPath().getAsDouble();
	}

	/**
	 * Gets the current {@link JsonPrimitivePath} as a float.
	 * 
	 * @return the current element as a float
	 */
	public default float getAsFloat() {
		return this.getAsNumberPath().getAsFloat();
	}

	/**
	 * Gets the current {@link JsonPrimitivePath} as a long.
	 * 
	 * @return the current element as a long
	 */
	public default long getAsLong() {
		return this.getAsNumberPath().getAsLong();
	}

	/**
	 * Gets the current {@link JsonPrimitivePath} as a integer.
	 * 
	 * @return the current element as a integer
	 */
	public default int getAsInt() {
		return this.getAsNumberPath().getAsInt();
	}

	/**
	 * Gets the current {@link JsonPrimitivePath} as a short.
	 * 
	 * @return the current element as a short
	 */
	public default short getAsShort() {
		return this.getAsNumberPath().getAsShort();
	}

	/**
	 * Gets the current {@link JsonPrimitivePath} as a byte.
	 * 
	 * @return the current element as a byte
	 */
	public default byte getAsByte() {
		return this.getAsNumberPath().getAsByte();
	}

	/**
	 * Gets the current {@link JsonElementPath} as a {@link BooleanPath}.
	 * 
	 * @return the current element as a {@link BooleanPath}
	 */
	public default BooleanPath getAsBooleanPath() {
		return this.getAsJsonPrimitivePath().getAsBooleanPath();
	}

	/**
	 * Gets the current {@link JsonElementPath} as a primitive {@link Boolean}.
	 * 
	 * @return the current element as a primitive {@link Boolean}
	 */
	public default boolean getAsBoolean() {
		return this.getAsJsonPrimitivePath().getAsBoolean();
	}

	/**
	 * Gets the current {@link JsonElementPath} as a Object serialized with the
	 * provided {@link JsonSerializer}.
	 * 
	 * @param <O>        the type of the final object
	 * @param serializer the {@link JsonSerializer} to deserialize the
	 *                   {@link JsonElement} to the object
	 * @return the current element parsed with the provided {@link JsonSerializer}
	 */
	public default <O> O getAsObject(JsonSerializer<O> serializer) {
		return serializer.deserializePath(this);
	}

	public record Case<T>(Predicate<JsonElementPath> isApplicable, Function<JsonElementPath, T> valueMapper) {

	}

	/**
	 * Experimental method to parse a object which can have different structures. e.
	 * g. a value is a sealed/abstract class and only the subtypes are set.
	 * 
	 * @param <T>   the type of the result object
	 * @param cases the different cases a value can be and how it can be parsed
	 * @return the result object
	 */
	public <T> T multiple(List<Case<T>> cases);

	/**
	 * Checks if the current value is a {@link JsonPrimitive}. Do only use this
	 * method in combination with {@link #multiple(List)} inside the
	 * {@link Case#isApplicable} method.
	 * 
	 * @return true if the current value is a {@link JsonPrimitive}
	 */
	public boolean isJsonPrimitive();

	/**
	 * Checks if the current value is a {@link JsonObject}. Do only use this method
	 * in combination with {@link #multiple(List)} inside the
	 * {@link Case#isApplicable} method.
	 * 
	 * @return true if the current value is a {@link JsonObject}
	 */
	public boolean isJsonObject();

	/**
	 * Checks if the current value is a {@link Number}. Do only use this method in
	 * combination with {@link #multiple(List)} inside the {@link Case#isApplicable}
	 * method.
	 * 
	 * @return true if the current value is a {@link Number}
	 */
	public boolean isNumber();

	/**
	 * Serializes this object based on the provided subtypes.
	 * 
	 * @param <I>             the type of the items to use for serializing each
	 *                        subtype
	 * @param <T>             the generic parent type
	 * @param items           the items to use for serializing each subtype
	 * @param itemKeyMapper   the mapper to map all types to a string
	 * @param objectToKeyPath the mapper to get the path to the json string
	 *                        identifier
	 * @param itemMapper      the final mapper to map one json to an element
	 * @return the parsed element
	 */
	public default <I, T> T polymorphic(//
			List<I> items, //
			Function<I, String> itemKeyMapper, //
			Function<JsonElementPath, StringPath<String>> objectToKeyPath, //
			BiFunction<JsonElementPath, Entry<String, I>, T> itemMapper //
	) {
		final var itemsByKey = items.stream().collect(toMap(itemKeyMapper, Function.identity()));
		return this.polymorphic(itemsByKey, objectToKeyPath, itemMapper);
	}

	/**
	 * Serializes this object based on the provided subtypes.
	 * 
	 * @param <I>             the type of the items to use for serializing each
	 *                        subtype
	 * @param <T>             the generic parent type
	 * @param itemsByKey      the items to use for serializing each subtype mapped
	 *                        by a string identifier
	 * @param objectToKeyPath the mapper to get the path to the json string
	 *                        identifier
	 * @param itemMapper      the final mapper to map one json to an element
	 * @return the parsed element
	 */
	public <I, T> T polymorphic(//
			Map<String, I> itemsByKey, //
			Function<JsonElementPath, StringPath<String>> objectToKeyPath, //
			BiFunction<JsonElementPath, Entry<String, I>, T> itemMapper //
	);

	/**
	 * Serializes this object based on the provided {@link PolymorphicSerializer}.
	 * 
	 * @param <I>                   the type of the items to use for serializing
	 *                              each subtype
	 * @param <T>                   the generic parent type
	 * @param polymorphicSerializer the {@link PolymorphicSerializer} to use for
	 *                              serializing
	 * @param objectToKeyPath       the mapper to get the path to the json string
	 *                              identifier
	 * @return the parsed element
	 */
	public default <I, T> T polymorphic(//
			PolymorphicSerializer<T> polymorphicSerializer, //
			Function<JsonElementPath, StringPath<String>> objectToKeyPath //
	) {
		return this.polymorphic(polymorphicSerializer.serializerByIdentifier(), objectToKeyPath, (t, u) -> {
			return u.getValue().deserializePath(t);
		});
	}

}