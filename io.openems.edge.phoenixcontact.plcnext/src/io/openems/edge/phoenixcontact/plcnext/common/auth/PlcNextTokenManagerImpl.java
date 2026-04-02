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

	@Override
	public synchronized String getToken() {
		return this.token;
	}

	@Override
	public synchronized CompletableFuture<Void> fetchToken(PlcNextAuthConfig authClientConfig) {
		if (!hasValidToken()) {
			log.info("Start fetching authentication");
			CompletableFuture<PlcNextAuthAndAccessTokenDTO> authTokenFuture = fetchAuthToken(authClientConfig);
			if (Objects.isNull(authTokenFuture) || authTokenFuture.isCompletedExceptionally()) {
				log.error("Fetching auth token failed! Cannot continue fetching the access token!");
				resetTokenAndExpiery();
				return CompletableFuture.failedFuture(new NullPointerException());
			}

			CompletableFuture<PlcNextAuthAndAccessTokenDTO> accessTokenFuture = authTokenFuture
					.thenCompose(code -> fetchAccessToken(code, authClientConfig));
			if (Objects.isNull(accessTokenFuture) || accessTokenFuture.isCompletedExceptionally()) {
				log.error("Fetching access token failed! Cannot continue processing response.");
				resetTokenAndExpiery();
				return CompletableFuture.failedFuture(new NullPointerException());
			}

			return accessTokenFuture.thenApply(combinedToken -> {
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

	@Override
	public synchronized boolean hasValidToken() {
		return Objects.nonNull(this.token) && //
				Objects.nonNull(tokenExpiery) && !tokenExpiery.isBefore(ZonedDateTime.now());
	}

	/**
	 * Creates endpoint configuration to fetch an auth token from REST-API
	 * 
	 * @param config represents the authentication configuration
	 * @return configured endpoint to be called
	 */
	Endpoint buildAuthTokenEndpointRepresentation(PlcNextAuthConfig config) {
		String requestBody = "{\"scope\":\"variables\" }";
		Map<String, String> headers = Map.of(//
				"Content-Type", "application/json");
		String authTokenUrlString = PlcNextUrlStringHelper.buildUrlString(config.authUrl(), PATH_AUTH_TOKEN);

		return new Endpoint(authTokenUrlString, HttpMethod.POST, BridgeHttp.DEFAULT_CONNECT_TIMEOUT,
				BridgeHttp.DEFAULT_READ_TIMEOUT, requestBody, headers);
	}

	/**
	 * Fetches new valid auth token for REST-API, required to fetch an access token
	 * 
	 * @param config represents the authentication configuration
	 * @return @link{CompletableFuture} covering the auth token and timeout
	 */
	CompletableFuture<PlcNextAuthAndAccessTokenDTO> fetchAuthToken(PlcNextAuthConfig config) {
		Endpoint authTokenEndpoint = buildAuthTokenEndpointRepresentation(config);
		log.info("Fetching bearer token from endpoint URL: '{}'", authTokenEndpoint.url());

		try {
			return http.requestJson(authTokenEndpoint) //
					.thenApply(authTokenResponse -> {

						if (HttpStatus.OK == authTokenResponse.status()) {
							JsonObject responseBody = authTokenResponse.data().getAsJsonObject();

							return new PlcNextAuthAndAccessTokenDTO(
									responseBody.getAsJsonPrimitive("code").getAsString(), //
									responseBody.getAsJsonPrimitive("expires_in").getAsInt());
						} else {
							log.error("Auth token endpoint responds with status: '{}' and body: '{}'",
									authTokenResponse.status(), authTokenResponse.data());

							return null;
						}
					});
		} catch (CompletionException e) {
			log.error("Error while fetching auth token!", e);
			resetTokenAndExpiery();
			return CompletableFuture.completedFuture(null);
		}
	}

	/**
	 * Creates endpoint configuration to fetch an access token from REST-API
	 * 
	 * @param config represents the authentication configuration
	 * @return configured endpoint to be called
	 */
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

	/**
	 * Fetches new valid access token for REST-API
	 * 
	 * @param config represents the authentication configuration
	 * @return @link{CompletableFuture} covering the authorization data
	 */
	CompletableFuture<PlcNextAuthAndAccessTokenDTO> fetchAccessToken(PlcNextAuthAndAccessTokenDTO authToken,
			PlcNextAuthConfig config) {

		if (Objects.isNull(authToken)) {
			log.error("Cannot fetch access token while auth token is not available! Skipping to fetch access token.");
			resetTokenAndExpiery();
			return CompletableFuture.completedFuture(null);
		}

		Endpoint accessTokenEndpoint = buildAccessTokenEndpointRepresentation(authToken, config);
		log.info("Fetching access token from endpoint URL: '{}'", accessTokenEndpoint.url());

		try {
			return http.requestJson(accessTokenEndpoint).thenApply(accessTokenResponse -> {
				if (HttpStatus.OK == accessTokenResponse.status()) {
					PlcNextAuthAndAccessTokenDTO extendedAccessToken = new PlcNextAuthAndAccessTokenDTO(
							authToken.getCode(), //
							authToken.getExpiresIn());
					JsonObject responseBody = accessTokenResponse.data().getAsJsonObject();

					if (responseBody.has("access_token")) {
						extendedAccessToken.setAccessToken(responseBody //
								.getAsJsonPrimitive("access_token") //
								.getAsString());
					}
					return extendedAccessToken;
				} else {
					log.error("Access token endpoint responds with status: '{}' and body: '{}'",
							accessTokenResponse.status(), accessTokenResponse.data());

					return null;
				}
			});
		} catch (CompletionException e) {
			log.error("Error while fetching access token!", e);
			resetTokenAndExpiery();
			return CompletableFuture.completedFuture(null);
		}
	}
}
