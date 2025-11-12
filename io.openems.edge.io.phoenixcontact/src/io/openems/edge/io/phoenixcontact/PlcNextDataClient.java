package io.openems.edge.io.phoenixcontact;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceScope;
import org.osgi.service.component.annotations.ServiceScope;

import io.openems.edge.bridge.http.api.BridgeHttp;
import io.openems.edge.bridge.http.api.HttpMethod;
import io.openems.edge.io.phoenixcontact.auth.PlcNextTokenManager;
import io.openems.edge.bridge.http.api.BridgeHttp.Endpoint;

@Component(scope = ServiceScope.SINGLETON, service = PlcNextDataClient.class)
public class PlcNextDataClient {
	private final BridgeHttp http;
	private final PlcNextTokenManager tokenManager;
	private final Config config;

	@Activate
	public PlcNextDataClient(@Reference(scope = ReferenceScope.PROTOTYPE_REQUIRED) BridgeHttp http,
			@Reference(scope = ReferenceScope.PROTOTYPE_REQUIRED) PlcNextTokenManager tokenManager,
			Config config) {
		this.http = http;
		this.tokenManager = tokenManager;
		this.config = config;
	}
	
	// TODO: just a dummy implementation
	public String fetchSingleGdsDataAspect(String instanceName, String aspectName) {
		Endpoint dataEndPoint = buildDataEndpointRepresentation(instanceName);
		CompletableFuture<String> dataAspectValue = http.requestJson(dataEndPoint).thenApply(s ->
			s.data().getAsJsonObject().getAsJsonPrimitive(aspectName).getAsString()
		);
		
		return dataAspectValue.join();
	}
	
	private Endpoint buildDataEndpointRepresentation(String instanceName) {
		String dataEndpointUrl = buildDataEndpointUrl(instanceName);
		
		Map<String, String> headers = Map.of(//
				"Authorization", "Bearer " + this.tokenManager.getToken(),
				"Accept", "application/json",
				"Content-Type", "application/json");
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
		dataEndpointUrl = dataEndpointUrl.concat(instanceName);
		
		return dataEndpointUrl;
	}
	


}
