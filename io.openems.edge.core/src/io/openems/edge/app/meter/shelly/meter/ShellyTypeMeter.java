package io.openems.edge.app.meter.shelly.meter;

import io.openems.common.session.Language;
import io.openems.edge.app.enums.TranslatableEnum;

public enum ShellyTypeMeter implements TranslatableEnum {
	PRO_3EM("Pro 3EM", "IO.Shelly.Pro3EM"), //
	;

	private final String description;
	private final String factoryId;

	ShellyTypeMeter(String description, String factoryId) {
		this.description = description;
		this.factoryId = factoryId;
	}

	public String getFactoryId() {
		return this.factoryId;
	}

	@Override
	public String getTranslation(Language language) {
		return this.description;
	}
}
