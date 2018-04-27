package io.openems.edge.ess.power;

import io.openems.common.exceptions.OpenemsException;

public class PowerException extends OpenemsException {

	private static final long serialVersionUID = 1L;

	public PowerException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public PowerException(String arg0) {
		super(arg0);
	}

}
