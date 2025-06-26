package io.openems.common.exceptions;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;

public class OpenemsException extends OpenemsNamedException {

	private static final long serialVersionUID = 1L;

	public OpenemsException(String message) {
		super(OpenemsError.GENERIC, message);
	}

	public OpenemsException(String message, Throwable cause) {
		super(OpenemsError.GENERIC, message + ": " + cause.getMessage());
	}

	public OpenemsException(Throwable cause) {
		this(cause.getClass().getSimpleName() + ": " + cause.getMessage());
	}

}
