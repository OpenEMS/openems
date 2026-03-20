package io.openems.edge.common.type;

import io.openems.common.session.Language;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public abstract class TextProvider {
	/**
	 * Returns the resulting text in the given language.
	 * 
	 * @param lang The language that should be used to lookup translations
	 * @return The resulting text
	 */
	public abstract String getText(Language lang);

	@Override
	public String toString() {
		return this.getText(null);
	}

	/**
	 * Returns a text provider that always returns the specified text.
	 * 
	 * @param text The text that should be always returned
	 * @return Text provider
	 */
	public static TextProvider byStatic(String text) {
		return new StaticTextProvider(text);
	}

	/**
	 * Returns a text provider that returns the translation to the specified
	 * translation key.
	 * 
	 * @param clazz          This class is used to identify the namespace where the
	 *                       translation files can be found.
	 * @param translationKey Translation key to lookup
	 * @return Text provider
	 */
	public static TextProvider byTranslation(Class<?> clazz, String translationKey) {
		return new TranslationTextProvider(clazz, translationKey);
	}

	static class StaticTextProvider extends TextProvider {
		private final String text;

		StaticTextProvider(String text) {
			this.text = text;
		}

		@Override
		public String getText(Language lang) {
			return this.text;
		}
	}

	static class TranslationTextProvider extends TextProvider {
		private final Class<?> clazz;
		private final String translationKey;

		public TranslationTextProvider(Class<?> clazz, String translationKey) {
			this.clazz = clazz;
			this.translationKey = translationKey;
		}

		@Override
		public String getText(Language lang) {
			if (lang == null) {
				lang = Language.DEFAULT;
			}

			var bundle = getResourceBundle(lang, this.clazz);
			if (bundle != null && bundle.containsKey(this.translationKey)) {
				return bundle.getString(this.translationKey);
			}
			if (lang != Language.EN) {
				// TODO: Use Language.DEFAULT for default language
				bundle = getResourceBundle(Language.EN, this.clazz);
				if (bundle != null && bundle.containsKey(this.translationKey)) {
					return bundle.getString(this.translationKey);
				}
			}

			return this.translationKey;
		}

		private static ResourceBundle getResourceBundle(Language lang, Class<?> clazz) {
			try {
				return ResourceBundle.getBundle(clazz.getPackageName() + ".translation", lang.getLocal(),
						clazz.getModule());
			} catch (MissingResourceException e) {
				return null;
			}
		}
	}
}
