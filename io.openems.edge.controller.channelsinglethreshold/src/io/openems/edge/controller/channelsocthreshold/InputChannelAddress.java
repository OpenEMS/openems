package io.openems.edge.controller.channelsocthreshold;

import io.openems.common.types.OptionsEnum;

public enum InputChannelAddress implements OptionsEnum {
	SOC(0, "_sum/EssSoc"); //

	private final int value;
	private final String name;

	private InputChannelAddress(int value, String name) {
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
		// TODO Auto-generated method stub
		return SOC;
	}
}