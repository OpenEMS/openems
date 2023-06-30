package io.openems.backend.uiwebsocket.impl;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.backend.common.metadata.User;
import io.openems.backend.uiwebsocket.jsonrpc.notification.LogMessageNotification;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcNotification;

public class OnNotification implements io.openems.common.websocket.OnNotification {

	private final Logger log = LoggerFactory.getLogger(OnNotification.class);
	private final UiWebsocketImpl parent;

	public OnNotification(UiWebsocketImpl parent) {
		this.parent = parent;
	}

	@Override
	public void run(WebSocket ws, JsonrpcNotification notification) throws OpenemsNamedException {
		WsData wsData = ws.getAttachment();
		User user = null;
		try {
			user = this.parent.assertUser(wsData, notification);
		} catch (OpenemsNamedException e) {
			// ignore
		}

		switch (notification.getMethod()) {
		case LogMessageNotification.METHOD:
			if (user == null) {
				// User is not authenticated!
				this.handleUnauthenticatedLogMessageNotification(LogMessageNotification.from(notification));
				return;

			} else {
				this.handleLogMessageNotification(user, LogMessageNotification.from(notification));
				return;
			}
		}

		this.parent.logWarn(this.log, "Unhandled Notification: " + notification);
	}

	/**
	 * Handles a {@link LogMessageNotification} with not-authenticated user. Logs
	 * given message from request.
	 *
	 * @param notification the {@link LogMessageNotification}
	 */
	private void handleUnauthenticatedLogMessageNotification(LogMessageNotification notification) {
		this.parent.logInfo(this.log, "User [NOT AUTHENTICATED] " //
				+ notification.level.getName() + "-Message: " //
				+ notification.msg);
	}

	/**
	 * Handles a {@link LogMessageNotification}. Logs given message from request.
	 *
	 * @param user         the {@link User}r
	 * @param notification the {@link LogMessageNotification}
	 */
	private void handleLogMessageNotification(User user, LogMessageNotification notification) {
		this.parent.logInfo(this.log, "User [" + user.getId() + ":" + user.getName() + "] " //
				+ notification.level.getName() + "-Message: " //
				+ notification.msg);
	}
}
