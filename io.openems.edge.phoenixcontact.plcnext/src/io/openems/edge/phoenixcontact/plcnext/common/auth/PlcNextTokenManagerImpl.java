package io.openems.edge.phoenixcontact.plcnext.common.auth;

import static org.osgi.service.component.annotations.ReferenceScope.PROTOTYPE_REQUIRED;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
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
	private ZonedDateTime tokenExpiry;

	@Activate
	public PlcNextTokenManagerImpl(@Reference(scope = PROTOTYPE_REQUIRED) BridgeHttp http) {
		this.http = http;
	}

	@Override
	public synchronized String getToken() {
		return this.token;
	}

	@Override
	public synchronized CompletableFuture<Void> fetchToken(PlcNextAuthConfig authClientConfig) {
		if (!this.hasValidToken()) {
			log.info("Start fetching authentication");
			var authTokenFuture = this.fetchAuthToken(authClientConfig);
			if (Objects.isNull(authTokenFuture) || authTokenFuture.isCompletedExceptionally()) {
				log.error("Fetching auth token failed! Cannot continue fetching the access token!");
				this.resetTokenAndExpiery();
				return CompletableFuture.failedFuture(new NullPointerException());
			}

			var accessTokenFuture = authTokenFuture //
					.thenCompose(code -> this.fetchAccessToken(code, authClientConfig));
			if (accessTokenFuture.isCompletedExceptionally()) {
				log.error("Fetching access token failed! Cannot continue processing response.");
				this.resetTokenAndExpiery();
				return CompletableFuture.failedFuture(new NullPointerException());
			}

			return accessTokenFuture.thenApply(combinedToken -> {
				if (Objects.nonNull(combinedToken) && Objects.nonNull(combinedToken.getAccessToken())) {
					log.debug("Fetching access token has been successful.");
					this.token = combinedToken.getAccessToken();
					this.tokenExpiry = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS)
							.plusSeconds(combinedToken.getExpiresIn());
				} else if (Objects.isNull(combinedToken)) {
					log.error("No token information returned!");
					this.resetTokenAndExpiery();
				} else {
					log.error("No access token or expiery information returned!");
					this.resetTokenAndExpiery();
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
		this.token = null;
		this.tokenExpiry = null;
	}

	@Override
	public synchronized boolean hasValidToken() {
		return Objects.nonNull(this.token) //
				&& Objects.nonNull(this.tokenExpiry) //
				&& !this.tokenExpiry.isBefore(ZonedDateTime.now(ZoneId.systemDefault()));
	}

	/**
	 * Creates endpoint configuration to fetch an auth token from REST-API.
	 *
	 * @param config represents the authentication configuration
	 * @return configured endpoint to be called
	 */
	Endpoint buildAuthTokenEndpointRepresentation(PlcNextAuthConfig config) {
		var requestBody = "{\"scope\":\"variables\" }";
		var headers = Map.of("Content-Type", "application/json");
		var authTokenUrlString = PlcNextUrlStringHelper.buildUrlString(config.authUrl(), PATH_AUTH_TOKEN);

		return new Endpoint(authTokenUrlString, HttpMethod.POST, BridgeHttp.DEFAULT_CONNECT_TIMEOUT,
				BridgeHttp.DEFAULT_READ_TIMEOUT, requestBody, headers);
	}

	/**
	 * Fetches new valid auth token for REST-API, required to fetch an access token.
	 *
	 * @param config represents the authentication configuration
	 * @return @link{CompletableFuture} covering the auth token and timeout
	 */
	CompletableFuture<PlcNextAuthAndAccessTokenDto> fetchAuthToken(PlcNextAuthConfig config) {
		var authTokenEndpoint = this.buildAuthTokenEndpointRepresentation(config);
		this.log.info("Fetching bearer token from endpoint URL: '{}'", authTokenEndpoint.url());

		try {
			return this.http.requestJson(authTokenEndpoint) //
					.thenApply(authTokenResponse -> {

						if (HttpStatus.OK == authTokenResponse.status()) {
							JsonObject responseBody = authTokenResponse.data().getAsJsonObject();

							return new PlcNextAuthAndAccessTokenDto(
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
			this.resetTokenAndExpiery();
			return CompletableFuture.completedFuture(null);
		}
	}

	/**
	 * Creates endpoint configuration to fetch an access token from REST-API.
	 *
	 * @param authAndAccessToken represents the PLCnext credentials object
	 * @param config             represents the authentication configuration
	 * @return configured endpoint to be called
	 */
	Endpoint buildAccessTokenEndpointRepresentation(PlcNextAuthAndAccessTokenDto authAndAccessToken,
			PlcNextAuthConfig config) {
		var requestBody = new StringBuilder("{ \"code\": \"") //
				.append(authAndAccessToken.getCode()).append("\", ") //
				.append("\"grant_type\": \"authorization_code\", ") //
				.append("\"username\": \"").append(config.username()).append("\", ") //
				.append("\"password\": \"").append(config.password()).append("\" }") //
				.toString();
		var headers = Map.of(//
				"Content-Type", "application/json");
		var accessTokenUrlString = PlcNextUrlStringHelper.buildUrlString(config.authUrl(), PATH_ACCESS_TOKEN);

		return new Endpoint(accessTokenUrlString, HttpMethod.POST, BridgeHttp.DEFAULT_CONNECT_TIMEOUT,
				BridgeHttp.DEFAULT_READ_TIMEOUT, requestBody, headers);
	}

	/**
	 * Fetches new valid access token for REST-API.
	 *
	 * @param authAndAccessToken represents the PLCnext credentials object
	 * @param config             represents the authentication configuration
	 * @return @link{CompletableFuture} covering the authorization data
	 */
	CompletableFuture<PlcNextAuthAndAccessTokenDto> fetchAccessToken(PlcNextAuthAndAccessTokenDto authAndAccessToken,
			PlcNextAuthConfig config) {

		if (Objects.isNull(authAndAccessToken)) {
			log.error("Cannot fetch access token while auth token is not available! Skipping to fetch access token.");
			this.resetTokenAndExpiery();
			return CompletableFuture.completedFuture(null);
		}

		var accessTokenEndpoint = this.buildAccessTokenEndpointRepresentation(authAndAccessToken, config);
		log.info("Fetching access token from endpoint URL: '{}'", accessTokenEndpoint.url());

		try {
			return this.http.requestJson(accessTokenEndpoint).thenApply(accessTokenResponse -> {
				if (HttpStatus.OK == accessTokenResponse.status()) {
					var extendedAccessToken = new PlcNextAuthAndAccessTokenDto(//
							authAndAccessToken.getCode(), //
							authAndAccessToken.getExpiresIn());
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
			this.resetTokenAndExpiery();
			return CompletableFuture.completedFuture(null);
		}
	}
}
