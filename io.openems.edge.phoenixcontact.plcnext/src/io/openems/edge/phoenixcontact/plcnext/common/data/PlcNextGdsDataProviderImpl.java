package io.openems.edge.phoenixcontact.plcnext.common.data;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
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
	public synchronized CompletableFuture<JsonObject> readDataFromRestApi(List<String> variableIdentifiers,
			PlcNextGdsDataAccessConfig dataAccessConfig, PlcNextAuthConfig authConfig) {

		return ensureAccessTokenAndSessionIdAreValid(dataAccessConfig, authConfig) //
			.thenCompose(ignore -> {
				String requestBody = buildPostBodyForRead(sessionId, variableIdentifiers, dataAccessConfig);
				Endpoint dataEndPoint = buildDataEndpointRepresentation(tokenManager.getToken(), //
						HttpMethod.POST, requestBody, dataAccessConfig);

				return sendRequestToApi(dataEndPoint, HttpStatus.OK, dataAccessConfig.stationId());
			});
	}

	@Override
	public synchronized CompletableFuture<JsonObject> writeDataToRestApi(List<JsonElement> mappedVariables,
			PlcNextGdsDataAccessConfig dataAccessConfig, PlcNextAuthConfig authConfig) {

		if (Objects.isNull(mappedVariables)) {
			log.warn("StationID '{}': Nothing to update, because variable data is NULL! This should never happen! Skipping PUT request.",
					dataAccessConfig.stationId());
			return CompletableFuture.failedFuture(new IllegalArgumentException("Mapped variables may not be NULL"));
		} else if (mappedVariables.isEmpty()) {
			log.info("StationID '{}': Nothing to update. Skipping PUT request.", dataAccessConfig.stationId());
			return CompletableFuture.completedFuture(null);
		} else {
			return ensureAccessTokenAndSessionIdAreValid(dataAccessConfig, authConfig)
					.thenCompose(ignore -> {
						String requestBody = buildPutBodyForWrite(sessionId, mappedVariables);
						Endpoint dataEndPoint = buildDataEndpointRepresentation(tokenManager.getToken(), HttpMethod.PUT, //
								requestBody, dataAccessConfig);
						
						return sendRequestToApi(dataEndPoint, HttpStatus.OK, dataAccessConfig.stationId());
					});
		}
	}


	@Override
	public synchronized void deactivateSessionMaintenance(PlcNextGdsDataAccessConfig config) {
		deactivateSessionMaintenanceIfNecessary(config);
	}

	private void deactivateSessionMaintenanceIfNecessary(PlcNextGdsDataAccessConfig config) {
		log.info("StationID '{}': Deactivating session maintenance called", config.stationId());
		String oldSessionId = this.sessionId;

		// remove session ID
		this.sessionId = null;

		// remove time endpoint that maintains the session if it has been created
		if (Objects.nonNull(this.maintainSessionTimeEndpoint)) {
			log.info("StationID '{}': Deactivating session maintenance for sessionID {}", config.stationId(), oldSessionId);
			timeService.removeTimeEndpoint(this.maintainSessionTimeEndpoint);
		}
		this.maintainSessionTimeEndpoint = null;
	}

	/** 
	 * Checks access token and session and triggers fetching both if not existing or invalid
	 * 
	 * @param dataAccessConfig    config to be used to fetch the data
	 * @param authConfig          config to be used for authentication
	 * @return @link{CompletableFuture} of void
	 */
	CompletableFuture<Void> ensureAccessTokenAndSessionIdAreValid( //
			PlcNextGdsDataAccessConfig dataAccessConfig, //
			PlcNextAuthConfig authConfig) {
		
		CompletableFuture<Void> result = CompletableFuture.completedFuture(null);
		
		if (!tokenManager.hasValidToken()) {
			log.warn("StationID '{}': No valid access token! Renewing authentication.", dataAccessConfig.stationId());			
			result = tokenManager.fetchToken(authConfig);
		} 
		if (canCreateSession(dataAccessConfig)) {
			result = result.thenCompose(ignore -> createOrFetchSessionID(dataAccessConfig)) //
					.thenApply(createSessionResponse -> {
						if (!createSessionResponse.sessionId.isBlank()) {							
							this.sessionId = createSessionResponse.sessionId();
							
							if (canActivateSessionMaintenance()) {
								this.maintainSessionTimeEndpoint = enableSessionMaintenance( //
										Delay.of(createSessionResponse.sessionTimeout()),  dataAccessConfig);
							}
						}		
						return null;
					});
		}
		return result; 
	}

	/**
	 * Create new session or try to fetch existing session ID and trigger session 
	 * maintenance while access token is valid and no error occurs when there is 
	 * no active session. It sets the member variables "sessionId" and "maintainSessionEndpoint".
	 * 
	 * @param config config of base URL and instance name
	 * @return @link{CompletableFuture} containing an object of
	 *         type @link{PlcNextCreateSessionResponse} representing the response
	 *         containing the session ID
	 */
	CompletableFuture<PlcNextCreateSessionResponse> createOrFetchSessionID(PlcNextGdsDataAccessConfig config) {
		log.debug("StationID '{}': Create new session. Current session ID: {}", config.stationId(), this.sessionId);

		// deactivate old session
		deactivateSessionMaintenanceIfNecessary(config);

		try {
			return createSession(config);
		} catch (CompletionException ce) {
			log.error("StationID '{}': Create session failed! Trying to fetch session ID.", config.stationId(), ce);
			return fetchSession(config);
		}
	}
	
	private boolean canCreateSession(PlcNextGdsDataAccessConfig config) {
		return Objects.isNull(this.sessionId) || Objects.isNull(config);
	}

	private CompletableFuture<PlcNextCreateSessionResponse> createSession(PlcNextGdsDataAccessConfig config) {
		// create session
		Endpoint createSessionEndpoint = buildCreateSessionEndpoint(tokenManager.getToken(), config);
		log.info("StationID '{}': Create session using endpoint: {}", config.stationId(), createSessionEndpoint);
		
		return sendRequestToApi(createSessionEndpoint, HttpStatus.CREATED, config.stationId()) //
				.thenApply(createSessionBody -> {
					log.debug("StationID '{}': Create session body: {}", config.stationId(), createSessionBody);
					PlcNextCreateSessionResponse result = null;
					
					if (Objects.nonNull(createSessionBody)) {
						String newSessionId = createSessionBody.get("sessionID").getAsString();
						Duration timeoutDuration = Duration.ofMillis(createSessionBody.get("timeout").getAsLong())
								.minusSeconds(1L);
						
						result = new PlcNextCreateSessionResponse(newSessionId, timeoutDuration);
					}
					return result;
				});
	}

	private CompletableFuture<PlcNextCreateSessionResponse> fetchSession(PlcNextGdsDataAccessConfig config) {
		// fetch session
		Endpoint fetchSessionsEndpoint = buildFetchSessionsEndpoint(tokenManager.getToken(), config);
		log.info("StationID '{}': Fetch sessions using endpoint: {}", config.stationId(), fetchSessionsEndpoint);
		
		return sendRequestToApi(fetchSessionsEndpoint, HttpStatus.OK, config.stationId()) //
				.thenApply(fetchSessionsResponseBody -> {
					log.debug("StationID '{}': Fetch sessions response body: {}", config.stationId(), fetchSessionsResponseBody);
					PlcNextCreateSessionResponse result = null;
					
					if (Objects.nonNull(fetchSessionsResponseBody)) {
						JsonElement sessions = fetchSessionsResponseBody.get("sessions");
						
						if (Objects.nonNull(sessions) && !sessions.isJsonNull() && sessions.isJsonArray()) {
							Optional<JsonObject> sessionJsonObject = sessions.getAsJsonArray().asList().stream() //
									.filter(item -> item.isJsonObject() && //
											item.getAsJsonObject().has("stationID") && //
											config.stationId().equalsIgnoreCase(item.getAsJsonObject().get("stationID").getAsString())) //
									.map(JsonElement::getAsJsonObject) //
									.findFirst();
							
							if (sessionJsonObject.isEmpty()) {
								log.info("StationID '{}': Cannot find session of this station!", config.stationId());
							} else {
								String sessionId = sessionJsonObject.get().get("id").getAsString();
								Duration timeoutDuration = Duration.ofMillis(fetchSessionsResponseBody.get("timeout").getAsLong())
										.minusSeconds(1L);
								
								result = new PlcNextCreateSessionResponse(sessionId, timeoutDuration);					
								log.debug("StationID '{}': Session of this station found {}.", config.stationId(), result);
							}
						}
					}
					return result;
				});
	}

	/**
	 * Activates the session maintenance when there is a session ID and maintenance
	 * is not active
	 * 
	 * @param sessionTimeout delay of session timeout
	 * @param config         config of base URL and instance name
	 * @return @link{TimeEndpoint} representing the continuous session
	 *         maintenance or NULL otherwise
	 */
	TimeEndpoint enableSessionMaintenance(Delay sessionTimeout,
			PlcNextGdsDataAccessConfig config) {

		// trigger session maintenance
		DelayTimeProvider delayTimeProvider = new DefaultDelayTimeProvider(() -> sessionTimeout, //
				(error) -> Delay.infinite(), //
				(result) -> sessionTimeout);
		Endpoint maintainSessionEndpoint = buildMaintainSessionEndpoint(tokenManager.getToken(), sessionId, config);
		log.info("SessionID '{}': Maintaining session using endpoint: {}", this.sessionId, maintainSessionEndpoint);

		return this.timeService.subscribeJsonTime(delayTimeProvider,
				maintainSessionEndpoint, (httpResponse, httpError) -> {
					if (Objects.isNull(httpResponse) && Objects.isNull(httpError)) {
						// Stop on no result
						deactivateSessionMaintenanceIfNecessary(config);
						log.info("SessionID '{}': No result while maintaining session. "
								+ "Processing skipped and session ID has been reset.", this.sessionId);
					} else if (Objects.nonNull(httpError)) {
						// Stop on error
						log.error("SessionID '{}': Got HTTP error '{}'! Session ID will be reset.", this.sessionId,
								httpError);
						deactivateSessionMaintenanceIfNecessary(config);
					} else if (Objects.nonNull(httpResponse) && !tokenManager.hasValidToken()) {
						// Stop on expired token
						log.info(
								"SessionID '{}': Got result, but access token has been expired. Session ID will be reset.",
								this.sessionId);
						deactivateSessionMaintenanceIfNecessary(config);
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
						deactivateSessionMaintenanceIfNecessary(config);
					}
				});
	}

	private boolean canActivateSessionMaintenance() {
		return Objects.nonNull(this.sessionId) && Objects.isNull(this.maintainSessionTimeEndpoint);
	}

	/**
	 * Sends request to PLCnext REST API to read from or write to controller
	 * 
	 * @param endpoint  represents the endpoint definition to be used for the request
	 * @param expectedStatus represents the expected status to state a successful API request
	 * @param stationId the stationID for logging
	 * @return response body wrapped into a @link{CompletableFuture}
	 */
	CompletableFuture<JsonObject> sendRequestToApi(Endpoint endpoint, HttpStatus expectedStatus, //
			String stationId) {
		
		try {
			log.debug("StationID '{}': Sending request to API endpoint: '{}'", stationId, endpoint.url());
			CompletableFuture<JsonObject> requestFuture =  http.requestJson(endpoint) //
					.thenApply(apiResponse -> {
						if (expectedStatus == apiResponse.status()) {
							log.debug("StationID '{}': Request successful", stationId);
							return apiResponse.data().getAsJsonObject();
						} else {
							throw new IllegalStateException("API endpoint responds with status: '" + apiResponse.status()
							+ "' and body: '" + apiResponse.data() + "'");
						}
					});
			return requestFuture;
		} catch (CompletionException e) {
			log.error("StationID '{}': Error while sending API request! Request body: {}", stationId,
					endpoint.body(), e);
			return CompletableFuture.failedFuture(e);
		}
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
	 * Build endpoint to be used to fetch a sessions
	 * 
	 * @param authToken the auth token of PLCnext REST-API
	 * @param config    config of base URL and instance name
	 * @return @link{Endpoint} object
	 */
	public Endpoint buildFetchSessionsEndpoint(String authToken, PlcNextGdsDataAccessConfig config) {
		String fetchSessionEndpointUrl = PlcNextUrlStringHelper.buildUrlString(config.dataUrl(), PATH_SESSIONS);
		Map<String, String> headers = Map.of("Accept", "application/json", //
				"Authorization", "Bearer " + authToken);

		return new Endpoint(fetchSessionEndpointUrl, HttpMethod.GET, BridgeHttp.DEFAULT_CONNECT_TIMEOUT,
				BridgeHttp.DEFAULT_READ_TIMEOUT, null, headers);
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
