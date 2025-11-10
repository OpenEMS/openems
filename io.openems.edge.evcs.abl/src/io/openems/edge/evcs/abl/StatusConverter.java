package io.openems.edge.evcs.abl;

import io.openems.edge.evcs.api.Status;

/**
 * Helper class to update the channels
 * {@link io.openems.edge.evcs.api.Evcs.ChannelId#PHASES PHASES},
 * {@link io.openems.edge.evcs.api.Evcs.ChannelId#STATUS STATUS} and
 * {@link io.openems.edge.evcs.api.Evcs.ChannelId#ENERGY_SESSION ENERGY_SESSION}
 * from the values given by the ABL.
 */
public class StatusConverter {

	private final EvcsAblImpl parent;

	private boolean vehicleConnected = false;

	protected StatusConverter(EvcsAblImpl parent) {
		this.parent = parent;
	}

	/**
	 * Converts ABL emh4 specific state into OpenEMS state.
	 *
	 */
	public void convertAblStatus() {
		switch (this.parent.getChargePointState()) {
		case UNDEFINED -> {
			this.parent._setStatus(Status.UNDEFINED);
			this.vehicleConnected = false;
		}
		case BLOCKED, WAITING_FOR_EV, COMPLETELY_BLOCKED, BOOTING -> {
			this.parent._setStatus(Status.NOT_READY_FOR_CHARGING);
			this.vehicleConnected = false;
		}
		case RESERVED, READY_FOR_CHARGING, AUTHENTICATING -> {
			this.parent._setStatus(Status.READY_FOR_CHARGING);
			this.vehicleConnected = true;
		}
		case CHARGING -> {
			this.parent._setStatus(Status.CHARGING);
			this.vehicleConnected = true;
		}
		case CHARGING_STOPPED, AUTHENTICATION_FAILED -> {
			this.parent._setStatus(Status.CHARGING_REJECTED);
			this.vehicleConnected = true;
		}
		default -> {
			this.parent._setStatus(Status.ERROR);
			this.vehicleConnected = false;
		}
		}
	}

	public boolean isVehicleConnected() {
		return this.vehicleConnected;
	}
}
