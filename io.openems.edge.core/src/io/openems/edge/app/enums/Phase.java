package io.openems.edge.app.enums;

import io.openems.common.session.Language;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.TranslationUtil;

/**
 * Copy of {@link io.openems.edge.bridge.modbus.api.Parity}.
 */
public enum Phase implements TranslatableEnum {
	ALL("all"), //
	L1("l1"), //
	L2("l2"), //
	L3("l3"), //
	;

	private final String translationKey;

	private Phase(String translationKey) {
		this.translationKey = translationKey;
	}

	@Override
	public final String getTranslation(Language l) {
		final var bundle = AbstractOpenemsApp.getTranslationBundle(l);
		return TranslationUtil.getTranslation(bundle, this.translationKey);
	}
}
