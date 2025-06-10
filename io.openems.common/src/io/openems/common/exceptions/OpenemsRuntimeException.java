package io.openems.common.exceptions;

public class OpenemsRuntimeException extends RuntimeException {

	private static final long serialVersionUID = -4509666272212124910L;

	public OpenemsRuntimeException() {
		super();
	}

	public OpenemsRuntimeException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public OpenemsRuntimeException(String message, Throwable cause) {
		super(message, cause);
	}

	public OpenemsRuntimeException(String message) {
		super(message);
	}

	public OpenemsRuntimeException(Throwable cause) {
		super(cause);
	}

}
