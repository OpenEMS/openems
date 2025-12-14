package io.openems.common.jsonrpc.serialization;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.function.Function;

import com.google.gson.JsonElement;

import io.openems.common.types.ChannelAddress;
import io.openems.common.types.SemanticVersion;

public interface JsonElementPathNullable {

	/**
	 * Maps the current value using the provided mapper if the current value is not
	 * null otherwise returns null.
	 * 
	 * @param <T>    the type of the mapping result
	 * @param mapper the mapper to convert the non-null {@link JsonArrayPath} to a
	 *               result object
	 * @return the result of the mapper function if the current value is not null
	 *         otherwise null
	 */
	public <T> T mapIfPresent(Function<JsonElementPath, T> mapper);

	/**
	 * Gets the current {@link JsonElementPathNullable} as a
	 * {@link JsonArrayPathNullable}.
	 * 
	 * @return the current element as a {@link JsonArrayPathNullable}
	 */
	public JsonArrayPathNullable getAsJsonArrayPathNullable();

	/**
	 * Gets the current {@link JsonElementPathNullable} as a
	 * {@link JsonObjectPathNullable}.
	 * 
	 * @return the current element as a {@link JsonObjectPathNullable}
	 */
	public JsonObjectPathNullable getAsJsonObjectPathNullable();

	/**
	 * Gets the current {@link JsonElementPathNullable} as a
	 * {@link JsonPrimitivePathNullable}.
	 * 
	 * @return the current element as a {@link JsonPrimitivePathNullable}
	 */
	public JsonPrimitivePathNullable getAsJsonPrimitivePathNullable();

	/**
	 * Gets the current {@link JsonElementPathNullable} as a
	 * {@link BooleanPathNullable}.
	 * 
	 * @return the current element as a {@link BooleanPathNullable}
	 */
	public default BooleanPathNullable getAsBooleanPathNullable() {
		return this.getAsJsonPrimitivePathNullable().getAsBooleanPathNullable();
	}

	/**
	 * Gets the current {@link JsonElementPathNullable} as a
	 * {@link NumberPathNullable}.
	 * 
	 * @return the current element as a {@link NumberPathNullable}
	 */
	public default NumberPathNullable getAsNumberPathNullable() {
		return this.getAsJsonPrimitivePathNullable().getAsNumberPathNullable();
	}

	/**
	 * Gets the current {@link JsonElementPathNullable} as a
	 * {@link StringPathNullable}.
	 * 
	 * @param <T>    the actual type of the string value
	 * @param parser the parser to parse the string
	 * @return the current element as a {@link StringPathNullable}
	 */
	public default <T> StringPathNullable<T> getAsStringPathNullable(StringParser<T> parser) {
		return this.getAsJsonPrimitivePathNullable().getAsStringPathNullable(parser);
	}

	/**
	 * Gets the current {@link JsonElementPathNullable} as a
	 * {@link StringPathNullable}.
	 * 
	 * @return the current element as a {@link StringPathNullable}
	 */
	public default StringPathNullable<String> getAsStringPathNullableString() {
		return this.getAsJsonPrimitivePathNullable().getAsStringPathNullableString();
	}

	/**
	 * Gets the current {@link JsonElementPathNullable} as a
	 * {@link StringPathNullable}.
	 * 
	 * @return the current element as a {@link StringPathNullable}
	 */
	public default StringPathNullable<ChannelAddress> getAsStringPathNullableChannelAddress() {
		return this.getAsJsonPrimitivePathNullable().getAsStringPathNullableChannelAddress();
	}

	/**
	 * Gets the current {@link JsonElementPathNullable} as a
	 * {@link StringPathNullable}.
	 * 
	 * @param <T>       the type of the enum value
	 * @param enumClass the type class of the enum
	 * @return the current element as a {@link StringPathNullable}
	 */
	public default <T extends Enum<T>> StringPathNullable<T> getAsStringPathNullableEnum(Class<T> enumClass) {
		return this.getAsJsonPrimitivePathNullable().getAsStringPathNullableEnum(enumClass);
	}

