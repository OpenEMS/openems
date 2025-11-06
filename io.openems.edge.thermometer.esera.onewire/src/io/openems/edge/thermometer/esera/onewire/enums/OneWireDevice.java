package io.openems.edge.thermometer.esera.onewire.enums;

public enum OneWireDevice {

    OneWireThermometer1("OneWire Thermometer 1", 40100),
    OneWireThermometer2("OneWire Thermometer 2", 40200),
    OneWireThermometer3("OneWire Thermometer 3", 40300),
    OneWireThermometer4("OneWire Thermometer 4", 40400),
    OneWireThermometer5("OneWire Thermometer 5", 40500),
    OneWireThermometer6("OneWire Thermometer 6", 40600),
    OneWireThermometer7("OneWire Thermometer 7", 40700),
    OneWireThermometer8("OneWire Thermometer 8", 40800),
    OneWireThermometer9("OneWire Thermometer 9", 40900),
    OneWireThermometer10("OneWire Thermometer 10", 41000),
	OneWireThermometer11("OneWire Thermometer 11", 41100),
	OneWireThermometer12("OneWire Thermometer 12", 41200),
	OneWireThermometer13("OneWire Thermometer 13", 41300),
	OneWireThermometer14("OneWire Thermometer 14", 41400),
	OneWireThermometer15("OneWire Thermometer 15", 41500),
	OneWireThermometer16("OneWire Thermometer 16", 41600),
	OneWireThermometer17("OneWire Thermometer 17", 41700),
	OneWireThermometer18("OneWire Thermometer 18", 41800),
	OneWireThermometer19("OneWire Thermometer 19", 41900),
	OneWireThermometer20("OneWire Thermometer 20", 42000);
	
    private final int modbusAddress;
    private final String name;

    OneWireDevice(String name, int modbusAddress) {
        this.name = name;
        this.modbusAddress = modbusAddress;
    }

    public int getModbusAddress() {
        return this.modbusAddress;
    }

    public String getName() {
        return this.name;
    }
}
