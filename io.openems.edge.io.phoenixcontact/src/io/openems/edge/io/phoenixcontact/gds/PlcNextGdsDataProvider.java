package io.openems.edge.io.phoenixcontact.gds;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
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
import io.openems.common.types.HttpStatus;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.io.phoenixcontact.auth.PlcNextTokenManager;
import io.openems.edge.io.phoenixcontact.gds.enums.PlcNextGdsDataVariableDefinition;

@Component(scope = ServiceScope.SINGLETON, service = PlcNextGdsDataProvider.class)
public class PlcNextGdsDataProvider {
	
	public static final String PLC_NEXT_VARIABLES = "variables";

	private static final Logger log = LoggerFactory.getLogger(PlcNextGdsDataProvider.class);

	private final BridgeHttp http;
	private final PlcNextTokenManager tokenManager;
	private final PlcNextGdsDataToChannelMapper gdsDataToChannelMapper;
	
	@Activate
	public PlcNextGdsDataProvider(@Reference(scope = ReferenceScope.PROTOTYPE_REQUIRED) BridgeHttp http,
			@Reference(scope = ReferenceScope.BUNDLE) PlcNextTokenManager tokenManager,
			@Reference(scope = ReferenceScope.BUNDLE) PlcNextGdsDataToChannelMapper gdsDataToChannelMapper) {
		this.http = http;
		this.tokenManager = tokenManager;
		this.gdsDataToChannelMapper = gdsDataToChannelMapper;
	}
	
	/**
	 * Fetch data and fill corresponding channels
	 * 
	 * @param instanceName name of the instance to pickup the data for
	 */
	public void readFromApiToChannels(PlcNextGdsDataProviderConfig config) {
		// TODO: integrate session handling (PLC-34)

		if (!Objects.nonNull(tokenManager.getToken())) {
			log.warn("No access token! Skipping data fetch.");
			return;
		}
		
		JsonObject apiResponseBody = null;
		
		try {
			Endpoint dataEndPoint = buildDataEndpointRepresentation(PlcNextGdsDataVariableDefinition.values(), config);
			log.debug("Fetching GDS data from endpoint: '" + dataEndPoint.url() + "'");

			CompletableFuture<JsonObject> dataAspectValue = http.requestJson(dataEndPoint).thenApply(dataResponse -> {
				if (HttpStatus.OK == dataResponse.status()) {
					return dataResponse.data().getAsJsonObject();
				} else {
					log.error("Data endpoint responds with status: '" + dataResponse.status() + "' and body: '"
							+ dataResponse.data() + "'");
					return null;
				}
			});
			apiResponseBody = dataAspectValue.join();

			List<PlcNextGdsDataMappedValue> mappedValues = gdsDataToChannelMapper.mapAllAspectsToChannel(
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

	void setNextValueToChannel(PlcNextGdsDataMappedValue mappedValue, Map<ChannelId, Channel<?>> channelId2ChannelMap) {
		Channel<?> destinationChannel = channelId2ChannelMap.get(mappedValue.getChannelId());

		if (Objects.isNull(destinationChannel)) {
			log.warn("Channel for ID '" + mappedValue.getChannelId() + "' not found! Cannot write value.");
		} else {
			log.info("Providing value '" + mappedValue.getValue() + "' to channel named '" + mappedValue.getChannelId()
					+ "'");
			destinationChannel.setNextValue(mappedValue.getValue());
		}
	}

	Endpoint buildDataEndpointRepresentation(PlcNextGdsDataVariableDefinition[] variableDefinitions,
			PlcNextGdsDataProviderConfig config) {
		String dataEndpointUrl = config.dataUrl();

		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Accept", "application/json");
		headers.put("Content-Type", "application/json");
		if (Objects.nonNull(tokenManager.getToken())) {
			headers.put("Authorization", "Bearer " + this.tokenManager.getToken());
		}

		String postRequestBody = "";
		if (Objects.nonNull(variableDefinitions) && variableDefinitions.length > 0) {
			List<String> variablenames = Stream.of(variableDefinitions)//
					.map(item -> new StringBuilder(config.dataInstanceName())//
							.append(".").append(item.getPlcNextChannel()) //
							.append(".").append(item.getIdentifier())//
							.toString())//
					.toList();

			postRequestBody = new StringBuilder("pathPrefix=")//
					.append(PlcNextGdsDataProviderConfig.PLC_NEXT_OPENEMS_COMPONENT_NAME)//
					.append("/&paths=")//
					.append(String.join(",", variablenames))//
					.toString();
		}
		Endpoint endPoint = new Endpoint(dataEndpointUrl, HttpMethod.POST, BridgeHttp.DEFAULT_CONNECT_TIMEOUT,
				BridgeHttp.DEFAULT_READ_TIMEOUT, postRequestBody, headers);

		return endPoint;
	}

}
