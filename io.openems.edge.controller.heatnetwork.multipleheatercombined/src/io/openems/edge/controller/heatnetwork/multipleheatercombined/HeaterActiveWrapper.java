package io.openems.edge.controller.heatnetwork.multipleheatercombined;

/**
 * This class is a Wrapper to to save the active value AND check if this heater should always run on Full power.
 */
class HeaterActiveWrapper {

    private boolean active;


    HeaterActiveWrapper(boolean active) {
        this.active = active;
    }

    HeaterActiveWrapper() {
        this(false);
    }

    boolean isActive() {
        return active;
    }

    void setActive(boolean active) {
        this.active = active;
    }

}
