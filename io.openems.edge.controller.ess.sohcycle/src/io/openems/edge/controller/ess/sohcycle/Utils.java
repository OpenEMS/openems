package io.openems.edge.controller.ess.sohcycle;

public class Utils {

    private Utils() {
        // Utils class
    }

    /**
     * Rounds a float value to 2 decimal places.
     *
     * @param value the float value to round
     * @return the rounded float value
     */
    public static float round2(float value) {
        return Math.round(value * 100f) / 100f;
    }
}
