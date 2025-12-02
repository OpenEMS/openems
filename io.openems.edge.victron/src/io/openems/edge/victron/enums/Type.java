package io.openems.edge.victron.enums;

import io.openems.common.types.OptionsEnum;

public enum Type implements OptionsEnum {

    UNDEFINED(-1, "undefined"), //
    DOOR(2, "Door"), //
    BILGE_PUMP(3, "Bilge pump"), //
    BILGE_ALARM(4, "Bilge alarm"), //
    BURGLAR_ALARM(5, "Burglar alarm"), //
    SMOKE_ALARM(6, "Smoke alarm"), //
    FIRE_ALARM(7, "Fire alarm"), //
    CO2_ALARM(8, "CO2 alarm") //
    ;

    private final int value;
    private final String name;

    private Type(int value, String name) {
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
