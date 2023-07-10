package io.openems.edge.core.appmanager;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;

public class TranslationUtil {

	/**
	 * Gets the value for the given key from the translationBundle.
	 *
	 * @param translationBundle the translation bundle
	 * @param key               the key of the translation
	 * @param params            the parameter of the translation
	 * @return the translated string or the key if the translation was not found or
	 *         the format is invalid
	 */
	public static String getTranslation(ResourceBundle translationBundle, String key, Object... params) {
		final var translation = getNullableTranslation(translationBundle, key, params);
		if (translation == null) {
			return key;
		}
		return translation;
	}

	/**
	 * Gets the value for the given key from the translationBundle.
	 *
	 * @param translationBundle the translation bundle
	 * @param key               the key of the translation
	 * @param params            the parameter of the translation
	 * @return the translated string or null if the translation was not found or the
	 *         format is invalid
	 */
	public static String getNullableTranslation(//
			final ResourceBundle translationBundle, //
			final String key, //
			final Object... params //
	) {
		try {
			final var string = Objects.requireNonNull(translationBundle) //
					.getString(Objects.requireNonNull(key));
			if (params == null || params.length == 0) {
				return string;
			}
			return MessageFormat.format(string, params);
		} catch (MissingResourceException | IllegalArgumentException e) {
			e.printStackTrace();
			return null;
		}
	}

}
