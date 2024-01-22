package io.openems.edge.common.currency;

import io.openems.common.types.OptionsEnum;

public enum Currency implements OptionsEnum {
	UNDEFINED(-1), //
	EUR(0), //
	SEK(1), //
	;

	private final int value;

	private Currency(int value) {
		this.value = value;
	}

	@Override
	public int getValue() {
		return this.value;
	}

	@Override
	public String getName() {
		return this.name();
	}

	@Override
	public OptionsEnum getUndefined() {
		return Currency.UNDEFINED;
	}

}
