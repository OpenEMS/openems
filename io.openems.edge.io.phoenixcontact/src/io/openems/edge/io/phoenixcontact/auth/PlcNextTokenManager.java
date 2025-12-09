package io.openems.edge.io.phoenixcontact.auth;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Objects;
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
import io.openems.common.types.HttpStatus;
import io.openems.edge.io.phoenixcontact.utils.PlcNextUrlStringHelper;

@Component(scope = ServiceScope.SINGLETON, service = PlcNextTokenManager.class)
public class PlcNextTokenManager {

	public static final String PATH_AUTH_TOKEN = "/auth-token";
	public static final String PATH_ACCESS_TOKEN = "/access-token";

	private static final Logger log = LoggerFactory.getLogger(PlcNextTokenManager.class);

	private final BridgeHttp http;

	private String token;
	private ZonedDateTime tokenExpirery;

	@Activate
	public PlcNextTokenManager(@Reference(scope = ReferenceScope.PROTOTYPE_REQUIRED) BridgeHttp http) {
		this.http = http;
	}

	/**
	 * 
	 * @return the cached token
	 */
	public synchronized String getToken() {
		return this.token;
	}

	/**
	 * Initialize fetching valid JWT periodically
	 * 
	 * @param authClientConfig configuration to be used
	 */
	public synchronized void fetchToken(PlcNextTokenManagerConfig authClientConfig) {
		if (isTokenRequestAllowed()) {
			log.info("Start fetching authentication");
			try {
				PlcNextAuthAndAccessTokenDTO combinedToken = null;
				CompletableFuture<PlcNextAuthAndAccessTokenDTO> authTokenFuture = fetchAuthToken(authClientConfig)
						.thenCompose(code -> fetchAccessToken(code, authClientConfig));

				combinedToken = authTokenFuture.join();
				if (Objects.nonNull(combinedToken)) {
					token = combinedToken.getAccessToken();
					tokenExpirery = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS)
							.plusSeconds(combinedToken.getExpiresIn());
				}
			} catch (CompletionException e) {
				log.error("Error while fetching access token!", e);
			}
			log.info("Fetching authentication finished. Got access token? " + Objects.nonNull(this.token));
		} else {
			log.info("Token still valid, skipping token refresh.");
		}
	}

	private boolean isTokenRequestAllowed() {
		return Objects.isNull(tokenExpirery) || tokenExpirery.isBefore(ZonedDateTime.now());
	}

	Endpoint buildAuthTokenEndpointRepresentation(PlcNextTokenManagerConfig config) {
		String requestBody = "{\"scope\":\"variables\" }";
		Map<String, String> headers = Map.of(//
				"Content-Type", "application/json");
		String authTokenUrlString = PlcNextUrlStringHelper.buildUrlString(config.authUrl(), PATH_AUTH_TOKEN);

		return new Endpoint(authTokenUrlString, HttpMethod.POST, BridgeHttp.DEFAULT_CONNECT_TIMEOUT,
				BridgeHttp.DEFAULT_READ_TIMEOUT, requestBody, headers);
	}

	CompletableFuture<PlcNextAuthAndAccessTokenDTO> fetchAuthToken(PlcNextTokenManagerConfig config) {
		Endpoint authTokenEndpoint = buildAuthTokenEndpointRepresentation(config);
		log.debug("Fetching auth token from endpoint: '" + authTokenEndpoint.url() + "'");

		return http.requestJson(authTokenEndpoint).thenApply(authTokenResponse -> {

			if (HttpStatus.OK == authTokenResponse.status()) {
				JsonObject responseBody = authTokenResponse.data().getAsJsonObject();

				return new PlcNextAuthAndAccessTokenDTO(responseBody.getAsJsonPrimitive("code").getAsString(), //
						responseBody.getAsJsonPrimitive("expires_in").getAsInt());
			} else {
				log.error("Auth token endpoint responds with status: '" + authTokenResponse.status() + "' and body: '"
						+ authTokenResponse.data() + "'");

				return null;
			}
		});
	}

	Endpoint buildAccessTokenEndpointRepresentation(PlcNextAuthAndAccessTokenDTO authAndAccessToken,
			PlcNextTokenManagerConfig config) {
		String requestBody = "{ \"code\": \"" + authAndAccessToken.getCode() + "\", "
				+ "\"grant_type\": \"authorization_code\", " + "\"username\": \"" + config.username() + "\", "
				+ "\"password\": \"" + config.password() + "\" }";
		Map<String, String> headers = Map.of(//
				"Content-Type", "application/json");
		String accessTokenUrlString = PlcNextUrlStringHelper.buildUrlString(config.authUrl(), PATH_ACCESS_TOKEN);

		return new Endpoint(accessTokenUrlString, HttpMethod.POST, BridgeHttp.DEFAULT_CONNECT_TIMEOUT,
				BridgeHttp.DEFAULT_READ_TIMEOUT, requestBody, headers);
	}

	CompletableFuture<PlcNextAuthAndAccessTokenDTO> fetchAccessToken(PlcNextAuthAndAccessTokenDTO authToken,
			PlcNextTokenManagerConfig config) {
		Endpoint accessTokenEndpoint = buildAccessTokenEndpointRepresentation(authToken, config);
		log.debug("Fetching access token from endpoint: '" + accessTokenEndpoint.url() + "', " + "with body: '"
				+ accessTokenEndpoint.body() + "'");

		return http.requestJson(accessTokenEndpoint).thenApply(accessTokenResponse -> {
			if (HttpStatus.OK == accessTokenResponse.status()) {
				PlcNextAuthAndAccessTokenDTO extendedAccessToken = new PlcNextAuthAndAccessTokenDTO(authToken.getCode(),
						authToken.getExpiresIn());
				JsonObject responseBody = accessTokenResponse.data().getAsJsonObject();

				extendedAccessToken.setAccessToken(responseBody.getAsJsonPrimitive("access_token").getAsString());

				return extendedAccessToken;
			} else {
				log.error("Access token endpoint responds with status: '" + accessTokenResponse.status()
						+ "' and body: '" + accessTokenResponse.data() + "'");

				return null;
			}
		});
	}
}
