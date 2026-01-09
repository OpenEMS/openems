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
import io.openems.edge.phoenixcontact.plcnext.common.auth.PlcNextTokenManager;
import io.openems.edge.phoenixcontact.plcnext.common.auth.PlcNextAuthConfig;
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
	PlcNextGdsDataAccessConfig config;

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

		if (!tokenManager.hasValidToken()) {
			log.warn("No valid access token! Renewing authentication.");
			tokenManager.fetchToken(authConfig);
		} else {
			log.debug("Session ID: " + sessionId);

			Optional<PlcNextCreateSessionResponse> createSessionResponse = createSessionIfNecessary(dataAccessConfig);

			if (createSessionResponse.isPresent()) {
				this.sessionId = createSessionResponse.get().sessionId();
				this.maintainSessionTimeEndpoint = triggerSessionMaintenanceIfNecessary(
						Delay.of(createSessionResponse.get().sessionTimeout()), dataAccessConfig).orElse(null);
			}
		}

		JsonObject apiResponseBody = fetchDataFromApi(variableIdentifiers, dataAccessConfig);

		if (Objects.nonNull(apiResponseBody)) {
			result = Optional.of(apiResponseBody);
		}
		return result;
	}

	/**
	 * Deactivates session maintenance mechanism
	 */
	@Override
	public synchronized void deactivateSessionMaintenance() {
		deactivateSessionMaintenanceIfNecessary();
	}

	private void deactivateSessionMaintenanceIfNecessary() {
		// remove session ID if necessary
		if (Objects.nonNull(sessionId)) {
			this.sessionId = null;
		}
		// remove time endpoint that maintains the session if necessary
		if (Objects.nonNull(this.maintainSessionTimeEndpoint)) {
			timeService.removeTimeEndpoint(this.maintainSessionTimeEndpoint);
			this.maintainSessionTimeEndpoint = null;
		}
	}

	/**
	 * Fetches given variables from GDS REST API
	 * 
	 * @param variableIdentifiers list of variable identifiers to fetch
	 * @param config              config to be used to fetch the data
	 * @return response body as @link{JsonObject}
	 */
	JsonObject fetchDataFromApi(List<String> variableIdentifiers, PlcNextGdsDataAccessConfig config) {
		try {
			Endpoint dataEndPoint = buildDataEndpointRepresentation(tokenManager.getToken(), sessionId,
					variableIdentifiers, config);
			log.debug("Fetching GDS data from endpoint: '" + dataEndPoint.url() + "'");

			return http.requestJson(dataEndPoint).thenApply(dataResponse -> {
				if (HttpStatus.OK == dataResponse.status()) {
					log.debug("Parsing returned data ...");
					return dataResponse.data().getAsJsonObject();
				} else {
					log.error("Data endpoint responds with status: '" + dataResponse.status() + "' and body: '"
							+ dataResponse.data() + "'");
					return null;
				}
			}).join();

		} catch (CompletionException e) {
			log.error("Error while fetching data from api!", e);
			return null;
		}
	}

	/**
	 * Creates new session and triggers session maintenance while access token is
	 * valid and no error occurs when there is no active session. It sets the member
	 * variables "sessionId" and "maintainSessionEndpoint".
	 * 
	 * @param config config of base URL and instance name
	 * @return @link{Optional} containing an object of type @link{PlcNextCreateSessionResponse} 
	 * 	representing the response containing the session ID
	 */
	Optional<PlcNextCreateSessionResponse> createSessionIfNecessary(PlcNextGdsDataAccessConfig config) {
		Optional<PlcNextCreateSessionResponse> createSessionResponse = Optional.empty();

		if (canCreateSession(config)) {
			this.config = config;
			
			// deactivate old session
			deactivateSessionMaintenanceIfNecessary();
			
			// create session
			Endpoint createSessionEndpoint = buildCreateSessionEndpoint(tokenManager.getToken(), config);
			log.info("Create session using endpoint: " + createSessionEndpoint);
			JsonObject createSessionBody = http.requestJson(createSessionEndpoint).thenApply(response -> {
				if (HttpStatus.CREATED == response.status()) {
					return response.data().getAsJsonObject();
				} else {
					log.error("Create session endpoint responds with status: '" + response.status() + "' and body: '"
							+ response.data() + "'");
					return null;
				}
			}).join();
			log.info("Create session body: " + createSessionBody);

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
		return Objects.isNull(this.sessionId) ||
				Objects.isNull(this.config) ||
				!this.config.equals(config);
	}

	/**
	 * Activates the session maintenance when there is a session ID and maintenance is not active
	 * 
	 * @param sessionTimeout	delay of session timeout
	 * @param config config of base URL and instance name
	 * @return @link{Optional} object containing an object of type @link{TimeEndpoint} representing 
	 * 	the continuous session maintenance, if the optional is not empty
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
			log.info("Maintaining session using endpoint: " + maintainSessionEndpoint);

			TimeEndpoint recurringSessionMaintenanceEndpoint = this.timeService.subscribeJsonTime(delayTimeProvider,
					maintainSessionEndpoint, (httpResponse, httpError) -> {
						if (Objects.isNull(httpResponse) && Objects.isNull(httpError)) {
							// Stop on no result
							deactivateSessionMaintenanceIfNecessary();
							log.info(
									"No result while maintaining session. Processing skipped and session ID has been reset.");
						} else if (Objects.nonNull(httpError)) {
							// Stop on error
							deactivateSessionMaintenanceIfNecessary();
							log.error("Got HTTP error '" + httpError + "'! Session ID has been reset.");
						} else if (Objects.nonNull(httpResponse) && !tokenManager.hasValidToken()) {
							// Stop on expired token
							deactivateSessionMaintenance();
							log.info("Got result, but access token has been expired. Session ID has been reset.");
						} else if (Objects.nonNull(httpResponse) && httpResponse.status() == HttpStatus.OK
								&& Objects.nonNull(httpResponse.data())
								&& Objects.nonNull(httpResponse.data().getAsJsonObject())
								&& Objects.nonNull(httpResponse.data().getAsJsonObject().get("sessionID"))) {
							// Success
							this.sessionId = httpResponse.data().getAsJsonObject().get("sessionID").getAsString();
							log.info("Maintaining session has been successful.");
						} else {
							// Fallback
							log.info("Got unprocessable result with status '" + httpResponse.status() + "' and body '"
									+ httpResponse.data() + "'");
							deactivateSessionMaintenanceIfNecessary();
							log.error("Session maintenance entered state UNDEFINED! Session ID has been reset.");
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
	 * @param authToken	the auth token of PLCnext REST-API
	 * @param config config of base URL and instance name
	 * @return @link{Endpoint} object
	 */
	public Endpoint buildCreateSessionEndpoint(String authToken, PlcNextGdsDataAccessConfig config) {
		String createSessionEndpointUrl = PlcNextUrlStringHelper.buildUrlString(config.dataUrl(), PATH_SESSIONS);
		Map<String, String> headers = Map.of("Accept", "application/json", //
				"Content-Type", "application/json", //
				"Authorization", "Bearer " + authToken);
		String postRequestBody = new StringBuilder("stationID=") //
				.append(PLC_NEXT_DEFAULT_STATION_ID) //
				.append("&timeout=")//
				.append(PLC_NEXT_DEFAULT_TIMEOUT_IN_MILLIS)//
				.toString();

		return new Endpoint(createSessionEndpointUrl, HttpMethod.POST, BridgeHttp.DEFAULT_CONNECT_TIMEOUT,
				BridgeHttp.DEFAULT_READ_TIMEOUT, postRequestBody, headers);
	}

	/**
	 * Build endpoint to be used to maintain the session
	 * 
	 * @param authToken	the auth token of PLCnext REST-API
	 * @param sissionId	Id of current PLCnext session
	 * @param config config of base URL and instance name
	 * @return @link{Endpoint} object
	 */
	public Endpoint buildMaintainSessionEndpoint(String authToken, String sessionId,
			PlcNextGdsDataAccessConfig config) {
		String maintainSessionEndpointUrl = new StringBuilder(
				PlcNextUrlStringHelper.buildUrlString(config.dataUrl(), PATH_SESSIONS))//
				.append("/").append(sessionId).toString();
		Map<String, String> headers = Map.of("Accept", "application/json", //
				"Content-Type", "application/json", //
				"Authorization", "Bearer " + authToken);

		return new Endpoint(maintainSessionEndpointUrl, HttpMethod.POST, BridgeHttp.DEFAULT_CONNECT_TIMEOUT,
				BridgeHttp.DEFAULT_READ_TIMEOUT, "", headers);
	}

	/**
	 * Build endpoint to fetch data of all given variables
	 * 
	 * @param authToken	the auth token of PLCnext REST-API
	 * @param sissionId	Id of current PLCnext session
	 * @param variableIdentifiers	variables to fetch data for
	 * @param config	config of base URL and instance name
	 * @return @link{Endpoint} object
	 */
	public Endpoint buildDataEndpointRepresentation(String authToken, String sessionId,
			List<String> variableIdentifiers, PlcNextGdsDataAccessConfig config) {
		String dataEndpointUrl = PlcNextUrlStringHelper.buildUrlString(config.dataUrl(), PATH_VARIABLES);
		Map<String, String> headers = Map.of("Accept", "application/json", //
				"Content-Type", "application/json");
		if (Objects.nonNull(authToken)) {
			headers = new HashMap<String, String>(headers);
			headers.put("Authorization", "Bearer " + authToken);
			headers = Collections.unmodifiableMap(headers);
		}

		String postRequestBody = "";
		if (Objects.nonNull(variableIdentifiers) && !variableIdentifiers.isEmpty()) {
			List<String> variablenames = variableIdentifiers.stream()//
					.map(item -> new StringBuilder(config.dataInstanceName())//
							.append(".").append(PlcNextGdsDataProvider.PLC_NEXT_INPUT_CHANNEL) //
							.append(".").append(item)//
							.toString())//
					.toList();

			StringBuilder postRequestBodyBuilder = new StringBuilder("pathPrefix=")//
					.append(PlcNextGdsDataAccessConfig.PLC_NEXT_OPENEMS_COMPONENT_NAME)//
					.append("/&paths=")//
					.append(String.join(",", variablenames));
			if (Objects.nonNull(sessionId)) {
				postRequestBodyBuilder.append("&sessionID=")//
						.append(sessionId);
			}
			postRequestBody = postRequestBodyBuilder.toString();
		}
		return new Endpoint(dataEndpointUrl, HttpMethod.POST, BridgeHttp.DEFAULT_CONNECT_TIMEOUT,
				BridgeHttp.DEFAULT_READ_TIMEOUT, postRequestBody, headers);
	}
}
