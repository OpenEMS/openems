package io.openems.edge.common.channel;

import io.openems.common.types.OptionsEnum;

public enum TestOptions implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	OPTION_1(1, "Option 1"), //
	OPTION_2(2, "Option 2"); //

	private final int value;
	private final String name;

	private TestOptions(int value, String name) {
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