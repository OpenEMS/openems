package io.openems.edge.app.enums;

import io.openems.common.session.Language;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.TranslationUtil;

public enum ExternalLimitationType implements TranslatableEnum {
	NO_LIMITATION("App.IntegratedSystem.externalLimitationType.noLimitation"), //
	EXTERNAL_LIMITATION("App.IntegratedSystem.externalLimitationType.externalLimitation"), //
	DYNAMIC_EXTERNAL_LIMITATION("App.IntegratedSystem.externalLimitationType.dynamicExternalLimitation"), //

	// Deprecated types, kept for backward compatibility
	@Deprecated
	DYNAMIC_LIMITATION("App.IntegratedSystem.feedInType.dynamicLimitation"), //
	@Deprecated
	DYNAMIC_AND_EXTERNAL_LIMITATION("App.IntegratedSystem.feedInType.dynamicAndExternalLimitation"), //
	;

	private final String translationKey;

	ExternalLimitationType(String translationKey) {
		this.translationKey = translationKey;
	}

	@Override
	public String getTranslation(Language language) {
		final var bundle = AbstractOpenemsApp.getTranslationBundle(language);
		return TranslationUtil.getTranslation(bundle, this.translationKey);
	}

}