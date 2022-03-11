package io.openems.edge.sma.enums;

import io.openems.common.types.OptionsEnum;

public enum MemoryCardStatus implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	READY(1788, "Ready"), //
	INITIALIZATION(1787, "Initialization"), //
	MEMORY_CARD_FULL(3102, "Memory Card Full"), //
	NO_FILE_SYSTEM_DETECTED(3103, "No file System Detected"), //
	UNSUPPORTED_DATA_SYSTEM(3104, "Unsupported Data System"), //
	WRITING_PARAMETERS(3105, "Writing Parameters"), //
	WRITING_PARAMETERS_FAILED(3106, "Writing Parameters Failed"), //
	WRITING_LOG_DATA(3107, "Writing log Data"), //
	NO_MEMORY_CARD_AVAILABLE(3108, "No Memory Card Available");

	private final int value;
	private final String name;

	private MemoryCardStatus(int value, String name) {
		this.value = value;
		this.name = name;
	}

	@Override
	public int getValue() {
		return this.value;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}
}