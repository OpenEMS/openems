package io.openems.common.utils;

import java.util.Dictionary;
import java.util.Optional;

/**
 * Provides helper utilities to handle {@link Dictionary}s.
 */
public final class DictionaryUtils {
	
	private DictionaryUtils()  {
	}

	/**
	 * Get the Dictionary value as String.
	 *
	 * @param dict the {@link Dictionary}
	 * @param key  the identifier key
	 * @return the value as {@link String}
	 * @param <T> the key type
	 */
	public static <T> String getAsString(Dictionary<T, Object> dict, T key) {
		final var raw = dict.get(key);
		if (raw == null) {
			return null;
		}
		if (raw instanceof String s) {
			return s;
		} else {
			return raw.toString();
		}
	}

	/**
	 * Get the Dictionary value as Optional String.
	 *
	 * @param dict the {@link Dictionary}
	 * @param key  the identifier key
	 * @return the value as {@link Optional} {@link String}
	 * @param <T> the key type
	 */
	public static <T> Optional<String> getAsOptionalString(Dictionary<T, Object> dict, T key) {
		return Optional.ofNullable(getAsString(dict, key));
	}

	/**
	 * Get the Dictionary value as Integer.
	 *
	 * @param dict the {@link Dictionary}
	 * @param key  the identifier key
	 * @return the value as {@link Integer}
	 * @param <T> the key type
	 */
	public static <T> Integer getAsInteger(Dictionary<T, Object> dict, T key) {
		final var raw = dict.get(key);
		if (raw == null) {
			return null;
		}
		return switch (raw) {
			case Boolean b -> b ? 1 : 0;
			case Number n -> {
				yield (int) Math.max(Integer.MIN_VALUE, Math.min(n.longValue(), Integer.MAX_VALUE));
			}
			case String s when !s.isEmpty() -> {
				try {
					yield Integer.parseInt(s);
				} catch (NumberFormatException e) {
					yield null;
				}
			}
			default -> null;
		};
	}

	/**
	 * Get the Dictionary value as Optional Integer.
	 *
	 * @param dict the {@link Dictionary}
	 * @param key  the identifier key
	 * @return the value as {@link Optional} {@link Integer}
	 * @param <T> the key type
	 */
	public static <T> Optional<Integer> getAsOptionalInteger(Dictionary<T, Object> dict, T key) {
		return Optional.ofNullable(getAsInteger(dict, key));
	}

	/**
	 * Get the Dictionary value as Boolean.
	 *
	 * @param dict the {@link Dictionary}
	 * @param key  the identifier key
	 * @return the value as {@link Integer}
	 * @param <T> the key type
	 */
	public static <T> Boolean getAsBoolean(Dictionary<T, Object> dict, T key) {
		final var raw = dict.get(key);
		if (raw == null) {
			return null;
		}
		return switch (raw) {
			case Boolean b -> b;
			case Number n -> n.intValue() != 0;
			case String s when !s.isEmpty() -> switch (s.toLowerCase()) {
				case "true" -> true;
				case "false" -> false;
				default -> null;
			};
			default -> null;
		};
	}

	/**
	 * Get the Dictionary value as Optional Boolean.
	 *
	 * @param dict the {@link Dictionary}
	 * @param key  the identifier key
	 * @return the value as {@link Optional} {@link Integer}
	 * @param <T> the key type
	 */
	public static <T> Optional<Boolean> getAsOptionalBoolean(Dictionary<T, Object> dict, T key) {
		return Optional.ofNullable(getAsBoolean(dict, key));
	}

	/**
	 * Check if any of the given keys is contained in the Dictionary.
	 *
	 * @param dict the {@link Dictionary}
	 * @param keys the keys to check
	 * @return {@code true} if any of the keys is contained in the Dictionary
	 * @param <T> the key type
	 */
	@SafeVarargs
	public static <T> boolean containsAnyKey(Dictionary<T, ?> dict, T... keys) {
		if (dict == null || dict.isEmpty()) {
			return false;
		}
		for (var key : keys) {
			if (dict.get(key) != null) {
				return true;
			}
		}
		return false;
	}

}
