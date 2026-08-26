package io.openems.edge.controller.io.heatingelement;

/**
 * A class for the tests to simulate the channel ACTIVE_CONSUMPTION_POWER from the meter.
 */
public class EnergyTracker {
	private float currentWh = 0;
	
	/**
	 * Adds the energy consumed depending on the parameters.
	 * @param powerW the power in Watt.
	 * @param durationSeconds the duration the power has in seconds.
	 * @return the current energy in Wh.
	 */
	public long add(int powerW, int durationSeconds) {
		this.currentWh += (powerW * durationSeconds) / 3600F;
		return (long) this.currentWh;
	}
	
	/**
	 * Gets the current energy in Wh.
	 * @return the current energy.
	 */
	public long getCurrentWh() {
		return (long) this.currentWh;
	}
	
	/**
	 * Resets the current Energy to 0.
	 * @return 0
	 */
	public long reset() {
		this.currentWh = 0;
		return 0;
	}
}
