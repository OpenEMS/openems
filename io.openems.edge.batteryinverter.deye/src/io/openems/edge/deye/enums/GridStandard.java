package io.openems.edge.deye.enums;
import io.openems.common.types.OptionsEnum;

public enum GridStandard implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	CHINA(1, "China grid standard"), //
	BRAZIL(2, "Brazil grid standard"), //
	INDIA(3, "India grid standard"), //
	EN50438(4, "EN50438 grid standard"), //
	OTHER(4, "OTHER grid standard"); //
	
	private final int value;
	private final String name;

	private GridStandard(int value, String name) {
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

