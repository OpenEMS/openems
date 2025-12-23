package io.openems.edge.app.integratedsystem.fenecon.industrial.l;

import io.openems.common.session.Language;
import io.openems.edge.app.enums.TranslatableEnum;

public enum BatteryFirmwareVersion implements TranslatableEnum {
	ENFAS_VERSION_1_0_17("Enfas 1.0.17 (HW2.2) (2023-07-17)"), //
	WUERTH_VERSION_1_0_20("Wuerth 1.0.20 (HW2.2) (2024-01-24)"), //
	WUERTH_VERSION_CURRENT("Wuerth (HW2.5) (2025 onwards)"), //
	;

	private final String description;

	private BatteryFirmwareVersion(String description) {
		this.description = description;
	}

	@Override
	public String getTranslation(Language language) {
		return this.description;
	}

}
