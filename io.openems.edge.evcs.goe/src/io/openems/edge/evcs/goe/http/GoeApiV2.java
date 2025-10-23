package io.openems.edge.evcs.goe.http;

public class GoeApiV2 {

	private static final String FILTER_NEW = "amp,car,err,pha,nrg";
	private static final String FILTER_LEGACY = "[\"amp\",\"car\",\"err\",\"pha\",\"nrg\"]";

	private final String ipAddress;

	public GoeApiV2(EvcsGoeHttpImpl p) {
		this.ipAddress = p.config.ip();
	}

	/**
	 * Gets the filtered status from go-e. See https://github.com/goecharger
	 *
	 * @param legacy if firmware version is <= 051.3
	 * @return the boolean value
	 */
	public String getFilteredStatusUrl(boolean legacy) {
		var filter = FILTER_NEW;
		if (legacy) {
			filter = FILTER_LEGACY;
		}
		return "http://" + this.ipAddress + "/api/status?filter=" + filter;
	}

	/**
	 * Gets the status from go-e. See https://github.com/goecharger
	 *
	 * @return the url
	 */
	public String getStatusUrl() {
		return "http://" + this.ipAddress + "/api/status";
	}

}
