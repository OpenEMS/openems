package io.openems.edge.core.appmanager;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;

public class TranslationUtil {

	private static Translator instance = new NormalTranslator();

	/**
	 * Enables the debug mode for getting translations.
	 * 
	 * @return the {@link DebugTranslator} to get debug metrics.
	 */
	public static DebugTranslator enableDebugMode() {
		return (DebugTranslator) (instance = new DebugTranslator());
	}

	/**
	 * Disables the debug mode.
	 */
	public static void disableDebugMode() {
		instance = new NormalTranslator();
	}

	private interface Translator {

		public String getTranslation(ResourceBundle translationBundle, String key, Object... params);

	}

	public static final class DebugTranslator implements Translator {

		private final Set<String> missingKeys = new HashSet<String>();

		private DebugTranslator() {
		}

		@Override
		public String getTranslation(ResourceBundle translationBundle, String key, Object... params) {
			final var translation = getNullableTranslation(translationBundle, key, params);
			if (translation == null) {
				this.missingKeys.add(key);
				return key;
			}
			return translation;
		}

		public Set<String> getMissingKeys() {
			return this.missingKeys;
		}

	}

	public static final class NormalTranslator implements Translator {

		private NormalTranslator() {
		}

		@Override
		public String getTranslation(ResourceBundle translationBundle, String key, Object... params) {
			final var translation = getNullableTranslation(translationBundle, key, params);
			if (translation == null) {
				return key;
			}
			return translation;
		}

	}

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
		return instance.getTranslation(translationBundle, key, params);
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
