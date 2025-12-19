package io.openems.edge.app.enums;

import io.openems.common.session.Language;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.TranslationUtil;

public enum EMobilityArchitectureType implements TranslatableEnum {

	EVCS("App.Evse.architecture.evcs"), //
	EVSE("App.Evse.architecture.evse");

	private final String translationKey;

	private EMobilityArchitectureType(String translationKey) {
		this.translationKey = translationKey;
	}

	@Override
	public String getTranslation(Language language) {
		final var bundle = AbstractOpenemsApp.getTranslationBundle(language);
		return TranslationUtil.getTranslation(bundle, this.translationKey);
	}
}
