package io.openems.edge.evcs.heidelberg.energy;

import io.openems.edge.common.channel.value.Value;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.Status;

public class StatusConverter {

	private final Evcs evcs;
	private boolean vehicleConnected = false;

	public StatusConverter(Evcs evcs) {
		this.evcs = evcs;
	}

	/**
	 * Converts Heidelberg Energy specific state into OpenEMS state.
	 *
	 * @param value the Heidelberg Energy specific state.
	 */
	public void applyHeidelbergStatus(Value<?> value) {
		if (!value.isDefined()) {
			this.evcs._setStatus(Status.UNDEFINED);
			return;
		}
		var heidelbergState = (int) value.get();
		this.vehicleConnected = heidelbergState > 3;

		if (heidelbergState < 2) {
			this.evcs._setStatus(Status.UNDEFINED);
		}

		if (heidelbergState < 3 || heidelbergState == 10) {
			this.evcs._setStatus(Status.NOT_READY_FOR_CHARGING);
			return;
		}
		if (heidelbergState < 6) {
			this.evcs._setStatus(Status.READY_FOR_CHARGING);

			return;
		}
		if (heidelbergState < 8) {
			this.evcs._setStatus(Status.CHARGING);
			return;
		}
		if (heidelbergState < 11) {
			this.evcs._setStatus(Status.ERROR);
			return;
		}
		this.evcs._setStatus(Status.UNDEFINED);
	}

	public boolean isVehicleConnected() {
		return this.vehicleConnected;
	}

}