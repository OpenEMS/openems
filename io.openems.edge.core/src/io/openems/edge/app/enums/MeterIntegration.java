package io.openems.edge.app.enums;

import io.openems.common.session.Language;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.TranslationUtil;

public enum MeterIntegration implements TranslatableEnum {
	INTERN("internal"), //
	EXTERN("external");

	private final String translationKey;

	private MeterIntegration(String translationKey) {
		this.translationKey = translationKey;
	}

	@Override
	public final String getTranslation(Language l) {
		final var bundle = AbstractOpenemsApp.getTranslationBundle(l);
		return TranslationUtil.getTranslation(bundle, this.translationKey);
	}
}
