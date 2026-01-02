package io.openems.edge.app.meter.shelly;

import io.openems.common.session.Language;
import io.openems.edge.app.enums.TranslatableEnum;

public enum ShellyType implements TranslatableEnum {
	PLUS_PLUG_S("Plus Plug S", "IO.Shelly.Plus.PlugS"), //
	PLUG_S_GEN_3("Plug S Gen3", "IO.Shelly.PlugSG3"), //
	OUTDOOR_PLUG_S_GEN_3("Outdoor Plug S Gen3", "IO.Shelly.OutdoorPlugSG3"), //
	;

	private final String description;
	private final String factoryId;

	ShellyType(String description, String factoryId) {
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