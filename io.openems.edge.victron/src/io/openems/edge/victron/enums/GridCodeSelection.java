package io.openems.edge.victron.enums;

import io.openems.common.types.OptionsEnum;

public enum GridCodeSelection implements OptionsEnum {
    UNDEFINED(-1, "Undefined"), //
    SA1741(0, "SA1741"), //
    VDE(1, "VDE"), //
    AUSTRALIAN(2, "Australian"), //
    G99(3, "G99"), //
    HAWAIIAN(4, "Hawaiian"), //
    EN50549(5, "EN50549"), //
    AUSTRIA_TYPEA(6, "Austria Type A") //
    ;

    private final int value;
    private final String name;

    private GridCodeSelection(int value, String name) {
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