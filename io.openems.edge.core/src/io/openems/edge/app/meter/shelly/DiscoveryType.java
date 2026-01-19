package io.openems.edge.app.meter.shelly;

import static io.openems.edge.core.appmanager.TranslationUtil.translate;

import io.openems.common.session.Language;
import io.openems.edge.app.enums.TranslatableEnum;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;

public enum DiscoveryType implements TranslatableEnum {
	STATIC("communication.discoveryType.static"), //
	MDNS("communication.discoveryType.mdns"), //
	;

	private final String value;

	DiscoveryType(String value) {
		this.value = value;
	}

	@Override
	public String getTranslation(Language language) {
		final var bundle = AbstractOpenemsApp.getTranslationBundle(language);
		return translate(bundle, this.value);
	}
}