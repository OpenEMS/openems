package io.openems.edge.goodwe.common.enums;

import io.openems.common.types.OptionsEnum;

public enum DredCmd implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	DRED0(0x00FF, "Dred 0"), //
	DRED1(0x0001, "Dred 1"), //
	DRED2(0x0002, "Dred 2"), //
	DRED3(0x0004, "Dred 3"), //
	DRED4(0x0008, "Dred 4"), //
	DRED5(0x0010, "Dred 5"), //
	DRED6(0x0020, "Dred 6"), //
	DRED7(0x0040, "Dred 7"), //
	DRED8(0x0080, "Dred 8"); //

	private final int value;
	private final String option;

	private DredCmd(int value, String option) {
		this.value = value;
		this.option = option;
	}

	@Override
	public int getValue() {
		return this.value;
	}

	@Override
	public String getName() {
		return this.option;
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}
}