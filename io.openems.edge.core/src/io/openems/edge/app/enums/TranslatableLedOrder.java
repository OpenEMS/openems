package io.openems.edge.app.enums;

import io.openems.common.session.Language;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.TranslationUtil;

public enum TranslatableLedOrder implements TranslatableEnum {

	DEFAULT_RED_BLUE_GREEN("App.FENECON.Home.ledOrder1"), //
	RED_GREEN_BLUE("App.FENECON.Home.ledOrder2"), //
	BLUE_RED_GREEN("App.FENECON.Home.ledOrder3"), //
	BLUE_GREEN_RED("App.FENECON.Home.ledOrder4"), //
	GREEN_RED_BLUE("App.FENECON.Home.ledOrder5"), //
	GREEN_BLUE_RED("App.FENECON.Home.ledOrder6");

	private final String translationKey;

	TranslatableLedOrder(String translationKey) {
		this.translationKey = translationKey;
	}

	@Override
	public String getTranslation(Language l) {
		final var bundle = AbstractOpenemsApp.getTranslationBundle(l);
		return TranslationUtil.getTranslation(bundle, this.translationKey);
	}

	/**
	 * Creates a {@link OptionsFactory} of this enum.
	 *
	 * @return the {@link OptionsFactory}
	 */
	public static final OptionsFactory optionsFactory() {
		return OptionsFactory.of(values());
	}
}
