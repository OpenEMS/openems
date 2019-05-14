package io.openems.edge.battery.renaultzoe;

import io.openems.common.types.OptionsEnum;

public enum BatteryModel implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	MODEL_0(0, "Model 0"), //
	MODEL_1(1, "Model 1"), //
	MODEL_2(2, "Model 2"), //
	MODEL_3(3, "Model 3"), //
	MODEL_4(4, "Model 4"), //
	MODEL_5(5, "Model 5"), //
	MODEL_6(6, "Model 6"), //
	MODEL_7(7, "Model 7"), //
	MODEL_8(8, "Model 8"), //
	MODEL_9(9, "Model 9"), //
	MODEL_10(10, "Model 10"); //
	
	private int value;
	private String name;

	private BatteryModel(int value, String name) {
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
