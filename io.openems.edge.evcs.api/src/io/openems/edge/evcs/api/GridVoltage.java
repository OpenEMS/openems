package io.openems.edge.evcs.api;

public enum GridVoltage {
    V_220_HZ_50(220),
    V_230_HZ_50(230),
    V_240_HZ_50(240),
    V_100_HZ_60(100),
    V_110_HZ_60(110),
    V_115_HZ_60(115),
    V_120_HZ_60(120),
    V_127_HZ_60(127),
    V_220_HZ_60(220),
    V_230_HZ_60(230),
    V_240_HZ_60(240),
    V_100_HZ_50(100),
    V_110_HZ_50(110),
    V_115_HZ_50(115),
    V_127_HZ_50(127);
    private final int value;


    private GridVoltage(int value) {
        this.value = value;
    }

    /**
     * Get Voltage of a given Region Value.
     *
     * @return Value
     */
    public int getValue() {
        return this.value;
    }
}
