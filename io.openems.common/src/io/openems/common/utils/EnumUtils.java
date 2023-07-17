package io.openems.common.utils;

import java.util.EnumMap;
import java.util.Optional;

import com.google.common.base.CaseFormat;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;

public class EnumUtils {

	/**
	 * Converts the Enum {@link CaseFormat#UPPER_UNDERSCORE} name to
	 * {@link CaseFormat#UPPER_CAMEL}-case.
	 * 
	 * @param <ENUM> the type
	 * @param e      the enum
	 * @return the name as Camel-Case
	 */
	public static <ENUM extends Enum<ENUM>> String nameAsCamelCase(ENUM e) {
		return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, e.name());
	}

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
	 * Gets the {@link JsonElement} as {@link Optional} {@link Enum}.
	 * 
	 * @param <ENUM>   the type {@link EnumMap}
	 * @param <E>      the {@link Enum} type
	 * @param enumType the class of the {@link Enum}
	 * @param map      the {@link EnumMap}
	 * @param member   the member of the {@link EnumMap}
	 * @return the enum value
	 */
	public static <ENUM extends Enum<ENUM>, E extends Enum<E>> Optional<E> getAsOptionalEnum(Class<E> enumType,
			EnumMap<ENUM, JsonElement> map, ENUM member) {
		try {
			return JsonUtils.getAsOptionalEnum(enumType, getAsPrimitive(map, member));
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
	 * Gets the member of the {@link EnumMap} as {@link Optional} {@link Integer}.
	 *
	 * @param <ENUM> the type of the EnumMap key
	 * @param map    the {@link EnumMap}
	 * @param member the member
	 * @return the {@link Optional} {@link Integer} value
	 * @throws OpenemsNamedException on error
	 */
	public static <ENUM extends Enum<ENUM>> Optional<Integer> getAsOptionalInt(EnumMap<ENUM, JsonElement> map,
			ENUM member) {
		try {
			return Optional.of(getAsInt(map, member));
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
	 * Gets the member of the {@link EnumMap} as {@link JsonArray}.
	 *
	 * @param <ENUM> the type of the EnumMap key
	 * @param map    the {@link EnumMap}
	 * @param member the member
	 * @return the {@link JsonArray} value
	 * @throws OpenemsNamedException on error
	 */
	public static <ENUM extends Enum<ENUM>> JsonArray getAsJsonArray(EnumMap<ENUM, JsonElement> map, ENUM member)
			throws OpenemsNamedException {
		var jSubElement = getSubElement(map, member);
		return JsonUtils.getAsJsonArray(jSubElement);
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
	 * Gets the member of the {@link EnumMap} as {@link Enum}.
	 *
	 * @param <ENUM>   the type {@link EnumMap}
	 * @param <E>      the {@link Enum} type
	 * @param enumType the class of the {@link Enum}
	 * @param map      the {@link EnumMap}
	 * @param member   the member
	 * @return the enum value
	 * @throws OpenemsNamedException on error
	 */
	public static <ENUM extends Enum<ENUM>, E extends Enum<E>> E getAsEnum(Class<E> enumType,
			EnumMap<ENUM, JsonElement> map, ENUM member) throws OpenemsNamedException {
		return JsonUtils.getAsEnum(enumType, getAsPrimitive(map, member));
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
