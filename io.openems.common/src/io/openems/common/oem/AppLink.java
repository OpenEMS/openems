package io.openems.common.oem;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import io.openems.common.session.Language;

public record AppLink(Map<Language, String> links) {

	/**
	 * Gets the Website Link for a given language.
	 * 
	 * @param language the {@link Language}.
	 * @return the {@link String} link as an {@link Optional}
	 */
	public Optional<String> getLinkByLanguage(Language language) {
		return Optional.ofNullable(this.links.get(language));
	}

	/**
	 * Adds an empty link for a given {@link Language}.
	 * 
	 * @param language {@link Language}
	 * @return this
	 */
	public AppLink emptyLink(Language language) {
		this.links.put(language, "");
		return this;
	}

	/**
	 * Adds a given link for a given {@link Language}.
	 * 
	 * @param language {@link Language}
	 * @param url      the link
	 * @return this
	 */
	public AppLink addLink(Language language, String url) {
		this.links.put(language, url);
		return this;
	}

	/**
	 * Creates an empty {@link AppLink}.
	 * 
	 * @return the empty {@link AppLink}
	 */
	public static final AppLink create() {
		return new AppLink(new HashMap<Language, String>());
	}

}