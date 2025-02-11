package io.openems.common.jsonrpc.serialization;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import com.google.gson.JsonPrimitive;

import io.openems.common.jsonrpc.serialization.StringPathParser.StringParserChannelAddress;
import io.openems.common.jsonrpc.serialization.StringPathParser.StringParserEnum;
import io.openems.common.jsonrpc.serialization.StringPathParser.StringParserLocalDate;
import io.openems.common.jsonrpc.serialization.StringPathParser.StringParserSemanticVersion;
import io.openems.common.jsonrpc.serialization.StringPathParser.StringParserString;
import io.openems.common.jsonrpc.serialization.StringPathParser.StringParserUuid;
import io.openems.common.jsonrpc.serialization.StringPathParser.StringParserZonedDateTime;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.SemanticVersion;

public interface JsonPrimitivePath extends JsonPath {

	/**
	 * Gets the current {@link JsonPrimitivePath} as a {@link StringPath}.
	 * 
	 * @param <T>    the actual type of the string value
	 * @param parser the parser to parse the string
	 * @return the current element as a {@link StringPath}
	 */
	public <T> StringPath<T> getAsStringPath(StringParser<T> parser);

	/**
	 * Gets the current {@link JsonPrimitivePath} as a {@link StringPath} which just
	 * contains its raw string as the parsed value.
	 * 
	 * @return the current element as a {@link StringPath}
	 */
	public default StringPath<String> getAsStringPathString() {
		return this.getAsStringPath(new StringParserString());
	}

	/**
	 * Gets the current {@link JsonPrimitivePath} as a {@link StringPath} which
	 * contains a {@link ChannelAddress} as its parsed value.
	 * 
	 * @return the current element as a {@link StringPath}
	 */
	public default StringPath<ChannelAddress> getAsStringPathChannelAddress() {
		return this.getAsStringPath(new StringParserChannelAddress());
	}

	/**
	 * Gets the current {@link JsonPrimitivePath} as a {@link StringPath} which
	 * contains a {@link UUID} as its parsed value.
	 * 
	 * @return the current element as a {@link StringPath}
	 */
	public default StringPath<UUID> getAsStringPathUuid() {
		return this.getAsStringPath(new StringParserUuid());
	}

	/**
	 * Gets the current {@link JsonPrimitivePath} as a {@link StringPath} which
	 * contains a {@link SemanticVersion} as its parsed value.
	 * 
	 * @return the current element as a {@link StringPath}
	 */
	public default StringPath<SemanticVersion> getAsStringPathSemanticVersion() {
		return this.getAsStringPath(new StringParserSemanticVersion());
	}

	/**
	 * Gets the current {@link JsonPrimitivePath} as a {@link StringPath} which
	 * contains a {@link Enum} as its parsed value.
	 * 
	 * @param <T>       the type of the enum value
	 * @param enumClass the type class of the enum
	 * @return the current element as a {@link StringPath}
	 */
	public default <T extends Enum<T>> StringPath<T> getAsStringPathEnum(Class<T> enumClass) {
		return this.getAsStringPath(new StringParserEnum<>(enumClass));
	}

	/**
	 * Gets the current {@link JsonPrimitivePath} as a {@link StringPath} which
	 * contains a {@link ZonedDateTime} as its parsed value.
	 * 
	 * @param formatter the {@link DateTimeFormatter} used to parse the string
	 * @return the current element as a {@link StringPath}
	 */
	public default StringPath<ZonedDateTime> getAsStringPathZonedDateTime(DateTimeFormatter formatter) {
		return this.getAsStringPath(new StringParserZonedDateTime(formatter));
	}

	/**
	 * Gets the current {@link JsonPrimitivePath} as a {@link StringPath} which
	 * contains a {@link ZonedDateTime} as its parsed value.
	 * 
	 * @return the current element as a {@link StringPath}
	 */
	public default StringPath<ZonedDateTime> getAsStringPathZonedDateTime() {
		return this.getAsStringPath(new StringParserZonedDateTime());
	}

	/**
	 * Gets the current {@link JsonPrimitivePath} as a {@link StringPath} which
	 * contains a {@link LocalDate} as its parsed value.
	 * 
	 * @param formatter the {@link DateTimeFormatter} used to parse the string
	 * @return the current element as a {@link StringPath}
	 */
	public default StringPath<LocalDate> getAsStringPathLocalDate(DateTimeFormatter formatter) {
		return this.getAsStringPath(new StringParserLocalDate(formatter));
	}

