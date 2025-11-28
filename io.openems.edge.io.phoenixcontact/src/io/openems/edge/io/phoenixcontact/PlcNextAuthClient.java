package io.openems.edge.io.phoenixcontact;

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

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttp.Endpoint;
import io.openems.common.bridge.http.api.HttpMethod;
import io.openems.edge.io.phoenixcontact.utils.PlcNextUrlStringHelper;

@Component(scope = ServiceScope.SINGLETON, service = PlcNextAuthClient.class)
public class PlcNextAuthClient {
	
	private static final Logger log = LoggerFactory.getLogger(PlcNextAuthClient.class);

	public static final String PATH_AUTH_TOKEN = "/auth-token";
	public static final String PATH_ACCESS_TOKEN = "/access-token";

	private final BridgeHttp http;
	private final Config config;

	@Activate
	public PlcNextAuthClient(@Reference(scope = ReferenceScope.PROTOTYPE_REQUIRED) BridgeHttp http, Config config) {
		this.http = http;
		this.config = config;
	}

	/**
	 * Fetches a single auth token
	 * 
	 * @return fetched auth token
	 */
	public String fetchSingleAuthentication() {
		try {
			Endpoint authTokenEndPoint = buildAuthTokenEndpointRepresentation();
			CompletableFuture<String> authTokenFuture = http.requestJson(authTokenEndPoint)
					.thenApply(authTokenResponse -> authTokenResponse.data().getAsJsonObject()
							.getAsJsonPrimitive("code").getAsString())
					.thenCompose(code -> http.requestJson(buildAccessTokenEndpointRepresentation(code))
							.thenApply(accessTokenResponse -> accessTokenResponse.data().getAsJsonObject()
									.getAsJsonPrimitive("access_token").getAsString()));

			return authTokenFuture.join();
		} catch (CompletionException e) {
			log.error("Error while fetching auth or access token!", e);
			return null;
		}
	}

	Endpoint buildAuthTokenEndpointRepresentation() {
		String requestBody = "{\"scope\":\"variables\" }";
		Map<String, String> headers = Map.of(//
				"Content-Type", "application/json");
		String authTokenUrlString = PlcNextUrlStringHelper.buildUrlString(config.authUrl(), PATH_AUTH_TOKEN);

		return new Endpoint(authTokenUrlString, HttpMethod.POST, BridgeHttp.DEFAULT_CONNECT_TIMEOUT,
				BridgeHttp.DEFAULT_READ_TIMEOUT, requestBody, headers);
	}

	Endpoint buildAccessTokenEndpointRepresentation(String code) {
		String requestBody = "{ \"code\": \"" + code + "\", " + "\"grant_type\": \"authorization_code\", "
				+ "\"username\": \"" + config.username() + "\", " + "\"password\": \"" + config.password() + "\" }";
		Map<String, String> headers = Map.of(//
				"Content-Type", "application/json");
		String accessTokenUrlString = PlcNextUrlStringHelper.buildUrlString(config.authUrl(), PATH_ACCESS_TOKEN);

		return new Endpoint(accessTokenUrlString, HttpMethod.POST, BridgeHttp.DEFAULT_CONNECT_TIMEOUT,
				BridgeHttp.DEFAULT_READ_TIMEOUT, requestBody, headers);
	}

}
