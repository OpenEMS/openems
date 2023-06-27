package io.openems.edge.app.timeofusetariff;

import io.openems.common.session.Language;
import io.openems.edge.app.enums.TranslatableEnum;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.TranslationUtil;

public enum ControlMode implements TranslatableEnum {
	CHARGE_CONSUMPTION("App.TimeOfUseTariff.controlMode.chargeConsumption"), //
	DELAY_DISCHARGE("App.TimeOfUseTariff.controlMode.delayDischarge"); //

	private final String translationKey;

	private ControlMode(String translationKey) {
		this.translationKey = translationKey;
	}

	@Override
	public final String getTranslation(Language l) {
		final var bundle = AbstractOpenemsApp.getTranslationBundle(l);
		return TranslationUtil.getTranslation(bundle, this.translationKey);
	}
}
