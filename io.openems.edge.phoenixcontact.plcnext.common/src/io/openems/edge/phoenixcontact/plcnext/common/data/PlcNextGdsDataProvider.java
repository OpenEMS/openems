package io.openems.edge.phoenixcontact.plcnext.common.data;

import java.util.List;
import java.util.Optional;

import com.google.gson.JsonObject;

import io.openems.edge.phoenixcontact.plcnext.common.auth.PlcNextAuthConfig;

/**
 * TODO
 */
public interface PlcNextGdsDataProvider {

	String PATH_VARIABLES = "/variables";
	String PATH_SESSIONS = "/sessions";

	String PLC_NEXT_DEFAULT_STATION_ID = "10";
	String PLC_NEXT_DEFAULT_TIMEOUT_IN_MILLIS = "50000";
	String PLC_NEXT_VARIABLES = "variables";
	String PLC_NEXT_INPUT_CHANNEL = "udtIn";

	/**
	 * Fetch data for given variables from PLCnext REST-API and return as JSON object
	 * 
	 * @param variableIdentifiers list of variable identifiers to fetch
	 * @param config              config to be used to fetch the data
	 * @return	@link{JsonObject} containing raw response from REST-API
	 */
	Optional<JsonObject> readDataFromRestApi(List<String> variableIdentifiers, 
			PlcNextGdsDataAccessConfig dataAccessConfig, 
			PlcNextAuthConfig authConfig);

	/**
	 * Deactivates session maintenance mechanism
	 */
	void deactivateSessionMaintenance();

}