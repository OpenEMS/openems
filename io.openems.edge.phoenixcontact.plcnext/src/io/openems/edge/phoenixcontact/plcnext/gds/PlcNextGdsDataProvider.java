package io.openems.edge.phoenixcontact.plcnext.gds;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.stream.Stream;

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
import io.openems.edge.phoenixcontact.plcnext.PlcNextDevice;
import io.openems.edge.phoenixcontact.plcnext.auth.PlcNextTokenManager;
import io.openems.edge.phoenixcontact.plcnext.gds.enums.PlcNextGdsDataVariableDefinition;
import io.openems.edge.phoenixcontact.plcnext.utils.PlcNextUrlStringHelper;

@Component(scope = ServiceScope.SINGLETON, service = PlcNextGdsDataProvider.class)
public class PlcNextGdsDataProvider {

	public static final String PATH_VARIABLES = "/variables";
	public static final String PATH_SESSIONS = "/sessions";

	public static final String PLC_NEXT_DEFAULT_STATION_ID = "10";
	public static final String PLC_NEXT_DEFAULT_TIMEOUT_IN_MILLIS = "50000";

	public static final String PLC_NEXT_VARIABLES = "variables";

	static record PlcNextCreateSessionResponse(String sessionId, Duration sessionTimeout) {
		public PlcNextCreateSessionResponse {
			Objects.requireNonNull(sessionId, "SessionId of PlcNextCreateSessionResponse must not be null!");
			Objects.requireNonNull(sessionTimeout, "SessionTimeout of PlcNextCreateSessionResponse  must not be null!");
		}
	}

	private static final Logger log = LoggerFactory.getLogger(PlcNextGdsDataProvider.class);

	private final BridgeHttp http;
	private final PlcNextTokenManager tokenManager;
	private final PlcNextGdsDataToChannelMapper gdsDataToChannelMapper;
	private final HttpBridgeTimeService timeService;

	String sessionId;
	TimeEndpoint maintainSessionTimeEndpoint;

	@Activate
	public PlcNextGdsDataProvider(@Reference(scope = ReferenceScope.PROTOTYPE_REQUIRED) BridgeHttp http,
			@Reference(scope = ReferenceScope.BUNDLE) PlcNextTokenManager tokenManager,
			@Reference(scope = ReferenceScope.BUNDLE) PlcNextGdsDataToChannelMapper gdsDataToChannelMapper) {
		this.http = http;
		this.tokenManager = tokenManager;
		this.gdsDataToChannelMapper = gdsDataToChannelMapper;
		this.timeService = http.createService(HttpBridgeTimeServiceDefinition.INSTANCE);
	}

	/**
	 * Fetch data and fill corresponding channels
	 * 
	 * @param instanceName name of the instance to pickup the data for
	 */
	public synchronized void readFromApiToChannels(PlcNextGdsDataProviderConfig config) {
		if (!tokenManager.hasValidToken()) {
			log.warn("No valid access token! Skipping data fetch.");
			return;
		}
		log.debug("Session ID: " + sessionId);

		Optional<PlcNextCreateSessionResponse> createSessionResponse = createSessionIfNecessary(config);
		if (createSessionResponse.isPresent()) {
			this.sessionId = createSessionResponse.get().sessionId();
			this.maintainSessionTimeEndpoint = triggerSessionMaintenanceIfNecessary(
					Delay.of(createSessionResponse.get().sessionTimeout()), config).orElse(null);
		}

		try {
			Endpoint dataEndPoint = buildDataEndpointRepresentation(tokenManager.getToken(), sessionId,
					PlcNextGdsDataVariableDefinition.values(), config);
			log.debug("Fetching GDS data from endpoint: '" + dataEndPoint.url() + "'");

			JsonObject apiResponseBody = http.requestJson(dataEndPoint).thenApply(dataResponse -> {
				if (HttpStatus.OK == dataResponse.status()) {
					return dataResponse.data().getAsJsonObject();
				} else {
					log.error("Data endpoint responds with status: '" + dataResponse.status() + "' and body: '"
							+ dataResponse.data() + "'");
					return null;
				}
			}).join();

			List<PlcNextGdsDataMappedValue> mappedValues = gdsDataToChannelMapper.mapAllValuesToChannels(
					apiResponseBody.getAsJsonArray(PLC_NEXT_VARIABLES), config.dataInstanceName());

			if (!mappedValues.isEmpty()) {
				for (PlcNextGdsDataMappedValue mappedValue : mappedValues) {
					setNextValueToChannel(mappedValue, config.device());
				}
			}
		} catch (CompletionException e) {
			log.error("Error while fetching access token!", e);
		} catch (PlcNextGdsDataMappingException e) {
			log.error("Mapping error!", e);
		}
	}

