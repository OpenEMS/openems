package io.openems.common.websocket;

public enum Notification {
	EDGE_CONNECTION_ClOSED(100, NotificationStatus.WARNING, "Connection [%s] was interrupted"),
	EDGE_CONNECTION_OPENED(101, NotificationStatus.INFO, "Connection [%s] was established"),
	EDGE_UNABLE_TO_FORWARD(102, NotificationStatus.ERROR, "Unable to forward command to [%s]: %s"),
	EDGE_AUTHENTICATION_BY_TOKEN_FAILED(103, NotificationStatus.INFO, "Authentication by token [%s] failed"),
	EDGE_CHANNEL_UPDATE_SUCCESS(104, NotificationStatus.SUCCESS, "Configuration successfully updated [%s]"),
	EDGE_CHANNEL_UPDATE_FAILED(105, NotificationStatus.ERROR, "Configuration update failed [%s]: %s"),
	BACKEND_NOT_ALLOWED(106, NotificationStatus.ERROR, "The operation [%s] is not allowed via Backend"),
	EDGE_CHANNEL_UPDATE_TIMEOUT(107, NotificationStatus.INFO, "Channel setting [%s] timed out.");
	
	private final int value;
	private final NotificationStatus status;
	private final String message;
	
	private Notification(int value, NotificationStatus status, String message) {
		this.value = value;
		this.status = status;
		this.message = message;
	}
	
	public int getValue() {
		return value;
	}
	
	public NotificationStatus getStatus() {
		return status;
	}
	
	public String getMessage() {
		return message;
	}
}
