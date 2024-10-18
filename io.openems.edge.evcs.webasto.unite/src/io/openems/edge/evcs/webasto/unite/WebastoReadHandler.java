package io.openems.edge.evcs.webasto.unite;

import static io.openems.edge.evcs.api.Evcs.evaluatePhaseCount;

import io.openems.edge.evcs.api.Status;

// TODO: Can also be done by registering onSetNextValue listeners on the depending channel in the WebastoImpl.
public class WebastoReadHandler {

	private final EvcsWebastoUniteImpl parent;

	protected WebastoReadHandler(EvcsWebastoUniteImpl parent) {
		this.parent = parent;
	}

	protected void run() {
		this.setPhaseCount();
		this.setStatus();
	}

	private void setStatus() {
		this.parent._setStatus(switch (this.parent.getChargePointState()) {
		case 0 -> Status.NOT_READY_FOR_CHARGING;
		case 1 -> Status.READY_FOR_CHARGING;
		case 2 -> Status.CHARGING;
		case 3, 4 -> Status.CHARGING_REJECTED;
		case 5 -> Status.CHARGING_FINISHED;
		// TODO Check if this state is also reached while paused
		case 7, 8 -> Status.ERROR;
		default -> null;
		});
	}

	/**
	 * Writes the Amount of Phases in the Phase channel.
	 */
	private void setPhaseCount() {
		this.parent._setPhases(evaluatePhaseCount(//
				this.parent.getActivePowerL1().get(), //
				this.parent.getActivePowerL2().get(), //
				this.parent.getActivePowerL3().get()));
	}
}
