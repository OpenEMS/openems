package io.openems.edge.evcs.goe.api;

import io.openems.edge.common.channel.value.Value;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.Status;

public class StatusConverter {

	private Evcs evcs;
	private boolean vehicleConnected = false;

	public StatusConverter(Evcs evcs) {
		this.evcs = evcs;
	}

	/**
	 * Converts goe specific state into OpenEMS state.
	 *
	 * @param value the goe specific state.
	 */
	public void applyGoeStatus(Value<?> value) {
		if (!value.isDefined()) {
			this.evcs._setStatus(Status.UNDEFINED);
			return;
		}
		var goeState = (int) value.get();
		this.vehicleConnected = goeState > 1;

		switch (goeState) {
		case 0 -> this.evcs._setStatus(Status.ERROR);
		case 1 -> this.evcs._setStatus(Status.NOT_READY_FOR_CHARGING);
		case 2 -> this.evcs._setStatus(Status.CHARGING);
		case 3, 4 -> this.evcs._setStatus(Status.READY_FOR_CHARGING);
		default -> this.evcs._setStatus(Status.UNDEFINED);
		}
	}

	public boolean isVehicleConnected() {
		return this.vehicleConnected;
	}

}