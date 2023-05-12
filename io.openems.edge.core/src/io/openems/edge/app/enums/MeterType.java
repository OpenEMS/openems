package io.openems.edge.app.enums;

import io.openems.common.session.Language;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.TranslationUtil;

/**
 * Copy of {@link io.openems.edge.meter.api.MeterType}.
 */
public enum MeterType implements TranslatableEnum {
	PRODUCTION("App.Meter.production"), //
	GRID("App.Meter.gridMeter"), //
	CONSUMPTION_METERED("App.Meter.consumtionMeter"), //
	;

	private final String translationKey;

	private MeterType(String translationKey) {
		this.translationKey = translationKey;
	}

	@Override
	public String getTranslation(Language l) {
		final var bundle = AbstractOpenemsApp.getTranslationBundle(l);
		return TranslationUtil.getTranslation(bundle, this.translationKey);
	}

}
