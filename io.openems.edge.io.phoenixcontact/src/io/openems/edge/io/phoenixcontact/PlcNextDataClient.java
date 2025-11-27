package io.openems.edge.io.phoenixcontact;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceScope;
import org.osgi.service.component.annotations.ServiceScope;

import com.google.gson.JsonObject;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttp.Endpoint;
import io.openems.common.bridge.http.api.HttpMethod;
import io.openems.common.types.OpenemsType;
import io.openems.edge.io.phoenixcontact.auth.PlcNextTokenManager;
import io.openems.edge.io.phoenixcontact.utils.PlcNextJsonElementHelper;

@Component(scope = ServiceScope.SINGLETON, service = PlcNextDataClient.class)
public class PlcNextDataClient {
	private final BridgeHttp http;
	private final PlcNextTokenManager tokenManager;
	private final Config config;

	@Activate
	public PlcNextDataClient(@Reference(scope = ReferenceScope.PROTOTYPE_REQUIRED) BridgeHttp http,
			@Reference(scope = ReferenceScope.PROTOTYPE_REQUIRED) PlcNextTokenManager tokenManager, Config config) {
		this.http = http;
		this.tokenManager = tokenManager;
		this.config = config;
	}

	/**
	 * Fetches a single data aspect and returns it
	 * 
	 * @param instanceName represents the GDS namespace instance to query
	 * @param aspectName   represents the name of the field to fetch a value for
	 * @return an Object containing the value of the given aspect
	 */
	public Object fetchSingleGdsDataAspect(String instanceName, String aspectName, OpenemsType type) {
		Endpoint dataEndPoint = buildDataEndpointRepresentation(instanceName);
		CompletableFuture<Object> dataAspectValue = http.requestJson(dataEndPoint)
				.thenApply(s -> PlcNextJsonElementHelper.getJsonValue(s.data().getAsJsonObject().getAsJsonPrimitive(aspectName), type));

		return dataAspectValue.join();
	}

	/**
	 * Fetch all data aspects of given instance name and returns it
	 * 
	 * @param instanceName represents the GDS namespace instance to query
	 * @return a JsonObject containing all fetched data
	 */
	public JsonObject fetchAllGdsDataAspects(String instanceName) {
		Endpoint dataEndPoint = buildDataEndpointRepresentation(instanceName);
		CompletableFuture<JsonObject> dataAspectValue = http.requestJson(dataEndPoint)
				.thenApply(s -> s.data().getAsJsonObject());

		return dataAspectValue.join();
	}

	private Endpoint buildDataEndpointRepresentation(String instanceName) {
		String dataEndpointUrl = buildDataEndpointUrl(instanceName);

		Map<String, String> headers = Map.of(//
				"Authorization", "Bearer " + this.tokenManager.getToken(), "Accept", "application/json", "Content-Type",
				"application/json");
		String requestBody = "{'todo': 'implement me!'}";
		Endpoint endPoint = new Endpoint(dataEndpointUrl, HttpMethod.POST, BridgeHttp.DEFAULT_CONNECT_TIMEOUT,
				BridgeHttp.DEFAULT_READ_TIMEOUT, requestBody, headers);

		return endPoint;
	}

	private String buildDataEndpointUrl(String instanceName) {
		String dataEndpointUrl = this.config.dataUrl();

		if (!dataEndpointUrl.endsWith("/")) {
			dataEndpointUrl = dataEndpointUrl.concat("/");
		}
		dataEndpointUrl = dataEndpointUrl.concat(instanceName)
				.concat("/").concat("data");

		return dataEndpointUrl;
	}

}
