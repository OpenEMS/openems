package io.openems.edge.phoenixcontact.plcnext.common.data;

import io.openems.edge.phoenixcontact.plcnext.common.utils.PlcNextUrlStringHelper;

/**
 * Covering configuration for GDS data access via REST-API
 */
public record PlcNextGdsDataAccessConfig(String baseUrl, String dataInstanceName, String stationId) {

	private static final String PLC_NEXT_DATA_API_PATH = "/api";

	/**
	 * Assembles URL for data access endpoint
	 * 
	 * @return URL for data access endpoint
	 */
	public String dataUrl() {
		return PlcNextUrlStringHelper.buildUrlString(baseUrl, PLC_NEXT_DATA_API_PATH);
	}

}
