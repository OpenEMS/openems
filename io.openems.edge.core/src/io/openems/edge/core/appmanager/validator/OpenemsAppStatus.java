package io.openems.edge.core.appmanager.validator;

public enum OpenemsAppStatus {
	/**
	 * Apps which are not compatible with the system. e. g. not a Home
	 */
	INCOMPATIBLE,

	/**
	 * Apps which are compatible but can not be installed. e. g. not enough relays
	 * available
	 */
	COMPATIBLE,

	/**
	 * Apps which can be installed.
	 */
	INSTALLABLE;

}
