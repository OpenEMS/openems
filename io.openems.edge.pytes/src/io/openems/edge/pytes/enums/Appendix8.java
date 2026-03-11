package io.openems.edge.pytes.enums;

import io.openems.common.types.OptionsEnum;

// ToDo
public enum Appendix8 implements OptionsEnum {
	UNDEFINED(-1, "Undefined"),
	UPS_MODE(0, "UPS Mode (off-grid, no alarm)"),
    SELF_USE_MODE(1, "Self-Use Mode"),
    TIME_OF_USE_SELF_USE(2, "Time of Use in Self-Use Mode"),
    FEED_IN_PRIORITY(3, "Feed-in Priority Mode"),
    TIME_OF_USE_FEED_IN(4, "Time of Use in Feed-in Priority Mode"),
    BACKUP_MODE(5, "Backup Mode"),
    OFF_GRID_MODE(6, "Off-Grid Mode"),
    REMOTE_BATT_CONTROL(7, "Remote Battery Charge/Discharge Mode"),
    PASSIVE_MODE(8, "Passive Mode");
	

	private final int value;
	private final String name;

	Appendix8(int value, String name) {
		this.value = value;
		this.name = name;
	}

	@Override public int getValue() { return this.value; }
	@Override public String getName() { return this.name; }
	@Override public OptionsEnum getUndefined() { return UNDEFINED; }
}
