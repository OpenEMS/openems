package io.openems.edge.phoenixcontact.plcnext.common.data;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionException;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceScope;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttp.Endpoint;
import io.openems.common.bridge.http.api.HttpMethod;
import io.openems.common.bridge.http.time.DefaultDelayTimeProvider;
import io.openems.common.bridge.http.time.DelayTimeProvider;
import io.openems.common.bridge.http.time.DelayTimeProvider.Delay;
import io.openems.common.bridge.http.time.HttpBridgeTimeService;
import io.openems.common.bridge.http.time.HttpBridgeTimeService.TimeEndpoint;
import io.openems.common.bridge.http.time.HttpBridgeTimeServiceDefinition;
import io.openems.common.types.HttpStatus;
import io.openems.edge.phoenixcontact.plcnext.common.auth.PlcNextAuthConfig;
import io.openems.edge.phoenixcontact.plcnext.common.auth.PlcNextTokenManager;
import io.openems.edge.phoenixcontact.plcnext.common.utils.PlcNextUrlStringHelper;

@Component(scope = ServiceScope.PROTOTYPE)
public class PlcNextGdsDataProviderImpl implements PlcNextGdsDataProvider {

	static record PlcNextCreateSessionResponse(String sessionId, Duration sessionTimeout) {
		public PlcNextCreateSessionResponse {
			Objects.requireNonNull(sessionId, "SessionId of PlcNextCreateSessionResponse must not be null!");
			Objects.requireNonNull(sessionTimeout, "SessionTimeout of PlcNextCreateSessionResponse  must not be null!");
		}
	}

	private static final Logger log = LoggerFactory.getLogger(PlcNextGdsDataProviderImpl.class);

	private final BridgeHttp http;
	private final PlcNextTokenManager tokenManager;
	private final HttpBridgeTimeService timeService;

	String sessionId;
	TimeEndpoint maintainSessionTimeEndpoint;

	@Activate
	public PlcNextGdsDataProviderImpl(@Reference(scope = ReferenceScope.PROTOTYPE_REQUIRED) BridgeHttp http,
			@Reference(scope = ReferenceScope.BUNDLE) PlcNextTokenManager tokenManager) {
		this.http = http;
		this.tokenManager = tokenManager;
		this.timeService = http.createService(HttpBridgeTimeServiceDefinition.INSTANCE);
	}

	@Override
	public synchronized Optional<JsonObject> readDataFromRestApi(List<String> variableIdentifiers,
			PlcNextGdsDataAccessConfig dataAccessConfig, PlcNextAuthConfig authConfig) {
		Optional<JsonObject> result = Optional.empty();

		ensureAccessTokenAndSessionIdAreValid(dataAccessConfig, authConfig);

		String requestBody = buildPostBodyForRead(sessionId, variableIdentifiers, dataAccessConfig);
		JsonObject apiResponseBody = sendRequestToApi(HttpMethod.POST, requestBody, dataAccessConfig);

		if (Objects.nonNull(apiResponseBody)) {
			result = Optional.of(apiResponseBody);
		}
		return result;
	}

	private void ensureAccessTokenAndSessionIdAreValid(PlcNextGdsDataAccessConfig dataAccessConfig,
			PlcNextAuthConfig authConfig) {
		if (!tokenManager.hasValidToken()) {
			log.warn("No valid access token! Renewing authentication.");
			tokenManager.fetchToken(authConfig);
		}

		Optional<PlcNextCreateSessionResponse> createSessionResponse = createSessionIfNecessary(dataAccessConfig);
		if (createSessionResponse.isPresent()) {
			this.sessionId = createSessionResponse.get().sessionId();
			this.maintainSessionTimeEndpoint = triggerSessionMaintenanceIfNecessary(
					Delay.of(createSessionResponse.get().sessionTimeout()), dataAccessConfig).orElse(null);
		}
	}

	/**
	 * Deactivates session maintenance mechanism
	 */
	@Override
	public synchronized void deactivateSessionMaintenance() {
		deactivateSessionMaintenanceIfNecessary();
	}

	private void deactivateSessionMaintenanceIfNecessary() {
		log.info("Deactivating session maintenance");

		// remove session ID
		this.sessionId = null;

		// remove time endpoint that maintains the session if it has been created
		if (Objects.nonNull(this.maintainSessionTimeEndpoint)) {
			timeService.removeTimeEndpoint(this.maintainSessionTimeEndpoint);
		}
		this.maintainSessionTimeEndpoint = null;
	}

