package io.openems.common.utils;

import java.util.EnumMap;
import java.util.Optional;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;

public class EnumUtils {

	/**
	 * Gets the member of the {@link EnumMap} as {@link Optional} {@link Boolean}.
	 *
	 * @param <ENUM> the type of the EnumMap key
	 * @param map    the {@link EnumMap}
	 * @param member the member
	 * @return the {@link Optional} {@link Boolean} value
	 * @throws OpenemsNamedException on error
	 */
	public static <ENUM extends Enum<ENUM>> Optional<Boolean> getAsOptionalBoolean(EnumMap<ENUM, JsonElement> map,
			ENUM member) {
		try {
			return Optional.of(getAsBoolean(map, member));
		} catch (OpenemsNamedException e) {
			return Optional.empty();
		}
	}

	/**
	 * Gets the member of the {@link EnumMap} as {@link Optional} {@link String}.
	 *
	 * @param <ENUM> the type of the EnumMap key
	 * @param map    the {@link EnumMap}
	 * @param member the member
	 * @return the {@link Optional} {@link String} value
	 * @throws OpenemsNamedException on error
	 */
	public static <ENUM extends Enum<ENUM>> Optional<String> getAsOptionalString(EnumMap<ENUM, JsonElement> map,
			ENUM member) {
		try {
			return Optional.of(getAsString(map, member));
		} catch (OpenemsNamedException e) {
			return Optional.empty();
		}
	}

	/**
	 * Gets the member of the {@link EnumMap} as {@link JsonPrimitive}.
	 *
	 * @param <ENUM> the type of the EnumMap key
	 * @param map    the {@link EnumMap}
	 * @param member the member
	 * @return the {@link JsonPrimitive} value
	 * @throws OpenemsNamedException on error
	 */
	public static <ENUM extends Enum<ENUM>> JsonPrimitive getAsPrimitive(EnumMap<ENUM, JsonElement> map, ENUM member)
			throws OpenemsNamedException {
		var jSubElement = getSubElement(map, member);
		return JsonUtils.getAsPrimitive(jSubElement);
	}

	/**
	 * Gets the member of the {@link EnumMap} as {@link Boolean}.
	 *
	 * @param <ENUM> the type of the EnumMap key
	 * @param map    the {@link EnumMap}
	 * @param member the member
	 * @return the {@link Boolean} value
	 * @throws OpenemsNamedException on error
	 */
	public static <ENUM extends Enum<ENUM>> Boolean getAsBoolean(EnumMap<ENUM, JsonElement> map, ENUM member)
			throws OpenemsNamedException {
		return JsonUtils.getAsBoolean(getAsPrimitive(map, member));
	}

	/**
	 * Gets the member of the {@link EnumMap} as {@link String}.
	 *
	 * @param <ENUM> the type of the EnumMap key
	 * @param map    the {@link EnumMap}
	 * @param member the member
	 * @return the {@link String} value
	 * @throws OpenemsNamedException on error
	 */
	public static <ENUM extends Enum<ENUM>> String getAsString(EnumMap<ENUM, JsonElement> map, ENUM member)
			throws OpenemsNamedException {
		return JsonUtils.getAsString(getAsPrimitive(map, member));
	}

	/**
	 * Gets the member of the {@link EnumMap} as {@link JsonElement}.
	 *
	 * @param <ENUM> the type of the EnumMap key
	 * @param map    the {@link EnumMap}
	 * @param member the member
	 * @return the {@link JsonElement} value
	 * @throws OpenemsNamedException on error
	 */
	public static <ENUM extends Enum<ENUM>> JsonElement getSubElement(EnumMap<ENUM, JsonElement> map, ENUM member)
			throws OpenemsNamedException {
		if (!map.containsKey(member)) {
			throw OpenemsError.JSON_HAS_NO_MEMBER.exception(member,
					StringUtils.toShortString(map.toString(), 100).replace("%", "%%"));
		}
		return map.get(member);
	}

	/**
	 * Gets the member of the {@link EnumMap} as int.
	 *
	 * @param <ENUM> the type of the EnumMap key
	 * @param map    the {@link EnumMap}
	 * @param member the member
	 * @return the int value
	 * @throws OpenemsNamedException on error
	 */
	public static <ENUM extends Enum<ENUM>> int getAsInt(EnumMap<ENUM, JsonElement> map, ENUM member)
			throws OpenemsNamedException {
		return JsonUtils.getAsInt(getAsPrimitive(map, member));
	}
}
