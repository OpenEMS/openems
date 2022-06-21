package io.openems.edge.core.appmanager;

import java.text.MessageFormat;
import java.util.MissingResourceException;
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
		try {
			var string = translationBundle.getString(key);
			return MessageFormat.format(string, params);
		} catch (MissingResourceException | IllegalArgumentException e) {
			e.printStackTrace();
			return key;
		}
	}

}
