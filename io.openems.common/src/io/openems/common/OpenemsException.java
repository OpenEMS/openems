package io.openems.common;

public class OpenemsException extends Exception {

	private static final long serialVersionUID = 1L;

	public OpenemsException(String message) {
		super(message);
	}
	
	public OpenemsException(String message, Throwable cause) {
		super(message, cause);
	}
}
