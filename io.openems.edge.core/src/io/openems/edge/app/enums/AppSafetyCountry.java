package io.openems.edge.app.enums;

import io.openems.common.session.Language;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.goodwe.common.enums.SafetyCountry;

public enum AppSafetyCountry implements TranslatableEnum {
	GERMANY("germany", SafetyCountry.GERMANY), //
	AUSTRIA("austria", SafetyCountry.AUSTRIA), //
	SWITZERLAND("switzerland", SafetyCountry.SWITZERLAND), //
	SWEDEN("sweden", SafetyCountry.SWEDEN), //
	CZECH("czech", SafetyCountry.CZECH), //
	HOLLAND("netherlands", SafetyCountry.HOLLAND), //
	GREECE_MAINLAND("greece", SafetyCountry.GREECE_MAINLAND), //
	LITHUANIA("lithuania", SafetyCountry.EN50549), //
	;

	public final SafetyCountry goodWeValue;

	private final String translationKey;

	private AppSafetyCountry(String translationKey, SafetyCountry goodWeValue) {
		this.translationKey = translationKey;
		this.goodWeValue = goodWeValue;
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
