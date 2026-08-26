package io.openems.edge.app.enums;

import io.openems.common.session.Language;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.TranslationUtil;

public enum TranslatableReactivePowerMode implements TranslatableEnum {

	UNSELECTED("App.IntegratedSystem.reactivePowerMode.unselected"), //
	FIX_PF("App.IntegratedSystem.reactivePowerMode.fixPf"), //
	FIX_Q("App.IntegratedSystem.reactivePowerMode.fixQ"), //
	QU_CURVE("App.IntegratedSystem.reactivePowerMode.QUCurve"), //
	COS_PHI_P_CURVE("App.IntegratedSystem.reactivePowerMode.cosPhiPCurve"), //
	QP_CURVE("App.IntegratedSystem.reactivePowerMode.QPCurve"); //

	private final String translationKey;

	private TranslatableReactivePowerMode(String translationKey) {
		this.translationKey = translationKey;
	}

	/**
	 * Gets the optionsFactory of this Enum.
	 * 
	 * @return the {@link OptionsFactory}
	 */
	public static OptionsFactory optionsFactory() {
		return OptionsFactory.of(TranslatableReactivePowerMode.class);
	}

	@Override
	public String getTranslation(Language language) {
		final var bundle = AbstractOpenemsApp.getTranslationBundle(language);
		return TranslationUtil.getTranslation(bundle, this.translationKey);
	}
}
