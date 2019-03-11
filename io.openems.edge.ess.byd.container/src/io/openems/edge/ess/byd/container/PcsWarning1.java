package io.openems.edge.ess.byd.container;

import io.openems.edge.common.channel.doc.OptionsEnum;

public enum PcsWarning1 implements OptionsEnum {
	UNDEFINED(-1, "Undefined"),
	DC_PRE_CHARGING_CHECKBACK(0, "DC-pre-charging-checkback"),
	AC_PRE_CHARGING_CHECKBACK(8, "AC-pre-charging-checkback"),
	AC_MAIN_CHECKBACK(16, "AC-main-checkback"),
	AC_CIRCUIT_BREAKER_CHECKBACK(32, "AC-cirbuit-breaker-checkback"),
	CONTAINER_DOOR_OPEN(4096, "Container-door-open"),
	AC_CIRCUIT_BREAKER_NOT_CLOSED(16384, "AC-circuit-breaker-not-closed"); 
	//0, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384, 32768
	//0, 1, 2, 3,  4,  5,  6,   7,    8,  9,   10,   11,   12,   13,    14,    15
	private final int value;
	private final String name;

	private PcsWarning1(int value, String name) {
		this.value = value;
		this.name = name;
	}

	@Override
	public int getValue() {
		return value;
	}

	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}

}
