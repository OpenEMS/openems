package io.openems.edge.io.phoenixcontact.gds;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
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
import io.openems.common.types.OpenemsType;
import io.openems.edge.io.phoenixcontact.auth.PlcNextTokenManager;
import io.openems.edge.io.phoenixcontact.utils.PlcNextJsonElementHelper;

@Component(scope = ServiceScope.SINGLETON, service = PlcNextGdsDataClient.class)
public class PlcNextGdsDataClient {

	private static final Logger log = LoggerFactory.getLogger(PlcNextGdsDataClient.class);

	private final BridgeHttp http;
	private final PlcNextTokenManager tokenManager;

	@Activate
	public PlcNextGdsDataClient(@Reference(scope = ReferenceScope.PROTOTYPE_REQUIRED) BridgeHttp http,
			@Reference(scope = ReferenceScope.BUNDLE) PlcNextTokenManager tokenManager) {
		this.http = http;
		this.tokenManager = tokenManager;
	}

	/**
	 * Fetches a single data aspect and returns it
	 * 
	 * @param config     represents the GDS configuration
	 * @param aspectName represents the name of the field to fetch a value for
	 * @return an Object containing the value of the given aspect
	 */
	public Object fetchSingleGdsDataAspect(PlcNextGdsDataClientConfig config, String aspectName, OpenemsType type) {
		try {
			Endpoint dataEndPoint = buildDataEndpointRepresentation(config);
			CompletableFuture<Object> dataAspectValue = http.requestJson(dataEndPoint)
					.thenApply(s -> PlcNextJsonElementHelper
							.getJsonValue(s.data().getAsJsonObject().getAsJsonPrimitive(aspectName), type));

			return dataAspectValue.join();
		} catch (CompletionException e) {
			log.error("Error while fetching data from GDS!", e);
			return null;
		}
	}

	/**
	 * Fetch all data aspects of given instance name and returns it
	 * 
	 * @param config represents the GDS configuration
	 * @return a JsonObject containing all fetched data
	 */
	public JsonObject fetchAllGdsDataAspects(PlcNextGdsDataClientConfig config) {
		Endpoint dataEndPoint = buildDataEndpointRepresentation(config);
		CompletableFuture<JsonObject> dataAspectValue = http.requestJson(dataEndPoint)
				.thenApply(s -> s.data().getAsJsonObject());

		return dataAspectValue.join();
	}

	private Endpoint buildDataEndpointRepresentation(PlcNextGdsDataClientConfig config) {
		String dataEndpointUrl = buildDataEndpointUrl(config);

		Map<String, String> headers = Map.of(//
				"Authorization", "Bearer " + this.tokenManager.getToken(), "Accept", "application/json", "Content-Type",
				"application/json");
		String requestBody = "{'todo': 'implement me!'}";
		Endpoint endPoint = new Endpoint(dataEndpointUrl, HttpMethod.POST, BridgeHttp.DEFAULT_CONNECT_TIMEOUT,
				BridgeHttp.DEFAULT_READ_TIMEOUT, requestBody, headers);

		return endPoint;
	}

	private String buildDataEndpointUrl(PlcNextGdsDataClientConfig config) {
		String dataEndpointUrl = config.dataUrl();

		if (!dataEndpointUrl.endsWith("/")) {
			dataEndpointUrl = dataEndpointUrl.concat("/");
		}
		dataEndpointUrl = dataEndpointUrl.concat(config.dataInstanceName())
				.concat("/").concat("data");

		return dataEndpointUrl;
	}

}
