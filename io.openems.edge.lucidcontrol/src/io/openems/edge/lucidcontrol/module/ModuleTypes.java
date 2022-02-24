package io.openems.edge.lucidcontrol.module;

public enum ModuleTypes {
    VOLTAGE_5(5), VOLTAGE_10(10), VOLTAGE_24(24), VOLTAGE_PLUS_MINUS_5(5), VOLTAGE_PLUS_MINUS_10(10);


    final int voltage;

    ModuleTypes(int voltValue) {
        this.voltage = voltValue;
    }


    /**
     * Gets the VoltValue of the Enum.
     *
     * @return the voltageValue
     */
    public int getVoltage() {
        return this.voltage;
    }
}
