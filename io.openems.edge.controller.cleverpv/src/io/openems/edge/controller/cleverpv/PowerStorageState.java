package io.openems.edge.controller.cleverpv;

public enum PowerStorageState {
    IDLE(0),
    DISCHARGING(1),
    DISABLED(2),
    CHARGING(3);

    public final int value;

    PowerStorageState(int value) {
        this.value = value;
    }

    /**
     * Maps the given power value to a PowerStorageState.
     *
     * @param power the ESS power
     * @return the corresponding PowerStorageState
     */
    public static PowerStorageState fromPower(Integer power) {
        if (power == null) {
            return IDLE;
        }
        if (power > 0) {
            return CHARGING;
        }
        if (power < 0) {
            return DISCHARGING;
        }
        return IDLE;
    }
}

