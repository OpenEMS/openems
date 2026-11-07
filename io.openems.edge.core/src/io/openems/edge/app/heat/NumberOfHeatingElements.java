package io.openems.edge.app.heat;

import io.openems.common.session.Language;
import io.openems.edge.app.enums.TranslatableEnum;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.TranslationUtil;

/**
 * The number of heating elements controlled by the
 * {@link io.openems.edge.app.heat.HeatingElement} App.
 *
 * <p>
 * The names of this enum have to match the names of
 * {@code io.openems.edge.controller.io.heatingelement.enums.NumberOfHeatingElements}
 * of the controller bundle, since the selected value is written as a plain
 * String into the components configuration.
 */
public enum NumberOfHeatingElements implements TranslatableEnum {
	/**
	 * One heating element with 3 relays (Levels 0-3).
	 */
	ONE("App.Heat.HeatingElement.numberOfHeatingElements.one"), //
	/**
	 * Two heating elements with 6 relays (Levels 0-6).
	 */
	TWO("App.Heat.HeatingElement.numberOfHeatingElements.two"), //
	;

	private final String translationKey;

	private NumberOfHeatingElements(String translationKey) {
		this.translationKey = translationKey;
	}

	@Override
	public String getTranslation(Language language) {
		final var bundle = AbstractOpenemsApp.getTranslationBundle(language);
		return TranslationUtil.getTranslation(bundle, this.translationKey);
	}

}
