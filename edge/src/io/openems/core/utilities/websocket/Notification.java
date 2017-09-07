package io.openems.core.utilities.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Notification {
	private Logger log = LoggerFactory.getLogger(Notification.class);

	private String message = "";
	private NotificationType type = NotificationType.INFO;

	public Notification(NotificationType type, String message) {
		set(type, message);
	}

	public void set(String message) {
		this.message = message;
	}

	public void set(NotificationType type, String message) {
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
		}
		// TODO WebsocketServer.broadcastNotification(this);
	}

	public static void send(NotificationType type, String message) {
		Notification notification = new Notification(type, message);
		notification.send();
	}

	public NotificationType getType() {
		return type;
	}

	public String getMessage() {
		return message;
	}
}
