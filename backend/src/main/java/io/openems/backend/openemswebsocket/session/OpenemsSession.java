package io.openems.backend.openemswebsocket.session;

import io.openems.common.session.Session;

public class OpenemsSession extends Session<OpenemsSessionData> {

	protected OpenemsSession(String token, OpenemsSessionData data) {
		super(token, data);
	}

	@Override
	public String toString() {
		return "Device [" + getData().getDevices().getNamesString() + "]";
	}
}
