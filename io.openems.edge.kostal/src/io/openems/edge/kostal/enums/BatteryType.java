package io.openems.edge.kostal.enums;

import io.openems.common.types.OptionsEnum;

// x24C 588 Battery Type 6 - U16 1 RO 0x03
public enum BatteryType implements OptionsEnum {
    NO_BATTERY(0x0000, "No battery (PV-Functionality)"), //
    PIKO_BATTERY_LI(0x0002, "PIKO Battery Li"), //
    BYD(0x0004, "BYD"), //
    BMZ(0x0008, "BMZ"), //
    AXISTORAGE_LI_SH(0x0010, "AXIstorage Li SH"), //
    LG(0x0040, "LG"), //
    PONTECH_FORCE_H(0x0200, "Pyontech Force H"), //
    AXISTORAGE_LI_SV(0x0400, "AXIstorage Li SV"), //
    DYNESS_TOWER(0x1000, "Dyness Tower / TowerPro"), //
    VARTA_WALL(0x2000, "VARTA.wall"), //
    ZYC(0x4000, "ZYC"), //
    UNDEFINED(-1, "Undefined");

    private final int value;
    private final String name;

    private BatteryType(int value, String name) {
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
