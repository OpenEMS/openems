package io.openems.edge.heater.api;

public enum HeaterState {
    OFFLINE, AWAIT, PREHEAT, RUNNING, ERROR, WARNING, UNDEFINED;


    public static boolean contains(String requestetState) {
        for (HeaterState heaterState : HeaterState.values()) {
            if (heaterState.name().equals(requestetState)) {
                return true;
            }
        }
        return false;
    }
}
