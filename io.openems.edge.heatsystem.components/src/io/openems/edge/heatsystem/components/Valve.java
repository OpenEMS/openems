package io.openems.edge.heatsystem.components;

/**
 * The Valve interface, an expansion of a HeatsystemComponent.
 * It allows other Components / Controller to force Open/Close or reset the Valve.
 */
public interface Valve extends HeatsystemComponent {

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

    /**
     * This checks if the SetPointPowerLevel (FuturePowerLevel) is reached, by checking the current PowerLevel + a Small Buffer.
     *
     * @return if the PowerLevel is Reached.
     */
    boolean powerLevelReached();

    /**
     * Checks if the Valve is Changing atm.
     * @return true if the valve is changing (opening/closing)
     */
    boolean isChanging();

    /**
     * Resets the Valve -> Force Closes it.
     * Can be useful if something weird is happening or fi the Valve was deactivated for some reason and got reactivated again.
     */
    void reset();

}
