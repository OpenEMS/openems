package io.openems.edge.thermometer.esera.onewire.enums;

import io.openems.common.types.OptionsEnum;

/*
 * Attention!. Differs from ChargeStateEss
 * 
 * */
public enum OneWireDevice implements OptionsEnum {

	UNDEFINED("undefined", -1), //
	OneWireThermometer1("OneWire Thermometer 1", 40100), //
	OneWireThermometer2("OneWire Thermometer 2", 40200), //
	OneWireThermometer3("OneWire Thermometer 3", 40300), //
	OneWireThermometer4("OneWire Thermometer 4", 40400), //
	OneWireThermometer5("OneWire Thermometer 5", 40500), //
	OneWireThermometer6("OneWire Thermometer 6", 40600), //
	OneWireThermometer7("OneWire Thermometer 7", 40700), //
	OneWireThermometer8("OneWire Thermometer 8", 40800), //
	OneWireThermometer9("OneWire Thermometer 9", 40900), //
	OneWireThermometer10("OneWire Thermometer 10", 41000),//
	;

	private final int modbusAddress;
	private final String name;

	private OneWireDevice(String name, int modbusAddress) {
		this.name = name;
		this.modbusAddress = modbusAddress;

	}

	@Override
	public int getValue() {
		return ordinal();
	}

	@Override
	public String getName() {
		return this.name;
	}
	
	public int getModbusAddress() {
		return this.modbusAddress;
	}	

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}

}
