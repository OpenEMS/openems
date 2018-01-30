package io.openems.backend.openemswebsocket.provider.internal;

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
