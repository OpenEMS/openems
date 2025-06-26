package io.openems.edge.evcs.webasto.unite;

import io.openems.edge.evcs.api.Status;

// TODO: Can also be done by registering onSetNextValue listeners on the depending channel in the WebastoImpl.
public class WebastoReadHandler {

	private final EvcsWebastoUniteImpl parent;

	protected WebastoReadHandler(EvcsWebastoUniteImpl parent) {
		this.parent = parent;
	}

	protected void run() {
		this.setStatus();
	}

	private void setStatus() {
		this.parent._setStatus(switch (this.parent.getChargePointState()) {
		case 0 -> Status.NOT_READY_FOR_CHARGING;
		case 1 -> Status.READY_FOR_CHARGING;
		case 2 -> Status.CHARGING;
		case 3, 4, 5 -> Status.CHARGING_REJECTED;
		// TODO Check if this state is also reached while paused
		case 7, 8 -> Status.ERROR;
		default -> null;
		});
	}

}