	/**
	 * Gets the current {@link JsonElementPathNullable} as a
	 * {@link StringPathNullable}.
	 * 
	 * @return the current element as a {@link StringPathNullable}
	 */
	public default StringPathNullable<LocalDate> getAsStringPathNullableLocalDate() {
		return this.getAsJsonPrimitivePathNullable().getAsStringPathNullableLocalDate();
	}

	/**
	 * Gets the current {@link JsonElementPathNullable} as a
	 * {@link StringPathNullable}.
	 * 
	 * @param formatter the {@link DateTimeFormatter} used to parse the string
	 * @return the current element as a {@link StringPathNullable}
	 */
	public default StringPathNullable<LocalDate> getAsStringPathNullableLocalDate(DateTimeFormatter formatter) {
		return this.getAsJsonPrimitivePathNullable().getAsStringPathNullableLocalDate(formatter);
	}

	/**
	 * Gets the current {@link JsonElementPathNullable} as a
	 * {@link StringPathNullable}.
	 * 
	 * @return the current element as a {@link StringPathNullable}
	 */
	public default StringPathNullable<LocalTime> getAsStringPathNullableLocalTime() {
		return this.getAsJsonPrimitivePathNullable().getAsStringPathNullableLocalTime();
	}

	/**
	 * Gets the current {@link JsonElementPathNullable} as a
	 * {@link StringPathNullable}.
	 * 
	 * @param formatter the {@link DateTimeFormatter} used to parse the string
	 * @return the current element as a {@link StringPathNullable}
	 */
	public default StringPathNullable<LocalTime> getAsStringPathNullableLocalTime(DateTimeFormatter formatter) {
		return this.getAsJsonPrimitivePathNullable().getAsStringPathNullableLocalTime(formatter);
	}

	/**
	 * Gets the current {@link JsonElementPathNullable} as a
	 * {@link StringPathNullable}.
	 * 
	 * @return the current element as a {@link StringPathNullable}
	 */
	public default StringPathNullable<SemanticVersion> getAsStringPathNullableSemanticVersion() {
		return this.getAsJsonPrimitivePathNullable().getAsStringPathNullableSemanticVersion();
	}

	/**
	 * Gets the current {@link JsonElementPathNullable} as a
	 * {@link StringPathNullable}.
	 * 
	 * @return the current element as a {@link StringPathNullable}
	 */
	public default StringPathNullable<UUID> getAsStringPathNullableUuid() {
		return this.getAsJsonPrimitivePathNullable().getAsStringPathNullableUuid();
	}

	/**
	 * Gets the current {@link JsonElementPathNullable} as a
	 * {@link StringPathNullable}.
	 * 
	 * @return the current element as a {@link StringPathNullable}
	 */
	public default StringPathNullable<ZonedDateTime> getAsStringPathNullableZonedDateTime() {
		return this.getAsJsonPrimitivePathNullable().getAsStringPathNullableZonedDateTime();
	}

	/**
	 * Gets the current {@link JsonElementPathNullable} as a
	 * {@link StringPathNullable}.
	 * 
	 * @param formatter the {@link DateTimeFormatter} used to parse the string
	 * @return the current element as a {@link StringPathNullable}
	 */
	public default StringPathNullable<ZonedDateTime> getAsStringPathNullableZonedDateTime(DateTimeFormatter formatter) {
		return this.getAsJsonPrimitivePathNullable().getAsStringPathNullableZonedDateTime(formatter);
	}

	/**
	 * Gets the current {@link JsonElementPath} as a Object serialized with the
	 * provided {@link JsonSerializer} or null if the current element is not
	 * present.
	 * 
	 * @param <O>        the type of the final object
	 * @param serializer the {@link JsonSerializer} to deserialize the
	 *                   {@link JsonElement} to the object
	 * @return the current element parsed with the provided {@link JsonSerializer}
	 *         or null if the current element is not present
	 */
	public default <O> O getAsObjectOrNull(JsonSerializer<O> serializer) {
		return this.mapIfPresent(serializer::deserializePath);
	}

	/**
	 * Checks if the current value is present.
	 * 
	 * @return true if the current value is present; else false
	 */
	public boolean isPresent();

	/**
	 * Gets the {@link JsonElement} value of the current path.
	 * 
	 * @return the value; or null if not present
	 */
	public JsonElement getOrNull();

}