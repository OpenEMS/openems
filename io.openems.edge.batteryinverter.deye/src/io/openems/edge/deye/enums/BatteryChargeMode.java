package io.openems.edge.deye.enums;
import io.openems.common.types.OptionsEnum;

public enum BatteryChargeMode implements OptionsEnum {
    UNDEFINED(-1, "Undefined"),
    LEAD(0, "Lead battery. Four stage charging"),
    Lithium(1, "Lithium battery");

	private final int value;
	private final String name;

	private BatteryChargeMode(int value, String name) {
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


