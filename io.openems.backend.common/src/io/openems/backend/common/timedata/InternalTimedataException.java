package io.openems.backend.common.timedata;

import io.openems.common.exceptions.OpenemsException;

public class InternalTimedataException extends OpenemsException {

	private static final long serialVersionUID = -2037204231739569486L;

	public InternalTimedataException(String message) {
		super(message);
	}

	public InternalTimedataException(String message, Throwable cause) {
		super(message, cause);
	}

	public InternalTimedataException(Throwable cause) {
		super(cause);
	}

}