	/**
	 * Sends request to PLCnext REST API to read variables from or write variables
	 * to GDS
	 * 
	 * @param httpMethod  represents the HTTP method to be used for the request
	 * @param requestBody the body to send to the API
	 * @param config      config to be used to fetch the data
	 * @return response body as @link{JsonObject}
	 */
	JsonObject sendRequestToApi(HttpMethod httpMethod, String requestBody, PlcNextGdsDataAccessConfig config) {
		try {
			Endpoint dataEndPoint = buildDataEndpointRepresentation(tokenManager.getToken(), httpMethod, requestBody,
					config);
			log.debug("StationID '{}': Sending GDS data request to endpoint: '{}'", config.stationId(),
					dataEndPoint.url());

			return http.requestJson(dataEndPoint).thenApply(dataResponse -> {
				if (HttpStatus.OK == dataResponse.status()) {
					log.debug("StationID '{}': Request successful", config.stationId());
					return dataResponse.data().getAsJsonObject();
				} else {
					throw new IllegalStateException("Data endpoint responds with status: '" + dataResponse.status()
							+ "' and body: '" + dataResponse.data() + "'");
				}
			}).join();

		} catch (CompletionException e) {
			log.error("StationID '{}': Error while sending GDS data request! Request body: {}", config.stationId(),
					requestBody, e);
			return null;
		}
	}

	/**
	 * Creates new session and triggers session maintenance while access token is
	 * valid and no error occurs when there is no active session. It sets the member
	 * variables "sessionId" and "maintainSessionEndpoint".
	 * 
	 * @param config config of base URL and instance name
	 * @return @link{Optional} containing an object of
	 *         type @link{PlcNextCreateSessionResponse} representing the response
	 *         containing the session ID
	 */
	Optional<PlcNextCreateSessionResponse> createSessionIfNecessary(PlcNextGdsDataAccessConfig config) {
		Optional<PlcNextCreateSessionResponse> createSessionResponse = Optional.empty();

		if (canCreateSession(config)) {
			log.debug("StationID '{}': Create new session. Current session ID: {}", config.stationId(), this.sessionId);

			// deactivate old session
			deactivateSessionMaintenanceIfNecessary();

			// create session
			Endpoint createSessionEndpoint = buildCreateSessionEndpoint(tokenManager.getToken(), config);
			log.info("StationID '{}': Create session using endpoint: {}", config.stationId(), createSessionEndpoint);
			JsonObject createSessionBody = http.requestJson(createSessionEndpoint).thenApply(response -> {
				if (HttpStatus.CREATED == response.status()) {
					return response.data().getAsJsonObject();
				} else {
					throw new IllegalStateException("Create session endpoint responds with status: '"
							+ response.status() + "' and body: '" + response.data() + "'");
				}
			}).join();
			log.debug("StationID '{}': Create session body: {}", config.stationId(), createSessionBody);

			if (Objects.nonNull(createSessionBody)) {
				String newSessionId = createSessionBody.get("sessionID").getAsString();
				Duration timeoutDuration = Duration.ofMillis(createSessionBody.get("timeout").getAsLong())
						.minusSeconds(1L);

				createSessionResponse = Optional.of(new PlcNextCreateSessionResponse(newSessionId, timeoutDuration));
			}
		}
		return createSessionResponse;
	}

	private boolean canCreateSession(PlcNextGdsDataAccessConfig config) {
		return Objects.isNull(this.sessionId) || Objects.isNull(config);
	}

