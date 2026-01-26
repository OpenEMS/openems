package io.openems.edge.phoenixcontact.plcnext.common.auth;

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
import io.openems.edge.phoenixcontact.plcnext.common.utils.PlcNextUrlStringHelper;

@Component(scope = ServiceScope.PROTOTYPE)
public class PlcNextTokenManagerImpl implements PlcNextTokenManager {

	private static final Logger log = LoggerFactory.getLogger(PlcNextTokenManagerImpl.class);

	private final BridgeHttp http;

	private String token;
	private ZonedDateTime tokenExpiery;

	@Activate
	public PlcNextTokenManagerImpl(@Reference(scope = ReferenceScope.PROTOTYPE_REQUIRED) BridgeHttp http) {
		this.http = http;
	}

	/**
	 * 
	 * @return the cached token
	 */
	@Override
	public synchronized String getToken() {
		return this.token;
	}

	/**
	 * Initialize fetching valid JWT periodically
	 * 
	 * @param authClientConfig configuration to be used
	 */
	@Override
	public synchronized CompletableFuture<Void> fetchToken(PlcNextAuthConfig authClientConfig) {
		if (!hasValidToken()) {
			log.info("Start fetching authentication");
			try {
				return fetchAuthToken(authClientConfig)
						.thenCompose(code -> fetchAccessToken(code, authClientConfig))
						.thenApply(combinedToken -> {
							if (Objects.nonNull(combinedToken) && Objects.nonNull(combinedToken.getAccessToken())) {
								log.debug("Fetching access token has been successful.");
								token = combinedToken.getAccessToken();
								tokenExpiery = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS)
										.plusSeconds(combinedToken.getExpiresIn());
							} else if (Objects.isNull(combinedToken)) {
								log.error("No token information returned!");
								resetTokenAndExpiery();
							} else {
								log.error("No access token or expiery information returned!");
								resetTokenAndExpiery();
							}
							log.info("Fetching authentication finished. Got access token? {}", Objects.nonNull(this.token));
							return null;
						});
			} catch (CompletionException e) {
				log.error("Error while fetching access token!", e);
				resetTokenAndExpiery();
				return CompletableFuture.completedFuture(null);
			}
		} else {
			log.info("Token still valid, skipping token refresh.");
			return CompletableFuture.completedFuture(null);
		}
	}
	
	private void resetTokenAndExpiery() {
		log.info("Resetting token and token expiery");
		token = null;
		tokenExpiery = null;		
	}

	/**
	 * Checks if a valid token has been fetched.
	 * 
	 * @return TRUE if token is valid, FALSE otherwise
	 */
	@Override
	public synchronized boolean hasValidToken() {
		return Objects.nonNull(this.token) && //
				Objects.nonNull(tokenExpiery) && !tokenExpiery.isBefore(ZonedDateTime.now());
	}

	Endpoint buildAuthTokenEndpointRepresentation(PlcNextAuthConfig config) {
		String requestBody = "{\"scope\":\"variables\" }";
		Map<String, String> headers = Map.of(//
				"Content-Type", "application/json");
		String authTokenUrlString = PlcNextUrlStringHelper.buildUrlString(config.authUrl(), PATH_AUTH_TOKEN);

		return new Endpoint(authTokenUrlString, HttpMethod.POST, BridgeHttp.DEFAULT_CONNECT_TIMEOUT,
				BridgeHttp.DEFAULT_READ_TIMEOUT, requestBody, headers);
	}

	CompletableFuture<PlcNextAuthAndAccessTokenDTO> fetchAuthToken(PlcNextAuthConfig config) {
		Endpoint authTokenEndpoint = buildAuthTokenEndpointRepresentation(config);
		log.info("Fetching auth token from endpoint: '{}'", authTokenEndpoint.url());

		return http.requestJson(authTokenEndpoint).thenApply(authTokenResponse -> {
			if (HttpStatus.OK == authTokenResponse.status()) {
				JsonObject responseBody = authTokenResponse.data().getAsJsonObject();

				return new PlcNextAuthAndAccessTokenDTO(responseBody.getAsJsonPrimitive("code").getAsString(), //
						responseBody.getAsJsonPrimitive("expires_in").getAsInt());
			} else {
				log.error("Auth token endpoint responds with status: '{}' and body: '{}'", authTokenResponse.status(),
						authTokenResponse.data());

				return null;
			}
		});
	}

	Endpoint buildAccessTokenEndpointRepresentation(PlcNextAuthAndAccessTokenDTO authAndAccessToken,
			PlcNextAuthConfig config) {
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
			PlcNextAuthConfig config) {
		Endpoint accessTokenEndpoint = buildAccessTokenEndpointRepresentation(authToken, config);
		log.debug("Fetching access token from endpoint: '{}', with body: '{}'", accessTokenEndpoint.url(),
				accessTokenEndpoint.body());

		return http.requestJson(accessTokenEndpoint).thenApply(accessTokenResponse -> {
			if (HttpStatus.OK == accessTokenResponse.status()) {
				PlcNextAuthAndAccessTokenDTO extendedAccessToken = new PlcNextAuthAndAccessTokenDTO(authToken.getCode(),
						authToken.getExpiresIn());
				JsonObject responseBody = accessTokenResponse.data().getAsJsonObject();

				extendedAccessToken.setAccessToken(responseBody.getAsJsonPrimitive("access_token").getAsString());

				return extendedAccessToken;
			} else {
				log.error("Access token endpoint responds with status: '{}' and body: '{}'",
						accessTokenResponse.status(), accessTokenResponse.data());

				return new PlcNextAuthAndAccessTokenDTO("", -1);
			}
		});
	}
}
