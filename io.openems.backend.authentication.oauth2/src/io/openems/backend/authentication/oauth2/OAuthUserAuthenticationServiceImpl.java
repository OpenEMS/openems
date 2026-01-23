package io.openems.backend.authentication.oauth2;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProviderBuilder;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.auth0.jwt.interfaces.RSAKeyProvider;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import io.openems.backend.authentication.api.AuthUserAuthorizationCodeFlowService;
import io.openems.backend.authentication.api.AuthUserPasswordAuthenticationService;
import io.openems.backend.authentication.api.AuthUserRegistrationService;
import io.openems.backend.authentication.api.model.OAuthToken;
import io.openems.backend.authentication.api.model.PasswordAuthenticationResult;
import io.openems.backend.authentication.api.model.request.RegisterUserRequest;
import io.openems.backend.authentication.api.model.response.InitiateConnectResponse;
import io.openems.backend.common.debugcycle.DebugLoggable;
import io.openems.backend.common.metadata.Metadata;
import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttpFactory;
import io.openems.common.bridge.http.api.HttpError;
import io.openems.common.bridge.http.api.UrlBuilder;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.HttpStatus;
import io.openems.common.utils.ThreadPoolUtils;

@Designate(ocd = OAuthUserAuthenticationServiceConfig.class)
@Component(//
		name = "Authentication.OAuth", //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		immediate = true //
)
public class OAuthUserAuthenticationServiceImpl implements AuthUserRegistrationService,
		AuthUserAuthorizationCodeFlowService, AuthUserPasswordAuthenticationService, DebugLoggable {

	private static final String ID = "auth0";
	private static final String SCOPES = "openid";

	private final Logger log = LoggerFactory.getLogger(OAuthUserAuthenticationServiceImpl.class);

	private final URI baseKeycloakUrl;
	private final String realm;
	private final URI issuerUrl;
	private final URI loginUrl;
	private final URI tokenUrl;
	private final URI certsUrl;

	private final JWTVerifier verifier;

	private record ConnectState(String codeVerifier, String redirectUri, String oem) {

	}

	private final Map<String, ConnectState> codeVerifiers = new ConcurrentHashMap<>();

	private final BridgeHttpFactory bridgeHttpFactory;
	private final BridgeHttp bridgeHttp;

	private final Metadata metadata;

	private final Map<String, OAuthOemConfig> oemsConfigs = new ConcurrentHashMap<>();

	private final Executor executor = Executors.newScheduledThreadPool(100, Thread.ofVirtual().name("Auth").factory());

	@Reference(//
			cardinality = ReferenceCardinality.MULTIPLE, //
			policy = ReferencePolicy.DYNAMIC, //
			policyOption = ReferencePolicyOption.GREEDY //
	)
	private void bindOAuthOemConfig(OAuthOemService service) {
		final var config = service.getConfig();
		final var prevValue = this.oemsConfigs.put(config.oem(), config);

		if (prevValue != null) {
			this.log.warn("Overriding existing OAuth OEM config for OEM: {}", config.oem());
		}
	}

	@SuppressWarnings("unused")
	private void unbindOAuthOemConfig(OAuthOemService service) {
		final var config = service.getConfig();
		if (this.oemsConfigs.remove(config.oem()) == null) {
			this.log.warn("No existing OAuth OEM config found to unbind for OEM: {}", config.oem());
		}
	}

	@Activate
	public OAuthUserAuthenticationServiceImpl(//
			OAuthUserAuthenticationServiceConfig config, //
			@Reference BridgeHttpFactory bridgeHttpFactory, //
			@Reference Metadata metadata //
	) throws Exception {
		this.bridgeHttpFactory = bridgeHttpFactory;
		this.bridgeHttp = this.bridgeHttpFactory.get();
		this.bridgeHttp.setMaximumPoolSize(config.maxConcurrentRequests());
		this.bridgeHttp.setDebugMode(config.debugMode());

		this.metadata = metadata;

		this.realm = config.realm();
		this.baseKeycloakUrl = URI.create(config.baseKeycloakUrl());
		this.issuerUrl = URI.create(config.issuerUrl());
		this.loginUrl = URI.create(config.loginUrl());
		this.tokenUrl = URI.create(config.tokenUrl());
		this.certsUrl = URI.create(config.certsUrl());

		final var provider = new JwkProviderBuilder(this.certsUrl.toURL()) //
				.rateLimited(config.rateLimitedBucketSize(), config.rateLimitedRefillRate(), TimeUnit.SECONDS) //
				.build();

		final var keyProvider = new RSAKeyProvider() {
			@Override
			public RSAPublicKey getPublicKeyById(String kid) {
				try {
					return (RSAPublicKey) provider.get(kid).getPublicKey();
				} catch (JwkException e) {
					throw new RuntimeException(e);
				}
			}

			@Override
			public RSAPrivateKey getPrivateKey() {
				return null;
			}

			@Override
			public String getPrivateKeyId() {
				return null;
			}
		};

		this.verifier = JWT.require(Algorithm.RSA256(keyProvider)) //
				.withIssuer(this.issuerUrl.toString()) //
				.build();
	}

	/**
	 * Called by OSGi to deactivate the component.
	 */
	@Deactivate
	public void deactivate() {
		this.bridgeHttpFactory.unget(this.bridgeHttp);
	}

	@Override
	public CompletableFuture<PasswordAuthenticationResult> authenticateWithPassword(String username, String password) {
		final var config = this.getServiceAccountOemConfig();
		return KeycloakApi.getToken(this.bridgeHttp, this.issuerUrl.toString(), config.clientId(),
				config.clientSecret(), username, password).thenApply(token -> {
					final var jwtToken = JWT.decode(token);
					final var email = jwtToken.getClaim("email").asString();

					// email is currently treated as login (originally from odoo)
					return new PasswordAuthenticationResult(jwtToken.getSubject(), email, token);
				});
	}

	@Override
	public CompletableFuture<PasswordAuthenticationResult> authenticateWithToken(String token) {
		return this.validate(token).thenApply(ignore -> {
			final var jwtToken = JWT.decode(token);
			final var email = jwtToken.getClaim("email").asString();

			// email is currently treated as login (originally from odoo)
			return new PasswordAuthenticationResult(jwtToken.getSubject(), email, token);
		});
	}

	@Override
	public CompletableFuture<Void> logout(String token) {
		// empty: Keycloak does not support access token revocation
		// (refresh tokens can be revoked but are not used here)
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public CompletableFuture<InitiateConnectResponse> initiateConnect(String oem, String redirectUri) {
		return CompletableFuture.supplyAsync(() -> {

			final var oemConfig = this.getOemConfig(oem);

			final var codeVerifier = generateCodeVerifier();
			final var codeChallenge = generateCodeChallenge(codeVerifier);

			final var identifier = UUID.randomUUID().toString();

			final var rUri = redirectUri != null ? redirectUri : oemConfig.redirectUri();

			this.codeVerifiers.put(identifier, new ConnectState(codeVerifier, rUri, oem));

			this.log.info("Initiate Connect oem={}, redirectUri={}, identifier={}", oem, rUri, identifier);

			final var url = UrlBuilder.parse(this.loginUrl.toString()) //
					.withQueryParam("client_id", oemConfig.clientId()) //
					.withQueryParam("state", identifier) //
					.withQueryParam("code_challenge", codeChallenge) //
					.withQueryParam("code_challenge_method", "S256") //
					.withQueryParam("scope", OAuthUserAuthenticationServiceImpl.SCOPES) //
					.withQueryParam("redirect_uri", rUri) //
					.withQueryParam("response_type", "code") //
					.toEncodedString();

			return new InitiateConnectResponse(identifier, identifier, url);
		}, this.executor).exceptionally(t -> {
			this.log.warn("Unable to initiate connection with oem={}, redirectUri={}", oem, redirectUri);
			throw new RuntimeException("Unable to initiate connection", t);
		});
	}

	@Override
	public CompletableFuture<OAuthToken> tokenByCode(String oem, String identifier, String codeRaw) {
		return CompletableFuture.supplyAsync(() -> {
			final var verifier = this.codeVerifiers.remove(identifier);

			this.log.info("Token by code oem={}, identifier={}, code={}, verifier={}", oem, identifier, codeRaw,
					verifier);
			if (verifier == null) {
				throw new RuntimeException("No code verifier found for the given code. " + identifier);
			}
			return verifier;
		}, this.executor).thenCompose(state -> {
			return this.fetchTokensHttpBridge(this.getOemConfig(oem), state.redirectUri(),
					new Grant.AuthorizationCodeGrant(codeRaw, state.codeVerifier));
		}).handle((oAuthToken, throwable) -> {
			if (throwable != null) {
				this.log.error("Error fetching tokens for identifier: {}", identifier, throwable);
				throw new CompletionException(OpenemsError.COMMON_AUTHENTICATION_FAILED.exception());
			}
			return oAuthToken;
		});
	}

	@Override
	public CompletableFuture<OAuthToken> tokenByRefreshToken(String oem, String refreshToken) {
		final var oemConfig = this.getOemConfig(oem);
		return this.fetchTokensHttpBridge(oemConfig, oemConfig.redirectUri(), new Grant.RefreshTokenGrant(refreshToken)) //
				.handle((oAuthToken, throwable) -> {
					if (throwable != null) {
						this.log.error("Error fetching tokens with refresh token: {}", refreshToken, throwable);
						throw new CompletionException(OpenemsError.COMMON_AUTHENTICATION_FAILED.exception());
					}
					return oAuthToken;
				});
	}

	@Override
	public CompletableFuture<Void> registerUser(RegisterUserRequest user) {
		return CompletableFuture.supplyAsync(this::getServiceAccountOemConfig, this.executor).thenCompose(oemConfig -> {

			return KeycloakApi
					.getToken(this.bridgeHttp, this.issuerUrl.toString(), oemConfig.clientId(),
							oemConfig.clientSecret())
					.thenCompose(token -> KeycloakApi
							.createUser(this.bridgeHttp, this.baseKeycloakUrl.toString(), this.realm, token,
									user.email(), user.email(), user.firstname(), user.lastname(), true) //
							.thenCompose(userId -> {
								final var futures = new ArrayList<CompletableFuture<?>>();

								futures.add(CompletableFuture.runAsync(() -> {
									try {
										final var request = RegisterUserRequest.serializer().serialize(user)
												.getAsJsonObject();

										// TODO should not be hardcoded
										request.addProperty("oauthProviderId", -1);
										request.addProperty("oauthUid", userId);
										this.metadata.registerUser(request, oemConfig.oem());
									} catch (OpenemsError.OpenemsNamedException e) {
										throw new RuntimeException(e);
									}
								}, this.executor));

								if (user.password() != null) {
									futures.add(
											KeycloakApi.resetPassword(this.bridgeHttp, this.baseKeycloakUrl.toString(),
													this.realm, token, userId, false, user.password()));
								}

								return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
							})) //
					.whenComplete((unused, throwable) -> {
						if (throwable != null) {
							this.log.error("Error registering user: {}", user, throwable);
						}
					});
		});
	}

	@Override
	public CompletableFuture<Void> registerUserIfNotExist(RegisterUserRequest user) {
		return this.registerUser(user).exceptionallyCompose(throwable -> {
			if (throwable instanceof CompletionException completionException //
					&& completionException.getCause() instanceof HttpError.ResponseError responseError //
					&& responseError.status.code() == HttpStatus.CONFLICT.code()) {
				return CompletableFuture.completedFuture(null);
			}

			return CompletableFuture.failedFuture(throwable);
		});
	}

	private CompletableFuture<OAuthToken> fetchTokensHttpBridge(OAuthOemConfig config, String redirectUri,
			Grant grant) {
		return this.bridgeHttp.requestJson(BridgeHttp.Endpoint.create(this.tokenUrl.toString()) //
				.setHeader("Accept", "application/json") //
				.setBodyFormEncoded(this.getQueryParams(config, redirectUri, grant)) //
				.build()) //
				.thenApply(response -> {
					final var obj = response.data().getAsJsonObject();

					final var accessToken = obj.get("access_token").getAsString();
					final var refreshToken = obj.get("refresh_token").getAsString();

					final var decodedToken = this.processAccessToken(accessToken);
					final var sub = decodedToken.getClaim("sub").asString();
					final var email = decodedToken.getClaim("email").asString();

					return new OAuthToken(sub, email, accessToken, refreshToken);
				});

	}

	private HashMap<String, String> getQueryParams(OAuthOemConfig config, String redirectUri, Grant grant) {
		final var queryParams = new HashMap<String, String>();
		queryParams.put("client_id", config.clientId());
		queryParams.put("client_secret", config.clientSecret());
		queryParams.put("redirect_uri", redirectUri);
		queryParams.put("scope", OAuthUserAuthenticationServiceImpl.SCOPES);
		switch (grant) {
		case Grant.AuthorizationCodeGrant acg -> {
			queryParams.put("grant_type", "authorization_code");
			queryParams.put("code", acg.code());
			if (acg.codeVerifier() != null) {
				queryParams.put("code_verifier", acg.codeVerifier());
			}
		}
		case Grant.RefreshTokenGrant rtg -> {
			queryParams.put("grant_type", "refresh_token");
			queryParams.put("refresh_token", rtg.refreshToken());
		}
		}
		return queryParams;
	}

	private sealed interface Grant {
		public record AuthorizationCodeGrant(String code, String codeVerifier) implements Grant {
		}

		public record RefreshTokenGrant(String refreshToken) implements Grant {
		}
	}

	@Override
	public CompletableFuture<Void> validate(String accessToken) {
		return CompletableFuture.runAsync(() -> {
			this.processAccessToken(accessToken);
		}, this.executor).handle((ignore, throwable) -> {
			if (throwable != null) {
				this.log.error("Error validating access token: {}", accessToken, throwable);
				throw new CompletionException(OpenemsError.COMMON_AUTHENTICATION_FAILED.exception());
			}
			return ignore;
		});
	}

	@Override
	public String debugLog() {
		return "[" + ID + "] [monitor] " + ThreadPoolUtils.debugLog(this.executor) + ", HttpBridge="
				+ this.bridgeHttp.getMetrics().entrySet().stream().map(t -> t.getKey() + "=" + t.getValue())
						.collect(Collectors.joining(", "));
	}

	@Override
	public Map<String, JsonElement> debugMetrics() {
		final var metrics = new HashMap<String, JsonElement>();
		for (var entry : ThreadPoolUtils.debugMetrics(this.executor).entrySet()) {
			metrics.put(String.format("%s/%s", ID, entry.getKey()), new JsonPrimitive(entry.getValue()));
		}
		for (var entry : this.bridgeHttp.getMetrics().entrySet()) {
			metrics.put(String.format("%s_httpBridge/%s", ID, entry.getKey()), new JsonPrimitive(entry.getValue()));
		}
		return metrics;
	}

	private OAuthOemConfig getOemConfig(String oem) {
		if (oem == null || oem.isBlank()) {
			throw new RuntimeException("No OEM provided");
		}
		final var config = this.oemsConfigs.get(oem);
		if (config == null) {
			this.log.warn("No redirect OEM config found for {}", oem);
			throw new RuntimeException("No OEM config found for " + oem);
		}
		return config;
	}

	private OAuthOemConfig getServiceAccountOemConfig() {
		for (var oemConfig : this.oemsConfigs.values()) {
			if (oemConfig.serviceAccount()) {
				return oemConfig;
			}
		}
		throw new RuntimeException("No OEM config found for service account");
	}

	private DecodedJWT processAccessToken(String accessToken) {
		try {
			return this.verifier.verify(accessToken);
		} catch (JWTCreationException exception) {
			// Invalid Signing configuration / Couldn't convert Claims.
			this.log.info("JWT failed", exception);
			throw new RuntimeException("Invalid JWT token", exception);
		}
	}

	private static String generateCodeVerifier() {
		final var secureRandom = new SecureRandom();
		byte[] codeVerifier = new byte[32];
		secureRandom.nextBytes(codeVerifier);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(codeVerifier);
	}

	private static String generateCodeChallenge(String codeVerifier) {
		try {
			byte[] bytes = codeVerifier.getBytes(StandardCharsets.US_ASCII);
			MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
			messageDigest.update(bytes, 0, bytes.length);
			byte[] digest = messageDigest.digest();
			return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

}
