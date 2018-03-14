package io.openems.common.websocket;

import org.slf4j.Logger;

public enum Notification {
	ERROR(1, NotificationType.ERROR, "Error: %s"), //
	EDGE_CONNECTION_ClOSED(100, NotificationType.WARNING, "Connection [%s] was interrupted"), //
	EDGE_CONNECTION_OPENED(101, NotificationType.INFO, "Connection [%s] was established"), //
	EDGE_UNABLE_TO_FORWARD(102, NotificationType.ERROR, "Unable to forward command to [%s]: %s"), //
	EDGE_AUTHENTICATION_BY_TOKEN_FAILED(103, NotificationType.INFO, "Authentication by token [%s] failed"), //
	EDGE_CHANNEL_UPDATE_SUCCESS(104, NotificationType.SUCCESS, "Configuration successfully updated [%s]"), //
	EDGE_CHANNEL_UPDATE_FAILED(105, NotificationType.ERROR, "Configuration update failed [%s]: %s"), //
	BACKEND_NOT_ALLOWED(106, NotificationType.ERROR, "The operation [%s] is not allowed via Backend"), //
	EDGE_CHANNEL_UPDATE_TIMEOUT(107, NotificationType.INFO, "Channel setting [%s] timed out."), //
	BACKEND_FORWARD_TO_EDGE_NOT_ALLOWED(108, NotificationType.ERROR,
			"Message forward to Edge [%s] is not allowed for user [%s]."), //
	BACKEND_UNABLE_TO_READ_EDGE_DETAILS(109, NotificationType.ERROR, "Unable to read details for Edge [ID:%s]: %s"), //
	UNABLE_TO_QUERY_HISTORIC_DATA(110, NotificationType.ERROR, "Unable to query historic data: %s"), //
	BACKEND_UNABLE_TO_READ_USER_DETAILS(111, NotificationType.ERROR, "Unable to read details for User [ID:%s]"), //
	METADATA_ERROR(112, NotificationType.ERROR, "Metadata operation failed: %s"), //
	UNKNOWN_MESSAGE(113, NotificationType.WARNING, "Unknown message. Source [%s]. Message: %s"), //
	SUBSCRIBE_CURRENT_DATA_FAILED(114, NotificationType.ERROR, "Subscription to current data failed: %s"), //
	CHANNEL_ACCESS_DENIED(115, NotificationType.WARNING, "Access to channel [%s] was denied for User [%s]"), //
	VALUE_CONVERSION_FAILED(116, NotificationType.ERROR, "Channel [%s] conversion failed: %s"), //
	CHANNEL_NOT_FOUND(117, NotificationType.ERROR, "Channel [%s] not found."), //
	UNABLE_TO_SUBSCRIBE_TO_LOG(118, NotificationType.ERROR, "Unable to subscribe to log: %s"), //
	UNABLE_TO_EXECUTE_SYSTEM_COMMAND(119, NotificationType.ERROR, "Unable to execute system command: %s"), //
	NO_TIMEDATA_SOURCE_AVAILABLE(120, NotificationType.ERROR, "No timedata source available"),
	UNABLE_TO_READ_CURRENT_CONFIG(121, NotificationType.ERROR, "Unable to read current config: %s");

	private final int value;
	private final NotificationType status;
	private final String message;

	private Notification(int value, NotificationType status, String message) {
		this.value = value;
		this.status = status;
		this.message = message;
	}

	public int getValue() {
		return value;
	}

	public NotificationType getType() {
		return status;
	}

	public String getMessage() {
		return message;
	}

	public void writeToLog(Logger log, Object... params) {
		String message = String.format(this.message, params);
		String logMessage = "Notification [" + this.value + "]: " + message;
		switch (this.status) {
		case INFO:
		case LOG:
		case SUCCESS:
			log.info(logMessage);
			break;
		case ERROR:
			log.error(logMessage);
			break;
		case WARNING:
			log.warn(logMessage);
			break;
		}
	}
}
