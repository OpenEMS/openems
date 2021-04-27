package io.openems.edge.consolinno.modbus.configurator.api;

public enum Error {
    //65535 = 0xFFFF
    ERROR(65535);
    private final int value;

    private Error(int value) {
        this.value = value;

    }

    /**
     * Get Error Value.
     *
     * @return Value
     */
    public int getValue() {
        return this.value;
    }
}
