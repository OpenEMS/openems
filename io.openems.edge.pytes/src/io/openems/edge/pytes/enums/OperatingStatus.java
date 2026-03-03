package io.openems.edge.pytes.enums;

import io.openems.common.types.OptionsEnum;

public enum OperatingStatus implements OptionsEnum {

	UNDEFINED(-1, "Undefined"),
	STOP_RUNNING(0, "Stop running"),
	OPEN_LOOP_OPERATION(1, "Open loop operation"),
	SOFT_START_OPERATION(2, "Soft start operation"),
	GRID_CONNECTED_OPERATION(3, "Grid-connected operation"),
	OFF_GRID_OPERATION(4, "Off-grid operation"),
	OFF_GRID_TO_ON_GRID(5, "Off grid to on grid"),
	BACKUP_BYPASS(6, "Backup bypass"),
	GENERATOR_RUNNING(7, "Generator running");

    private final int value;
    private final String name;

    OperatingStatus(int value, String name) {
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
