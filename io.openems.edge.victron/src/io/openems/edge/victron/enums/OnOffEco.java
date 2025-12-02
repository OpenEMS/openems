package io.openems.edge.victron.enums;

import io.openems.common.types.OptionsEnum;

public enum OnOffEco implements OptionsEnum {

    UNDEFINED(-1, "undefined"), //
    ON(2, "On"), //
    OFF(4, "Off"), //
    ECO(5, "Eco") //
    ;

    private final int value;
    private final String name;

    private OnOffEco(int value, String name) {
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
