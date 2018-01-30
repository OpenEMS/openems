package io.openems.backend.browserwebsocket.provider.internal;

import io.openems.common.session.Session;

public class BrowserSession extends Session<BrowserSessionData> {

	protected BrowserSession(String token, BrowserSessionData data) {
		super(token, data);
	}

	@Override
	public String toString() {
		return "User [" + getData().getUserName() + "] Session [" + getData().getOdooSessionId().orElse("") + "]";
	}
}
