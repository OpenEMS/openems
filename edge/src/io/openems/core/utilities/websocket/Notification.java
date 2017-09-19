package io.openems.core.utilities.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.websocket.NotificationStatus;

public class Notification {
	private Logger log = LoggerFactory.getLogger(Notification.class);

	private String message = "";
	private NotificationStatus type = NotificationStatus.INFO;

	public Notification(NotificationStatus type, String message) {
		set(type, message);
	}

	public void set(String message) {
		this.message = message;
	}

	public void set(NotificationStatus type, String message) {
		this.type = type;
		this.message = message;
	}

	public void send() {
		// log message to syslog
		switch (type) {
		case INFO:
		case SUCCESS:
			log.info(this.message);
			break;
		case ERROR:
			log.error(this.message);
			break;
		case WARNING:
			log.warn(this.message);
			break;
		case LOG:
			// ignore
		}
		// TODO WebsocketServer.broadcastNotification(this);
	}

	public static void send(NotificationStatus type, String message) {
		Notification notification = new Notification(type, message);
		notification.send();
	}

	public NotificationStatus getType() {
		return type;
	}

	public String getMessage() {
		return message;
	}
}
