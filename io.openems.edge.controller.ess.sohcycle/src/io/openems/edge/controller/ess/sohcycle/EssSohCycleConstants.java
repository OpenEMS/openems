package io.openems.edge.controller.ess.sohcycle;

/**
 * Internal constants for SoH Cycle controller. These are not exposed via
 * configuration to avoid misconfiguration; the controller currently always
 * operates with these values.
 */
public final class EssSohCycleConstants {

    public static final int MAX_SOC = 100;
    public static final int MIN_SOC = 0;
    public static final int MAX_CELL_VOLTAGE_DIFFERENCE_MV = 100;
    public static final int WAIT_DURATION_MINUTES = 30;
    /**
     * Charge/discharge C-rate as a fraction of the battery capacity per hour.
     *
     * <p>
     * For example, a value of {@code 0.2} corresponds to 20% of the battery
     * capacity charged or discharged per hour (0.2C).
     */
    public static final float C_RATE = 0.2f; // 0.2C

    private EssSohCycleConstants() {
    }
}

