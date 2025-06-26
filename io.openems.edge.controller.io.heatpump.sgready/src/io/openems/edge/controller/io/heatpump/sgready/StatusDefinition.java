package io.openems.edge.controller.io.heatpump.sgready;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.timedata.api.utils.CalculateActiveTime;

/**
 * State Definition.
 *
 * <p>
 * Stores information about the time, it was switched on and provides an method
 * to set this state.
 */
public class StatusDefinition {

	private final Status status;
	private final ControllerIoHeatPumpSgReadyImpl parent;
	private final CalculateActiveTime calculateActiveTime;

	public StatusDefinition(ControllerIoHeatPumpSgReadyImpl parent, Status status,
			ControllerIoHeatPumpSgReady.ChannelId activeTimeChannelId) {
		this.parent = parent;
		this.status = status;
		this.calculateActiveTime = new CalculateActiveTime(this.parent, activeTimeChannelId);
	}

	/**
	 * Switch on the state/it's referring relays.
	 *
	 * <p>
	 * Forward its state to the parent changeState method, to set the corresponding
	 * relay outputs and the present state, if the state is not already active.
	 *
	 * @throws OpenemsNamedException    on error
	 * @throws IllegalArgumentException on error
	 */
	protected void switchOn() throws IllegalArgumentException, OpenemsNamedException {
		if (this.isActive()) {
			return;
		}
		this.parent.changeState(this.status);
	}

	/**
	 * Updates the time channel depending if the state is active or not.
	 */
	protected void updateActiveTime() {
		this.calculateActiveTime.update(this.isActive());
	}

	/**
	 * Returns if this state is the active state.
	 *
	 * @return Is this state active.
	 */
	protected boolean isActive() {
		if (this.parent.activeState == this.status) {
			return true;
		}
		return false;
	}
}
