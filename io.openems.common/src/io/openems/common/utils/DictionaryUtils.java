package io.openems.common.utils;

import com.google.common.primitives.Ints;

import java.util.Dictionary;
import java.util.Optional;

/**
 * Provides helper utilities to handle {@link Dictionary}s.
 */
public final class DictionaryUtils {

    private DictionaryUtils() {
    }

    /**
     * Get the Dictionary value as String.
     *
     * @param <T>  the key type
     * @param dict the {@link Dictionary}
     * @param key  the identifier key
     * @return the value as {@link String}
     */
    public static <T> String getAsString(Dictionary<T, Object> dict, T key) {
        final var raw = dict.get(key);
        return switch (raw) {
            case null -> null;
            case String s -> s;
            default -> raw.toString();
        };
    }

    /**
     * Get the Dictionary value as Optional String.
     *
     * @param <T>  the key type
     * @param dict the {@link Dictionary}
     * @param key  the identifier key
     * @return the value as {@link Optional} {@link String}
     */
    public static <T> Optional<String> getAsOptionalString(Dictionary<T, Object> dict, T key) {
        return Optional.ofNullable(getAsString(dict, key));
    }

    /**
     * Get the Dictionary value as Integer.
     *
     * @param <T>  the key type
     * @param dict the {@link Dictionary}
     * @param key  the identifier key
     * @return the value as {@link Integer}
     */
    public static <T> Integer getAsInteger(Dictionary<T, Object> dict, T key) {
        final var raw = dict.get(key);
        return switch (raw) {
            case Boolean b -> b ? 1 : 0;
            case Number n -> (int) Math.max(Integer.MIN_VALUE, Math.min(n.longValue(), Integer.MAX_VALUE));
            case String s -> Ints.tryParse(s);
            case null, default -> null;
        };
    }

    /**
     * Get the Dictionary value as Optional Integer.
     *
     * @param <T>  the key type
     * @param dict the {@link Dictionary}
     * @param key  the identifier key
     * @return the value as {@link Optional} {@link Integer}
     */
    public static <T> Optional<Integer> getAsOptionalInteger(Dictionary<T, Object> dict, T key) {
        return Optional.ofNullable(getAsInteger(dict, key));
    }

    /**
     * Get the Dictionary value as Boolean.
     *
     * @param <T>  the key type
     * @param dict the {@link Dictionary}
     * @param key  the identifier key
     * @return the value as {@link Integer}
     */
    public static <T> Boolean getAsBoolean(Dictionary<T, Object> dict, T key) {
        final var raw = dict.get(key);
        return switch (raw) {
            case Boolean b -> b;
            case Number n -> n.intValue() != 0;
            case String s when !s.isEmpty() -> switch (s.toLowerCase()) {
                case "true" -> true;
                case "false" -> false;
                default -> null;
            };
            case null, default -> null;
        };
    }

    /**
     * Get the Dictionary value as Optional Boolean.
     *
     * @param <T>  the key type
     * @param dict the {@link Dictionary}
     * @param key  the identifier key
     * @return the value as {@link Optional} {@link Integer}
     */
    public static <T> Optional<Boolean> getAsOptionalBoolean(Dictionary<T, Object> dict, T key) {
        return Optional.ofNullable(getAsBoolean(dict, key));
    }

    /**
     * Check if any of the given keys is contained in the Dictionary.
     *
     * @param <T>  the key type
     * @param dict the {@link Dictionary}
     * @param keys the keys to check
     * @return {@code true} if any of the keys is contained in the Dictionary
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
