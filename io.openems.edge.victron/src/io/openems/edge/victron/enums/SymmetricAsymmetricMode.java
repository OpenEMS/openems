package io.openems.edge.victron.enums;

import io.openems.common.types.OptionsEnum;

public enum SymmetricAsymmetricMode implements OptionsEnum {
    UNDEFINED(-1, "Undefined"), //
    UNKNOWN(0, "Unknown"), //
    ASYMMETRIC(1, "Asymmetric Mode"), //
    SYMMETRIC(2, "Symmetric Mode"), //

    ;

    private final int value;
    private final String name;

    private SymmetricAsymmetricMode(int value, String name) {
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
