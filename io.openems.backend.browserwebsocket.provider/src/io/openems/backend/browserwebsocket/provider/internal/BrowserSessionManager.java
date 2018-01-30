package io.openems.backend.browserwebsocket.provider.internal;

import io.openems.common.session.SessionManager;

public class BrowserSessionManager extends SessionManager<BrowserSession, BrowserSessionData> {

	@Override
	public BrowserSession _createNewSession(String token, BrowserSessionData data) {
		return new BrowserSession(token, data);
	}
}
