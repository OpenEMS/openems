package io.openems.edge.app.enums;

import io.openems.common.session.Language;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.TranslationUtil;

public enum Wiring implements TranslatableEnum {
	SINGLE_PHASE("App.Evse.ChargePoint.Keba.Wiring.one"), //
	THREE_PHASE("App.Evse.ChargePoint.Keba.Wiring.three") //
	;

	private final String translationKey;

	private Wiring(String translationKey) {
		this.translationKey = translationKey;
	}

	@Override
	public String getTranslation(Language l) {
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
