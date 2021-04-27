package io.openems.edge.controller.heatnetwork.communication.api;

/**
 * Types of FallbackHandling.
 */
public enum FallbackHandling {
    HEAT, OPEN, CLOSE, DEFAULT;

    public static boolean contains(String handling) {
        for (FallbackHandling fallbackHandling : FallbackHandling.values()) {
            if (fallbackHandling.name().equals(handling)) {
                return true;
            }
        }
        return false;
    }
}
