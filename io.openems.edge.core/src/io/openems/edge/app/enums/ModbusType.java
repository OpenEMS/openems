package io.openems.edge.app.enums;

import io.openems.common.session.Language;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.TranslationUtil;

public enum ModbusType implements TranslatableEnum {
	TCP("communication.modbusIntegrationType.tcp"), //
	RTU("communication.modbusIntegrationType.rtu"), //
	;

	private final String translationKey;

	private ModbusType(String translation) {
		this.translationKey = translation;
	}

	@Override
	public String getTranslation(Language l) {
		final var bundle = AbstractOpenemsApp.getTranslationBundle(l);
		return TranslationUtil.getTranslation(bundle, this.translationKey);
	}

}
