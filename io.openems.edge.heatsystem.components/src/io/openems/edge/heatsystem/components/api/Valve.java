package io.openems.edge.heatsystem.components.api;

public interface Valve extends PassingForPid {

    boolean readyToChange();

    boolean changeByPercentage(double percentage);

    /**
     * Closes the valve completely, overriding any current valve operation.
     * If a closed valve is all you need, better use this instead of changeByPercentage(-100) as you do not need
     * to check if the valve is busy or not.
     */

    void forceClose();

    /**
     * Opens the valve completely, overriding any current valve operation.
     * If an open valve is all you need, better use this instead of changeByPercentage(100) as you do not need
     * to check if the valve is busy or not.
     */

    void forceOpen();

    void updatePowerLevel();

    boolean powerLevelReached();

    boolean isChanging();

    void reset();

    boolean shouldReset();

    //void shutdownRelays();
}
