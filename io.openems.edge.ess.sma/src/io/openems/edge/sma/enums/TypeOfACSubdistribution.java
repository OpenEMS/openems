package io.openems.edge.sma.enums;

import io.openems.common.types.OptionsEnum;

public enum TypeOfACSubdistribution implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	NONE(302, "None"), //
	MULTICLUSTER_BOX_6(2609, "Multicluster Box 6"), //
	MULTICLUSTER_BOX_12(2610, "Multicluster Box 12"), //
	MULTICLUSTER_BOX_36(2611, "Multicluster Box 36");

	private final int value;
	private final String name;

	private TypeOfACSubdistribution(int value, String name) {
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