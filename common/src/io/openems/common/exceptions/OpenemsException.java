package io.openems.common.exceptions;

public class OpenemsException extends Exception {
	private static final long serialVersionUID = 5015013132334439401L;

	public OpenemsException(String message) {
		super(message);
	}
	
	public OpenemsException(String message, Throwable cause) {
		super(message, cause);
	}
}
