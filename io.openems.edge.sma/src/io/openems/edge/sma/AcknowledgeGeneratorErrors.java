package io.openems.edge.sma;

import io.openems.edge.common.channel.doc.OptionsEnum;

public enum AcknowledgeGeneratorErrors implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	ACKNOWLEDGE_ERROR(26, "Acknowledge Error");

	private final int value;
	private final String name;

	private AcknowledgeGeneratorErrors(int value, String name) {
		this.value = value;
		this.name = name;
	}

	@Override
	public int getValue() {
		return value;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}
}