package io.openems.edge.core.appmanager;

/**
 * The status of the current {@link OpenemsApp}.
 * 
 * @see OpenemsAppStatus#STABLE
 * @see OpenemsAppStatus#BETA
 */
public enum OpenemsAppStatus {
	/**
	 * Default value for {@link OpenemsApp OpenemsApps}. Usually indicates that the
	 * {@link OpenemsApp} is in a stable state.
	 */
	STABLE,
	/**
	 * Has to be set explicit to indicate that the {@link OpenemsApp} is in a beta
	 * testing state.
	 */
	BETA;
}
