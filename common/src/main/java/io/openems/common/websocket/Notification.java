package io.openems.common.websocket;

public enum Notification {
	OPENEMS_EDGE_CONNECTION_ClOSED(100, "warning", "Connection [%s] was interrupted"),
	OPENEMS_EDGE_CONNECTION_OPENED(101, "info", "Connection [%s] was established");
	
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
