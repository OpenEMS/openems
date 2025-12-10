package io.openems.edge.phoenixcontact.plcnext.gds;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionException;
import java.util.function.Function;
import java.util.stream.Collectors;
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
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.ChannelId;
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

	private static final Logger log = LoggerFactory.getLogger(PlcNextGdsDataProvider.class);

	private final BridgeHttp http;
	private final PlcNextTokenManager tokenManager;
	private final PlcNextGdsDataToChannelMapper gdsDataToChannelMapper;
	private final HttpBridgeTimeService timeService;

	private String sessionId;
	private TimeEndpoint maintainSessionTimeEndpoint;

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
		maintainSessionTimeEndpoint = createSessionIfNecessary(maintainSessionTimeEndpoint, config);
		
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
				Map<ChannelId, Channel<?>> channelId2ChannelMap = config.channels().stream()
						.collect(Collectors.toMap(Channel::channelId, Function.identity()));

				for (PlcNextGdsDataMappedValue mappedValue : mappedValues) {
					setNextValueToChannel(mappedValue, channelId2ChannelMap);
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
		deativateTimeEndpointIfNecessary(maintainSessionTimeEndpoint);
	}

	private void deativateTimeEndpointIfNecessary(TimeEndpoint timeEndpoint) {
		if (Objects.nonNull(timeEndpoint)) {
			timeService.removeTimeEndpoint(timeEndpoint);
		}
	}

	/**
	 * 
	 */
	synchronized TimeEndpoint createSessionIfNecessary(TimeEndpoint oldTimeEndpoint,
			PlcNextGdsDataProviderConfig config) {
		log.debug("Time endpoint: " + oldTimeEndpoint);
		log.debug("Session ID: " + sessionId);

		TimeEndpoint maintainSessionTimeEndpoint = oldTimeEndpoint;

		if (Objects.isNull(sessionId)) {
			// remove old time endpoint if necessary
			deativateTimeEndpointIfNecessary(maintainSessionTimeEndpoint);

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

			sessionId = createSessionBody.get("sessionID").getAsString();
			Delay sessionTimeout = Delay.of(Duration.ofMillis(createSessionBody.get("timeout").getAsLong()));

			// trigger session maintenance
			DelayTimeProvider delayTimeProvider = new DefaultDelayTimeProvider(() -> sessionTimeout, //
					(error) -> Delay.infinite(), //
					(result) -> sessionTimeout);
			Endpoint maintainSessionEndpoint = buildMaintainSessionEndpoint(tokenManager.getToken(), sessionId, config);
			log.info("Maintaining session using endpoint: " + maintainSessionEndpoint);

			maintainSessionTimeEndpoint = this.timeService.subscribeJsonTime(delayTimeProvider, maintainSessionEndpoint,
					(httpResponse, httpError) -> {
						if (Objects.isNull(httpResponse) && Objects.isNull(httpError)) {
							log.info("No result while maintaining session. Skipping processing.");
							return;
						} else if (Objects.nonNull(httpError)) {
							log.error("Got HTTP error '" + httpError + "'! Resetting session ID.");
							this.sessionId = null;
						} else if (Objects.nonNull(httpResponse) && !tokenManager.hasValidToken()) {
							log.info("Got result, but access token has been expired. Resetting session ID.");
							this.sessionId = null;
						} else if (Objects.nonNull(httpResponse) && Objects.nonNull(httpResponse.data())) {
							log.info("Maintaining session has been successful.");
							this.sessionId = httpResponse.data().getAsJsonObject().get("sessionID").getAsString();
						} else {
							log.warn("Undefined state while maintaining the session!");
						}
			});
		}
		return maintainSessionTimeEndpoint;
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
			PlcNextGdsDataVariableDefinition[] variableDefinitions,
			PlcNextGdsDataProviderConfig config) {
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
	 * 
	 * @param mappedValue
	 * @param channelId2ChannelMap
	 */
	void setNextValueToChannel(PlcNextGdsDataMappedValue mappedValue, Map<ChannelId, Channel<?>> channelId2ChannelMap) {
		Channel<?> destinationChannel = channelId2ChannelMap.get(mappedValue.getChannelId());

		if (Objects.isNull(destinationChannel)) {
			log.warn("Channel for ID '" + mappedValue.getChannelId() + "' not found! Cannot write value.");
		} else {
			log.debug("Providing value '" + mappedValue.getValue() + "' to channel named '" + mappedValue.getChannelId()
					+ "'");
			destinationChannel.setNextValue(mappedValue.getValue());
		}
	}
}
