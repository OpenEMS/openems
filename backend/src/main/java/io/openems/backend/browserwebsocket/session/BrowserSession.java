package io.openems.backend.browserwebsocket.session;

import io.openems.common.session.Session;

public class BrowserSession extends Session<BrowserSessionData> {

	protected BrowserSession(String token, BrowserSessionData data) {
		super(token, data);
	}

	@Override
	public String toString() {
		return "User [" + getData().getUserId() + "] Session [" + getData().getOdooSessionId() + "]";
	}
}
