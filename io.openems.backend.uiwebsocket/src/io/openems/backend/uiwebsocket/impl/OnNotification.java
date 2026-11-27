package io.openems.backend.uiwebsocket.impl;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;

import io.openems.backend.common.component.AbstractOpenemsBackendComponent;
import io.openems.backend.common.metadata.User;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.jsonrpc.notification.LogMessageNotification;

public class OnNotification implements io.openems.common.websocket.OnNotification {

	private final Logger log;
	private final UiWebsocketImpl parent;

	public OnNotification(UiWebsocketImpl parent) {
		this.parent = parent;
		this.log = AbstractOpenemsBackendComponent.getComponentLogger(this.getClass(), parent);
	}

	@Override
	public void accept(WebSocket ws, JsonrpcNotification notification) throws OpenemsNamedException {
		WsData wsData = ws.getAttachment();
		if (!wsData.checkLimiter(notification.getMethod())) {
			this.log.debug("Too Many Requests! Notification [{}] discarded by Rate-Limiter.", notification.getMethod());
			return;
		}

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

		this.log.warn("Unhandled Notification: {}", notification);
	}

	/**
	 * Handles a {@link LogMessageNotification} with not-authenticated user. Logs
	 * given message from request.
	 *
	 * @param notification the {@link LogMessageNotification}
	 */
	private void handleUnauthenticatedLogMessageNotification(LogMessageNotification notification) {
		this.log.info("User [NOT AUTHENTICATED] {}-Message: {}", //
				notification.level.getName(), notification.msg);
	}

	/**
	 * Handles a {@link LogMessageNotification}. Logs given message from request.
	 *
	 * @param user         the {@link User}r
	 * @param notification the {@link LogMessageNotification}
	 */
	private void handleLogMessageNotification(User user, LogMessageNotification notification) {
		this.log.info("User [{}:{}] {}-Message: {}", //
				user.getId(), user.getName(), notification.level.getName(), notification.msg);
	}
}
