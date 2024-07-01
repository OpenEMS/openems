package io.openems.edge.app.enums;

import io.openems.common.session.Language;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.TranslationUtil;

public enum FeedInType implements TranslatableEnum {
	DYNAMIC_LIMITATION("App.IntegratedSystem.feedInType.dynamicLimitation"), //
	EXTERNAL_LIMITATION("App.IntegratedSystem.feedInType.externalLimitation"), //
	NO_LIMITATION("App.IntegratedSystem.feedInType.noLimitation"), //
	;

	private final String translationKey;

	private FeedInType(String translationKey) {
		this.translationKey = translationKey;
	}

	@Override
	public String getTranslation(Language language) {
		final var bundle = AbstractOpenemsApp.getTranslationBundle(language);
		return TranslationUtil.getTranslation(bundle, this.translationKey);
	}

}