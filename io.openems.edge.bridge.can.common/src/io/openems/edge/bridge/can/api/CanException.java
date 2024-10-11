package io.openems.edge.bridge.can.api;

import io.openems.common.exceptions.OpenemsException;

/**
 * Generic CAN exception.
 */
public class CanException extends OpenemsException {

	private static final long serialVersionUID = -8261240678924535474L;

	public CanException() {
		super("");
	}

	public CanException(String txt) {
		super(txt);
	}

}
