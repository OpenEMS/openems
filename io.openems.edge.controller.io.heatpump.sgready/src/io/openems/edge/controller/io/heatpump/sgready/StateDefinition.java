package io.openems.edge.controller.io.heatpump.sgready;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.timedata.api.utils.CalculateActiveTime;

/**
 * State Definition.
 * <p>
 * Stores information about the time, it was switched on and provides an method
 * to set this state.
 */
public class StateDefinition {

	private final Status status;
	private final HeatPumpImpl parent;
	private final CalculateActiveTime calculateActiveTime;

	public StateDefinition(HeatPumpImpl parent, Status status, HeatPump.ChannelId activeTimeChannelId) {
		this.parent = parent;
		this.status = status;
		this.calculateActiveTime = new CalculateActiveTime(this.parent, this.parent, this.parent, activeTimeChannelId);
	}

	/**
	 * Switch the corresponding relay outputs to set the present state and sets the active state.
	 * 
	 * @throws OpenemsNamedException
	 * @throws IllegalArgumentException
	 */
	protected void switchOn() throws IllegalArgumentException, OpenemsNamedException {
		if(isActive()) {
			return;
		}
		this.parent.setOutputs(status.getOutput1(), status.getOutput2());
		this.parent.stateChanged(this.status);
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