	/**
	 * Deactivates session maintenance mechanism
	 */
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
	 * Creates new session and triggers session maintenance while access token is
	 * valid and no error occurs when there is no active session. It sets the member
	 * variables "sessionId" and "maintainSessionEndpoint".
	 * 
	 * @param config
	 * @return
	 */
	Optional<PlcNextCreateSessionResponse> createSessionIfNecessary(PlcNextGdsDataProviderConfig config) {
		Optional<PlcNextCreateSessionResponse> createSessionResponse = Optional.empty();

		if (Objects.isNull(this.sessionId)) {
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

	/**
	 * 
	 * @param timeoutDuration
	 * @param config
	 * @return
	 */
	Optional<TimeEndpoint> triggerSessionMaintenanceIfNecessary(Delay sessionTimeout,
			PlcNextGdsDataProviderConfig config) {
		Optional<TimeEndpoint> newMaintainSessionTimeEndpoint = Optional.empty();

		if (Objects.isNull(this.maintainSessionTimeEndpoint)) {
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
							log.info("Got unprocessable result with status '" + httpResponse.status() + "' and body '" + httpResponse.data() + "'");
							deactivateSessionMaintenanceIfNecessary();
							log.error("Session maintenance entered state UNDEFINED! Session ID has been reset.");
						}
					});
			newMaintainSessionTimeEndpoint = Optional.of(recurringSessionMaintenanceEndpoint);
		}
		return newMaintainSessionTimeEndpoint;
	}

	/**
	 * Build endpoint to be used to create a session
	 * 
	 * @param config config of base URL and instance name
	 * @return @link{Endpoint} object
	 */
	public Endpoint buildCreateSessionEndpoint(String authToken, PlcNextGdsDataProviderConfig config) {
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
	 * @param config config of base URL and instance name
	 * @return @link{Endpoint} object
	 */
	public Endpoint buildMaintainSessionEndpoint(String authToken, String sessionId,
			PlcNextGdsDataProviderConfig config) {
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
	 * @param variableDefinitions variables to fetch data for
	 * @param config              config of base URL and instance name
	 * @return @link{Endpoint} object
	 */
	public Endpoint buildDataEndpointRepresentation(String authToken, String sessionId,
			PlcNextGdsDataVariableDefinition[] variableDefinitions, PlcNextGdsDataProviderConfig config) {
		String dataEndpointUrl = PlcNextUrlStringHelper.buildUrlString(config.dataUrl(), PATH_VARIABLES);
		Map<String, String> headers = Map.of("Accept", "application/json", //
				"Content-Type", "application/json");
		if (Objects.nonNull(authToken)) {
			headers = new HashMap<String, String>(headers);
			headers.put("Authorization", "Bearer " + authToken);
			headers = Collections.unmodifiableMap(headers);
		}

		String postRequestBody = "";
		if (Objects.nonNull(variableDefinitions) && variableDefinitions.length > 0) {
			List<String> variablenames = Stream.of(variableDefinitions)//
					.map(item -> new StringBuilder(config.dataInstanceName())//
							.append(".").append(item.getPlcNextChannel()) //
							.append(".").append(item.getIdentifier())//
							.toString())//
					.toList();

			StringBuilder postRequestBodyBuilder = new StringBuilder("pathPrefix=")//
					.append(PlcNextGdsDataProviderConfig.PLC_NEXT_OPENEMS_COMPONENT_NAME)//
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

	/**
	 * Writes value fetched from PLCnext GDS to device channel
	 * 
	 * @param mappedValue represents a value object containing channel ID and value
	 *                    to set to channel
	 * @param device      represents the device holding the channels
	 */
	void setNextValueToChannel(PlcNextGdsDataMappedValue mappedValue, PlcNextDevice device) {
		log.info("Providing value '" + mappedValue.getValue() + "' to channel named '" + mappedValue.getChannelId()
				+ "'");
		device.channel(mappedValue.getChannelId()).setNextValue(mappedValue.getValue());
	}
}
