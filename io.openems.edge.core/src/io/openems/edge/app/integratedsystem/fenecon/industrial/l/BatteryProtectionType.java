package io.openems.edge.app.integratedsystem.fenecon.industrial.l;

import io.openems.common.session.Language;
import io.openems.edge.app.enums.TranslatableEnum;

public enum BatteryProtectionType implements TranslatableEnum {
	DEFAULT("Default battery protection"), //
	EXTENDED_VOLTAGE_RANGE("Extended Voltage Range"), //
	;

	private final String description;

	private BatteryProtectionType(String description) {
		this.description = description;
	}

	@Override
	public String getTranslation(Language language) {
		return this.description;
	}

}
