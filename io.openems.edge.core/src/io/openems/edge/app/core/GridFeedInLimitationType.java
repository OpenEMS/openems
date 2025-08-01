package io.openems.edge.app.core;

import io.openems.common.session.Language;
import io.openems.edge.app.enums.TranslatableEnum;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.TranslationUtil;

public enum GridFeedInLimitationType implements TranslatableEnum {
	DYNAMIC_LIMITATION("App.IntegratedSystem.feedInType.dynamicLimitation"), //
	NO_LIMITATION("App.IntegratedSystem.feedInType.noLimitation"), //
	;

	private final String translationKey;

	GridFeedInLimitationType(String translationKey) {
		this.translationKey = translationKey;
	}

	@Override
	public String getTranslation(Language language) {
		final var bundle = AbstractOpenemsApp.getTranslationBundle(language);
		return TranslationUtil.getTranslation(bundle, this.translationKey);
	}

}