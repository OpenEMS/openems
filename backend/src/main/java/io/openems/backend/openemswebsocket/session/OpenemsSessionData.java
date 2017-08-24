package io.openems.backend.openemswebsocket.session;

import io.openems.backend.odoo.device.Device;
import io.openems.common.session.SessionData;

public class OpenemsSessionData extends SessionData {
	private final Device device;

	public OpenemsSessionData(Device device) {
		this.device = device;
	}

	public Device getDevice() {
		return device;
	}
}
