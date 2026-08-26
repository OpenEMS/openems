package io.openems.edge.app.enums;

import io.openems.common.session.Language;

public enum KebaHardwareType implements TranslatableEnum {
	P30, P40;

	@Override
	public String getTranslation(Language language) {
		return this.name();
	}
}
