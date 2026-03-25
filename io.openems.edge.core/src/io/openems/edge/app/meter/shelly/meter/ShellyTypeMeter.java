package io.openems.edge.app.meter.shelly.meter;

import io.openems.common.session.Language;
import io.openems.edge.app.enums.TranslatableEnum;

public enum ShellyTypeMeter implements TranslatableEnum {
	PRO_3EM("Pro 3EM", "IO.Shelly.Pro3EM", 0), //
	PRO_1PM("Pro 1PM", "IO.Shelly.Pro1PM", 0), //
	PRO_2PM("Pro 2PM", "IO.Shelly.Pro2PM", 2), //
	PRO_4PM("Pro 4PM", "IO.Shelly.Pro4PM", 4), //
	GEN3_3EM("3EM Gen3", "IO.Shelly.3EMG3", 0), //
	;

	private final String description;
	private final String factoryId;
	private final int amountOfRequiredTerminalComponents;

	ShellyTypeMeter(String description, String factoryId, int amountOfRequiredTerminalComponents) {
		this.description = description;
		this.factoryId = factoryId;
		this.amountOfRequiredTerminalComponents = amountOfRequiredTerminalComponents;
	}

	public String getFactoryId() {
		return this.factoryId;
	}

	@Override
	public String getTranslation(Language language) {
		return this.description;
	}

	public int getAmountOfRequiredTerminalComponents() {
		return this.amountOfRequiredTerminalComponents;
	}
}
