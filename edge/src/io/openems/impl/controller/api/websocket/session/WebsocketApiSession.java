package io.openems.impl.controller.api.websocket.session;

import io.openems.common.session.Session;

public class WebsocketApiSession extends Session<WebsocketApiSessionData> {

	protected WebsocketApiSession(String token, WebsocketApiSessionData data) {
		super(token, data);
	}

}
