package io.openems.edge.consolinno.modbus.configurator;

public enum MinimumFirmwareVersion {
    VERSION(78);
    private final int value;

    private MinimumFirmwareVersion(int value) {
        this.value = value;

    }

    /**
     * Get Minimum Version number.
     *
     * @return Value
     */
    public int getValue() {
        return this.value;
    }
}


