package io.openems.edge.controller.api.backend.api;

/**
 * This enum signals how data (e.g. market prices or currency exchange) should
 * be fetched.
 */
public enum DataFetchType {
	/**
	 * Data should be fetched via backend.
	 */
	FETCH_VIA_BACKEND,
	/**
	 * Data should be fetched locally (for example via http requests).
	 */
	FETCH_LOCALLY,
	/**
	 * The status is not known yet - maybe the system is still starting or the
	 * backend connection was lost recently. This status means that the fetch method
	 * should not be changed (or initialized) for now, but the system should wait
	 * until the status is known.
	 */
	WAITING
}
