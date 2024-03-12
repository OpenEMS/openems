package io.openems.edge.app.integratedsystem;

import static io.openems.edge.core.appmanager.TranslationUtil.translate;

import io.openems.common.session.Language;
import io.openems.edge.app.enums.TranslatableEnum;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;

public enum GoodWeGridMeterCategory implements TranslatableEnum {
	SMART_METER("App.IntegratedSystem.gridMeterType.option.smartMeter"), //
	COMMERCIAL_METER("App.IntegratedSystem.gridMeterType.option.commercialMeter"), //
	;

	private final String translationKey;

	private GoodWeGridMeterCategory(String translationKey) {
		this.translationKey = translationKey;
	}

	@Override
	public String getTranslation(Language language) {
		final var bundle = AbstractOpenemsApp.getTranslationBundle(language);
		return translate(bundle, this.translationKey);
	}

}
