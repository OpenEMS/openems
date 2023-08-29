package io.openems.edge.common.currency;

import io.openems.common.types.OptionsEnum;

public enum Currency implements OptionsEnum {
	UNDEFINED(-1, "-"), //
	EUR(0, "€"), //
	SEK(1, "kr"), //
	;

	private final String name;
	private final int value;

	private Currency(int value, String name) {
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
		return Currency.UNDEFINED;
	}

}
