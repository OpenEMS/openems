package io.openems.edge.battery.api;

public interface BatteryInhibitable {
	/**
	 * Sets the main contactor to an unlocked state. This method is used to indicate
	 * whether the main contactor can be unlocked or not.
	 * 
	 * <p>
	 * This method allows unlocking the main contactor, which is an electrical
	 * switch used to control the flow of battery current.
	 * </p>
	 * 
	 * <p>
	 * When the main contactor is unlocked, it lets the battery operate on
	 * {@link Battery.ChannelId#MAIN_CONTACTOR}. And when the main contactor turned
	 * on, it enables the flow of current through the circuit.
	 * </p>
	 * 
	 * @param value {@code true} if the main contactor switch can be unlocked,
	 *              {@code false} otherwise.
	 */
	public void setMainContactorUnlocked(boolean value);
}