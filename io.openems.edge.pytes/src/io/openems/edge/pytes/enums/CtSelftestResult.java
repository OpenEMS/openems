package io.openems.edge.pytes.enums;

import io.openems.common.types.OptionsEnum;

public enum CtSelftestResult implements OptionsEnum {

	UNDEFINED(-1, "Undefined"),
	INVALID(0, "Invalid"),
	NOT_MEETING_DETECTION_CONDITIONS(1, "Not meeting detection conditions"),
	TESTING(2, "Testing"),
	NORMAL(3, "Normal"),
	ABNORMAL_CT_CONNECTION_DETECTION(100, "Abnormal CT connection detection (direction or phase)");

    private final int value;
    private final String name;

    CtSelftestResult(int value, String name) {
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
