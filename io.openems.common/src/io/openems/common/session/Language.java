package io.openems.common.session;

import java.util.Locale;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;

public enum Language {
	EN(Locale.ENGLISH), //
	DE(Locale.GERMAN), //
	CZ(Locale.forLanguageTag("cs")), //
	NL(Locale.forLanguageTag("nl")), //
	ES(Locale.forLanguageTag("es")), //
	FR(Locale.FRENCH);

	public static Language DEFAULT = Language.EN;

	private static Logger LOG = LoggerFactory.getLogger(Language.class);

	/**
	 * Get {@link Language} for given key of the language. If the language key does
	 * not exist, {@link Language#EN} is returned as default. The given key is
	 * removed all leading and trailing whitespaces and converts all characters to
	 * upper case.
	 *
	 * @param languageKey to get the {@link Language}
	 * @return the founded {@link Language} or throws an exception
	 * @throws OpenemsException on error
	 */
	public static Language from(String languageKey) throws OpenemsException {
		try {
			return Language.valueOf(languageKey.trim().toUpperCase());
		} catch (IllegalArgumentException ex) {
			LOG.warn("Language [" + languageKey + "] is not supported");
			return Language.DEFAULT;
		}
	}

	/**
	 * Get {@link Language} for given key of the language. If the language key does
	 * not exist, {@link Language#EN} is returned as default. The given key is
	 * removed all leading and trailing whitespaces and converts all characters to
	 * upper case.
	 *
	 * @param languageKey to get the {@link Language}
	 * @return the founded {@link Language} or throws an exception
	 * @throws OpenemsException on error
	 */
	public static Language from(Optional<String> languageKey) throws OpenemsException {
		if (languageKey.isPresent()) {
			return Language.from(languageKey.get());
		}
		return Language.DEFAULT;
	}

	private final Locale locale;

	private Language(Locale locale) {
		this.locale = locale;
	}

	public Locale getLocal() {
		return this.locale;
	}

}