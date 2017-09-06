package io.openems.common.websocket;

public enum Notification {
	// TODO warning, info, error should be an enum
	EDGE_CONNECTION_ClOSED(100, "warning", "Connection [%s] was interrupted"),
	EDGE_CONNECTION_OPENED(101, "info", "Connection [%s] was established"),
	EDGE_UNABLE_TO_FORWARD(102, "error", "Unable to forward command to [%s]: %s"),
	EDGE_AUTHENTICATION_BY_TOKEN_FAILED(103, "info", "Authentication by token [%s] failed");
	
	private final int value;
	private final String status;
	private final String message;
	
	private Notification(int value, String status, String message) {
		this.value = value;
		this.status = status;
		this.message = message;
	}
	
	public int getValue() {
		return value;
	}
	
	public String getStatus() {
		return status;
	}
	
	public String getMessage() {
		return message;
	}
}