	/**
	 * Activates the session maintenance when there is a session ID and maintenance
	 * is not active
	 * 
	 * @param sessionTimeout delay of session timeout
	 * @param config         config of base URL and instance name
	 * @return @link{Optional} object containing an object of
	 *         type @link{TimeEndpoint} representing the continuous session
	 *         maintenance, if the optional is not empty
	 */
	Optional<TimeEndpoint> triggerSessionMaintenanceIfNecessary(Delay sessionTimeout,
			PlcNextGdsDataAccessConfig config) {
		Optional<TimeEndpoint> newMaintainSessionTimeEndpoint = Optional.empty();

		if (canActivateSessionMaintenance()) {
			// trigger session maintenance
			DelayTimeProvider delayTimeProvider = new DefaultDelayTimeProvider(() -> sessionTimeout, //
					(error) -> Delay.infinite(), //
					(result) -> sessionTimeout);
			Endpoint maintainSessionEndpoint = buildMaintainSessionEndpoint(tokenManager.getToken(), sessionId, config);
			log.info("SessionID '{}': Maintaining session using endpoint: {}", this.sessionId, maintainSessionEndpoint);

			TimeEndpoint recurringSessionMaintenanceEndpoint = this.timeService.subscribeJsonTime(delayTimeProvider,
					maintainSessionEndpoint, (httpResponse, httpError) -> {
						if (Objects.isNull(httpResponse) && Objects.isNull(httpError)) {
							// Stop on no result
							deactivateSessionMaintenanceIfNecessary();
							log.info("SessionID '{}': No result while maintaining session. "
									+ "Processing skipped and session ID has been reset.", this.sessionId);
						} else if (Objects.nonNull(httpError)) {
							// Stop on error
							log.error("SessionID '{}': Got HTTP error '{}'! Session ID will be reset.", this.sessionId,
									httpError);
							deactivateSessionMaintenanceIfNecessary();
						} else if (Objects.nonNull(httpResponse) && !tokenManager.hasValidToken()) {
							// Stop on expired token
							log.info(
									"SessionID '{}': Got result, but access token has been expired. Session ID will be reset.",
									this.sessionId);
							deactivateSessionMaintenanceIfNecessary();
						} else if (Objects.nonNull(httpResponse) && httpResponse.status() == HttpStatus.OK
								&& Objects.nonNull(httpResponse.data())
								&& Objects.nonNull(httpResponse.data().getAsJsonObject())
								&& Objects.nonNull(httpResponse.data().getAsJsonObject().get("sessionID"))) {
							// Success
							this.sessionId = httpResponse.data().getAsJsonObject().get("sessionID").getAsString();
							log.info("SessionID '{}': Maintaining session has been successful.", this.sessionId);
						} else {
							// Fallback
							log.info("SessionID '{}': Got unprocessable result with status '{}' and body '{}'..",
									this.sessionId, httpResponse.status(), httpResponse.data());
							log.error(
									"SessionID '{}': Session maintenance entered state UNDEFINED! Session ID has been reset.",
									this.sessionId);
							deactivateSessionMaintenanceIfNecessary();
						}
					});
			newMaintainSessionTimeEndpoint = Optional.of(recurringSessionMaintenanceEndpoint);
		}
		return newMaintainSessionTimeEndpoint;
	}

	private boolean canActivateSessionMaintenance() {
		return Objects.nonNull(this.sessionId) && Objects.isNull(this.maintainSessionTimeEndpoint);
	}

	/**
	 * Build endpoint to be used to create a session
	 * 
	 * @param authToken the auth token of PLCnext REST-API
	 * @param config    config of base URL and instance name
	 * @return @link{Endpoint} object
	 */
	public Endpoint buildCreateSessionEndpoint(String authToken, PlcNextGdsDataAccessConfig config) {
		String createSessionEndpointUrl = PlcNextUrlStringHelper.buildUrlString(config.dataUrl(), PATH_SESSIONS);
		Map<String, String> headers = Map.of("Accept", "application/json", //
				"Content-Type", "text/plain", //
				"Authorization", "Bearer " + authToken);
		String postRequestBody = new StringBuilder("stationID=") //
				.append(config.stationId()) //
				.append("&timeout=")//
				.append(PLC_NEXT_DEFAULT_TIMEOUT_IN_MILLIS)//
				.toString();

		return new Endpoint(createSessionEndpointUrl, HttpMethod.POST, BridgeHttp.DEFAULT_CONNECT_TIMEOUT,
				BridgeHttp.DEFAULT_READ_TIMEOUT, postRequestBody, headers);
	}

	/**
	 * Build endpoint to be used to maintain the session
	 * 
	 * @param authToken the auth token of PLCnext REST-API
	 * @param sissionId Id of current PLCnext session
	 * @param config    config of base URL and instance name
	 * @return @link{Endpoint} object
	 */
	public Endpoint buildMaintainSessionEndpoint(String authToken, String sessionId,
			PlcNextGdsDataAccessConfig config) {
		String maintainSessionEndpointUrl = new StringBuilder(
				PlcNextUrlStringHelper.buildUrlString(config.dataUrl(), PATH_SESSIONS))//
				.append("/").append(sessionId).toString();
		Map<String, String> headers = Map.of("Accept", "application/json", //
				"Authorization", "Bearer " + authToken);

		return new Endpoint(maintainSessionEndpointUrl, HttpMethod.POST, BridgeHttp.DEFAULT_CONNECT_TIMEOUT,
				BridgeHttp.DEFAULT_READ_TIMEOUT, "", headers);
	}

