package io.openems.edge.phoenixcontact.plcnext.common.data;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.edge.phoenixcontact.plcnext.common.auth.PlcNextAuthConfig;

/**
 * TODO
 */
public interface PlcNextGdsDataProvider {

	String PATH_VARIABLES = "/variables";
	String PATH_SESSIONS = "/sessions";

	String PLC_NEXT_DEFAULT_TIMEOUT_IN_MILLIS = "50000";

	String PLC_NEXT_SESSION_ID = "sessionID";
	String PLC_NEXT_PATH_PREFIX = "pathPrefix";
	String PLC_NEXT_VARIABLES = "variables";

	String PLC_NEXT_OPENEMS_COMPONENT_NAME = "OpenEMS_V1Component1";

	String PLC_NEXT_INPUT_CHANNEL = "udtIn";
	String PLC_NEXT_OUTPUT_CHANNEL = "udtOut";

	/**
	 * Fetch data for given variables from PLCnext REST-API and return as JSON
	 * object
	 * 
	 * @param variableIdentifiers list of variable identifiers to fetch
	 * @param dataAccessConfig    config to be used to fetch the data
	 * @param authConfig          config to be used for authentication
	 * @return @link{JsonObject} containing raw response from REST-API
	 */
	CompletableFuture<JsonObject> readDataFromRestApi(List<String> variableIdentifiers,
			PlcNextGdsDataAccessConfig dataAccessConfig, PlcNextAuthConfig authConfig);

	/**
	 * WIP: Proposal
	 * 
	 * @param mappedVariables  map containing key values of variables to write to
	 *                         PLCnext REST-API
	 * @param dataAccessConfig config to be used to fetch the data
	 * @param authConfig       config to be used for authentication
	 * @return @link{JsonObject} containing raw response from REST-API
	 */
	CompletableFuture<JsonObject> writeDataToRestApi(List<JsonElement> mappedVariables,
			PlcNextGdsDataAccessConfig dataAccessConfig, PlcNextAuthConfig authConfig);

	/**
	 * Deactivates session maintenance mechanism
	 */
	void deactivateSessionMaintenance();

}