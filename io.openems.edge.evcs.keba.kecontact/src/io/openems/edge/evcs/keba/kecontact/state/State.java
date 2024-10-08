package io.openems.edge.evcs.keba.kecontact.state;

import io.openems.edge.evcs.keba.kecontact.EvcsKebaKeContactImpl;

/**
 * Interface representing a state in the EVCS Keba KeContact phase switch
 * handler.
 */
public interface State {

	/**
	 * Handles the power application logic for the current state.
	 *
	 * @param power   The power to be applied.
	 * @param context The context of the EVCS Keba KeContact implementation.
	 */
	void handlePower(int power, EvcsKebaKeContactImpl context);

	/**
	 * Switches the phase in the current state.
	 *
	 * @param context The context of the EVCS Keba KeContact implementation.
	 */
	void switchPhase(EvcsKebaKeContactImpl context);
}
