package io.openems.edge.core.appmanager;

public enum OpenemsAppCardinality {
	/**
	 * Only one instance per {@link OpenemsAppCategory}.
	 */
	SINGLE_IN_CATEGORY,

	/**
	 * Only one instance per {@link OpenemsApp}.
	 */
	SINGLE,

	/**
	 * Any number of instances.
	 */
	MULTIPLE;

}
