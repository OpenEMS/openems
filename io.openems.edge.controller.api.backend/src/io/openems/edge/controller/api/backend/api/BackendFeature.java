package io.openems.edge.controller.api.backend.api;

public enum BackendFeature {
	/**
	 * Backend provides syncable data. Used for spot tariff and currency exchange
	 * rates.
	 */
	DATA_PROVIDER,
	/**
	 * Uploads channel data to this backend.
	 */
	UPLOAD_CHANNEL_DATA
}
