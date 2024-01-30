package io.openems.edge.app.enums;

import io.openems.common.session.Language;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.TranslationUtil;

public enum SafetyCountry implements TranslatableEnum {
	GERMANY("germany"), //
	AUSTRIA("austria"), //
	SWITZERLAND("switzerland"), //
	SWEDEN("sweden"), //
	CZECH("czech"), //
	HOLLAND("netherlands"), //
	IRELAND("ireland"), //
	UNITED_KINGDOM("united_kingdom"), //
	;

	private final String translationKey;

	private SafetyCountry(String translationKey) {
		this.translationKey = translationKey;
	}

	@Override
	public final String getTranslation(Language l) {
		final var bundle = AbstractOpenemsApp.getTranslationBundle(l);
		return TranslationUtil.getTranslation(bundle, this.translationKey);
	}

	/**
	 * Creates a {@link OptionsFactory} of this enum.
	 * 
	 * @return the {@link OptionsFactory}
	 */
	public static final OptionsFactory optionsFactory() {
		return OptionsFactory.of(values());
	}

}
