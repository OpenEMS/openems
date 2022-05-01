package io.openems.edge.controller.heating.room;

import java.time.Instant;

import io.openems.edge.controller.heating.room.RoomHeatingControllerImpl.Switch;

public class RelayState {

	public final Instant lastChange;
	public final Switch target;

	public RelayState(Instant lastChange, Switch target) {
		this.lastChange = lastChange;
		this.target = target;
	}
}