	/**
	 * Gets the current {@link JsonPrimitivePath} as a {@link StringPath} which
	 * contains a {@link LocalDate} as its parsed value.
	 * 
	 * @return the current element as a {@link StringPath}
	 */
	public default StringPath<LocalDate> getAsStringPathLocalDate() {
		return this.getAsStringPath(new StringParserLocalDate());
	}

	/**
	 * Gets the current {@link JsonPrimitivePath} as a {@link String}.
	 * 
	 * @return the current element as a {@link String}
	 */
	public default String getAsString() {
		return this.getAsStringPathString().get();
	}

	/**
	 * Gets the current {@link JsonPrimitivePath} as a {@link ChannelAddress}.
	 * 
	 * @return the current element as a {@link ChannelAddress}
	 */
	public default ChannelAddress getAsChannelAddress() {
		return this.getAsStringPathChannelAddress().get();
	}

	/**
	 * Gets the current {@link JsonPrimitivePath} as a {@link UUID}.
	 * 
	 * @return the current element as a {@link UUID}
	 */
	public default UUID getAsUuid() {
		return this.getAsStringPathUuid().get();
	}

	/**
	 * Gets the current {@link JsonPrimitivePath} as a {@link SemanticVersion}.
	 * 
	 * @return the current element as a {@link SemanticVersion}
	 */
	public default SemanticVersion getAsSemanticVersion() {
		return this.getAsStringPathSemanticVersion().get();
	}

	/**
	 * Gets the current {@link JsonPrimitivePath} as a {@link Enum}.
	 * 
	 * @param <T>       the type of the enum value
	 * @param enumClass the type class of the enum
	 * @return the current element as a {@link Enum}
	 */
	public default <T extends Enum<T>> T getAsEnum(Class<T> enumClass) {
		return this.getAsStringPathEnum(enumClass).get();
	}

	/**
	 * Gets the current {@link JsonPrimitivePath} as a {@link ZonedDateTime}.
	 * 
	 * @param formatter the {@link DateTimeFormatter} used to parse the string
	 * @return the current element as a {@link ZonedDateTime}
	 */
	public default ZonedDateTime getAsZonedDateTime(DateTimeFormatter formatter) {
		return this.getAsStringPathZonedDateTime(formatter).get();
	}

	/**
	 * Gets the current {@link JsonPrimitivePath} as a {@link ZonedDateTime}.
	 * 
	 * @return the current element as a {@link ZonedDateTime}
	 */
	public default ZonedDateTime getAsZonedDateTime() {
		return this.getAsStringPathZonedDateTime().get();
	}

	/**
	 * Gets the current {@link JsonPrimitivePath} as a {@link LocalDate}.
	 * 
	 * @param formatter the {@link DateTimeFormatter} used to parse the string
	 * @return the current element as a {@link LocalDate}
	 */
	public default LocalDate getAsLocalDate(DateTimeFormatter formatter) {
		return this.getAsStringPathLocalDate(formatter).get();
	}

	/**
	 * Gets the current {@link JsonPrimitivePath} as a {@link LocalDate}.
	 * 
	 * @return the current element as a {@link LocalDate}
	 */
	public default LocalDate getAsLocalDate() {
		return this.getAsStringPathLocalDate().get();
	}

	/**
	 * Gets the current {@link JsonPrimitivePath} as a {@link NumberPath}.
	 * 
	 * @return the current element as a {@link NumberPath}
	 */
	public NumberPath getAsNumberPath();

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
	 * Gets the current {@link JsonPrimitivePath} as a {@link BooleanPath}.
	 * 
	 * @return the current element as a {@link BooleanPath}
	 */
	public BooleanPath getAsBooleanPath();

	/**
	 * Gets the current {@link JsonPrimitivePath} as a primitive {@link Boolean}.
	 * 
	 * @return the current element as a primitive {@link Boolean}
	 */
	public default boolean getAsBoolean() {
		return this.getAsBooleanPath().get();
	}

	/**
	 * Gets the current {@link JsonPrimitive} element.
	 * 
	 * @return the raw element
	 */
	public JsonPrimitive get();

}
