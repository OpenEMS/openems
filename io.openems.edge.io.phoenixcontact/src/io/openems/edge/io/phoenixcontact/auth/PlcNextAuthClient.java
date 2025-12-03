package io.openems.edge.io.phoenixcontact.auth;

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
import io.openems.common.types.HttpStatus;
import io.openems.edge.io.phoenixcontact.utils.PlcNextUrlStringHelper;

@Component(scope = ServiceScope.SINGLETON, service = PlcNextAuthClient.class)
public class PlcNextAuthClient {
	
	private static final Logger log = LoggerFactory.getLogger(PlcNextAuthClient.class);

	public static final String PATH_AUTH_TOKEN = "/auth-token";
	public static final String PATH_ACCESS_TOKEN = "/access-token";

	private final BridgeHttp http;

	@Activate
	public PlcNextAuthClient(@Reference(scope = ReferenceScope.PROTOTYPE_REQUIRED) BridgeHttp http) {
		this.http = http;
	}

	/**
	 * Fetches a single auth token
	 * 
	 * @return fetched auth token
	 */
	public String fetchSingleAuthentication(PlcNextAuthClientConfig config) {
		try {
			CompletableFuture<String> authTokenFuture = fetchAuthToken(config)
					.thenCompose(code -> fetchAccessToken(code, config));

			return authTokenFuture.join();
		} catch (CompletionException e) {
			log.error("Error while fetching access token!", e);
			return null;
		}
	}

	Endpoint buildAuthTokenEndpointRepresentation(PlcNextAuthClientConfig config) {
		String requestBody = "{\"scope\":\"variables\" }";
		Map<String, String> headers = Map.of(//
				"Content-Type", "application/json");
		String authTokenUrlString = PlcNextUrlStringHelper.buildUrlString(config.authUrl(), PATH_AUTH_TOKEN);

		return new Endpoint(authTokenUrlString, HttpMethod.POST, BridgeHttp.DEFAULT_CONNECT_TIMEOUT,
				BridgeHttp.DEFAULT_READ_TIMEOUT, requestBody, headers);
	}

	CompletableFuture<String> fetchAuthToken(PlcNextAuthClientConfig config) {
		Endpoint authTokenEndpoint = buildAuthTokenEndpointRepresentation(config);
		log.debug("Fetching auth token from endpoint: '" + authTokenEndpoint.url() + "'");

		return http.requestJson(authTokenEndpoint).thenApply(authTokenResponse -> {

			if (HttpStatus.OK == authTokenResponse.status()) {
				return authTokenResponse.data().getAsJsonObject().getAsJsonPrimitive("code").getAsString();
			} else {
				log.error("Auth token endpoint responds with status: '" + authTokenResponse.status() + "' and body: '"
						+ authTokenResponse.data() + "'");

				return null;
			}
		});
	}

	Endpoint buildAccessTokenEndpointRepresentation(String code, PlcNextAuthClientConfig config) {
		String requestBody = "{ \"code\": \"" + code + "\", " + "\"grant_type\": \"authorization_code\", "
				+ "\"username\": \"" + config.username() + "\", " + "\"password\": \"" + config.password() + "\" }";
		Map<String, String> headers = Map.of(//
				"Content-Type", "application/json");
		String accessTokenUrlString = PlcNextUrlStringHelper.buildUrlString(config.authUrl(), PATH_ACCESS_TOKEN);

		return new Endpoint(accessTokenUrlString, HttpMethod.POST, BridgeHttp.DEFAULT_CONNECT_TIMEOUT,
				BridgeHttp.DEFAULT_READ_TIMEOUT, requestBody, headers);
	}

	CompletableFuture<String> fetchAccessToken(String code, PlcNextAuthClientConfig config) {
		Endpoint accessTokenEndpoint = buildAccessTokenEndpointRepresentation(code, config);
		log.debug("Fetching access token from endpoint: '" + accessTokenEndpoint.url() + "', " + "with body: '"
				+ accessTokenEndpoint.body() + "'");

		return http.requestJson(accessTokenEndpoint).thenApply(accessTokenResponse -> {
			if (HttpStatus.OK == accessTokenResponse.status()) {
				return accessTokenResponse.data().getAsJsonObject().getAsJsonPrimitive("access_token").getAsString();
			} else {
				log.error("Access token endpoint responds with status: '" + accessTokenResponse.status()
						+ "' and body: '" + accessTokenResponse.data() + "'");

				return null;
			}
		});
	}
}
