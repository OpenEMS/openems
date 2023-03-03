package io.openems.edge.timeofusetariff.tibber;

import io.openems.common.exceptions.OpenemsException;

public class FoundMultipleHomesException extends OpenemsException {

	private static final long serialVersionUID = 1L;

	public FoundMultipleHomesException() {
		super("Found multiple 'Homes'");
	}

}
