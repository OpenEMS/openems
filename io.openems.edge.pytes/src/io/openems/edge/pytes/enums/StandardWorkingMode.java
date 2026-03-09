package io.openems.edge.pytes.enums;

import io.openems.common.types.OptionsEnum;

public enum StandardWorkingMode implements OptionsEnum {

	UNDEFINED(-1, "Undefined"),
	NO_RESPNSE_MODE(0, "No response mode"),
	VOLT_WATT_DEFAULT(1, "Volt watt default"),
	VOLT_VAR(2, "Volt–var"),
	FIXED_POWER_FACTOR(3, "Fixed power factor"),
	FIXED_REACTIVE_POWER(4, "Fix reactive power"),
	POWER_PF(5, "Power-PF"),
	RULE21_VOLT_WATT(6, "Rule21 Volt–watt"),
	IEEE1547_2018_REQUIRED_PQ(12, "IEEE1547-2018 Required P-Q");

    private final int value;
    private final String name;

    StandardWorkingMode(int value, String name) {
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
