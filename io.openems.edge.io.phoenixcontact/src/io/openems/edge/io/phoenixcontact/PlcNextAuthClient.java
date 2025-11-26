package io.openems.edge.io.phoenixcontact;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceScope;
import org.osgi.service.component.annotations.ServiceScope;

import com.google.gson.JsonElement;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttp.Endpoint;
import io.openems.common.bridge.http.time.HttpBridgeTimeService;
import io.openems.common.bridge.http.time.HttpBridgeTimeServiceDefinition;
import io.openems.common.bridge.http.api.HttpError;
import io.openems.common.bridge.http.api.HttpMethod;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.edge.io.phoenixcontact.utils.PlcNextDelayTimeProvider;

@Component(scope = ServiceScope.SINGLETON, service = PlcNextAuthClient.class)
public class PlcNextAuthClient {
	
	private static final int TOKEN_FETCH_DELAY = 30;

	private final BridgeHttp http;
	private final Config config;
	private final HttpBridgeTimeService timeService;

	@Activate
	public PlcNextAuthClient(@Reference(scope = ReferenceScope.PROTOTYPE_REQUIRED) BridgeHttp http, Config config) {
		this.http = http;
		this.config = config;
		this.timeService = http.createService(HttpBridgeTimeServiceDefinition.INSTANCE);
	}

	/**
	 * Fetches a single auth token
	 * 
	 * @return fetched auth token
	 */
	public String fetchSingleAuthentication() {
		Endpoint authEndPoint = buildAuthenticationEndpointRepresentation();
		CompletableFuture<String> authToken = http.requestJson(authEndPoint)
				.thenApply(s -> s.data().getAsJsonObject().getAsJsonPrimitive("jwtToken").getAsString());

		return authToken.join();
	}

	/**
	 * Registers a time based subscription refreshing the auth token in intervals
	 * 
	 * @param action	represents an action processing the response
	 */
	public void fetchAuthenticationPeriodically(BiConsumer<HttpResponse<JsonElement>, HttpError> action) {
		Endpoint authEndpoint = buildAuthenticationEndpointRepresentation();

		this.timeService.subscribeJsonTime(new PlcNextDelayTimeProvider(TOKEN_FETCH_DELAY), authEndpoint, action);
	}

	private Endpoint buildAuthenticationEndpointRepresentation() {
		String requestBody = "{userCredentials: {name: \"" + config.username() + "\" , password: \"" + config.password()
				+ "\"}}";
		Map<String, String> headers = Map.of(//
				"Content-Type", "application/json");
		Endpoint endPoint = new Endpoint(config.authUrl(), HttpMethod.POST, BridgeHttp.DEFAULT_CONNECT_TIMEOUT,
				BridgeHttp.DEFAULT_READ_TIMEOUT, requestBody, headers);
		return endPoint;
	}

}