	/**
	 * Build endpoint to fetch data of all given variables
	 * 
	 * @param authToken   the auth token of PLCnext REST-API
	 * @param method      method of request
	 * @param requestBody body to send
	 * @param config      config of base URL and instance name
	 * @return @link{Endpoint} object
	 */
	public Endpoint buildDataEndpointRepresentation(String authToken, HttpMethod method, String requestBody,
			PlcNextGdsDataAccessConfig config) {
		String dataEndpointUrl = PlcNextUrlStringHelper.buildUrlString(config.dataUrl(), PATH_VARIABLES);
		Map<String, String> headers = Map.of("Accept", "application/json", //
				"Content-Type", "application/json");
		if (Objects.nonNull(authToken)) {
			headers = new HashMap<String, String>(headers);
			headers.put("Authorization", "Bearer " + authToken);
			headers = Collections.unmodifiableMap(headers);
		}

		return new Endpoint(dataEndpointUrl, method, BridgeHttp.DEFAULT_CONNECT_TIMEOUT,
				BridgeHttp.DEFAULT_READ_TIMEOUT, requestBody, headers);
	}

	/**
	 * Builds the request body to fetch variables via HTTP POST method
	 * 
	 * @param sessionId           ID to coordinate read and write data
	 * @param variableIdentifiers identifiers of variables to read
	 * @param config              config of base URL and instance name
	 * @return request body
	 */
	public String buildPostBodyForRead(String sessionId, List<String> variableIdentifiers,
			PlcNextGdsDataAccessConfig config) {
		String postRequestBody = "";
		if (Objects.nonNull(variableIdentifiers) && !variableIdentifiers.isEmpty()) {
			List<String> variablenames = variableIdentifiers.stream()//
					.map(item -> new StringBuilder(config.dataInstanceName())//
							.append(".").append(PLC_NEXT_INPUT_CHANNEL) //
							.append(".").append(item)//
							.toString())//
					.toList();

			StringBuilder postRequestBodyBuilder = new StringBuilder(PLC_NEXT_PATH_PREFIX) //
					.append("=")//
					.append(PLC_NEXT_OPENEMS_COMPONENT_NAME)//
					.append("/&paths=")//
					.append(String.join(",", variablenames));
			if (Objects.nonNull(sessionId)) {
				postRequestBodyBuilder.append("&") //
						.append(PLC_NEXT_SESSION_ID).append("=") //
						.append(sessionId);
			}
			postRequestBody = postRequestBodyBuilder.toString();
		}
		return postRequestBody;
	}

	@Override
	public synchronized Optional<JsonObject> writeDataToRestApi(List<JsonElement> mappedVariables,
			PlcNextGdsDataAccessConfig dataAccessConfig, PlcNextAuthConfig authConfig) {

		Optional<JsonObject> result = Optional.empty();

		if (Objects.isNull(mappedVariables)) {
			log.warn(
					"StationID '{}': Nothing to update, because variable data is NULL! This should never happen! Skipping PUT request.",
					dataAccessConfig.stationId());
		} else if (mappedVariables.isEmpty()) {
			log.info("StationID '{}': Nothing to update. Skipping PUT request.", dataAccessConfig.stationId());
		} else {
			ensureAccessTokenAndSessionIdAreValid(dataAccessConfig, authConfig);

			String requestBody = buildPutBodyForWrite(sessionId, mappedVariables);
			JsonObject apiResponseBody = sendRequestToApi(HttpMethod.PUT, requestBody, dataAccessConfig);

			if (Objects.nonNull(apiResponseBody)) {
				result = Optional.of(apiResponseBody);
			}
		}
		return result;
	}

	/**
	 * Builds the request body to write variables via HTTP PUT method
	 * 
	 * @param sessionId       ID to coordinate read and write data
	 * @param mappedVariables represents the mapped variables
	 * @return request body
	 */
	public String buildPutBodyForWrite(String sessionId, List<JsonElement> mappedVariables) {
		JsonObject requestBodyObject = new JsonObject();

		requestBodyObject.addProperty(PLC_NEXT_SESSION_ID, sessionId);
		requestBodyObject.addProperty(PLC_NEXT_PATH_PREFIX, PLC_NEXT_PATH_PREFIX);

		if (Objects.nonNull(mappedVariables) && !mappedVariables.isEmpty()) {
			JsonArray valueArray = new JsonArray(mappedVariables.size());
			for (JsonElement elem : mappedVariables) {
				valueArray.add(elem);
			}
			requestBodyObject.add(PLC_NEXT_VARIABLES, valueArray);
		}
		return requestBodyObject.toString();
	}
}
