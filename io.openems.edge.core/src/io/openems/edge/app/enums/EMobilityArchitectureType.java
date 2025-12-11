package io.openems.edge.app.enums;

import io.openems.common.session.Language;

public enum EMobilityArchitectureType implements TranslatableEnum {

	EVCS, //
	EVSE;

	@Override
	public String getTranslation(Language language) {
		return this.name();
	}
}
