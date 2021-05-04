package io.openems.edge.heatsystem.components;

/**
 * The Pump is an extension of the HeatsystemComponent and provides the ability to set a PowerLevel by other components.
 * Otherwise it functions as a normal HeatsystemComponent.
 */
public interface Pump extends HeatsystemComponent {
    /**
     * Sets the PowerLevel of the Pump. Values between 0-100% can be applied.
     *
     * @param percent the PowerLevel the Pump should be set to.
     */
    void setPowerLevel(double percent);
}
