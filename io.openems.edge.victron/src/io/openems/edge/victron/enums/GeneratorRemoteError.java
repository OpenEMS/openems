package io.openems.edge.victron.enums;

import io.openems.common.types.OptionsEnum;

public enum GeneratorRemoteError implements OptionsEnum {

    UNDEFINED(-1, "undefined"), //
    NO_ERROR(0, "No error"), //
    REMOTE_DISABLED(1, "Remote disabled"), //
    REMOTE_FAULT(2, "Remote Fault") //
    ;

    private final int value;
    private final String name;

    private GeneratorRemoteError(int value, String name) {
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
