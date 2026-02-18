package io.openems.backend.authentication.oauth2;

import java.net.MalformedURLException;
import java.net.URL;
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
import com.auth0.jwk.JwkProvider;
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
@Component(
		name = "Authentication.OAuth",
		configurationPolicy = ConfigurationPolicy.REQUIRE,
		immediate = true
)
public class OAuthUserAuthenticationServiceImpl implements
		AuthUserRegistrationService,
		AuthUserAuthorizationCodeFlowService,
		AuthUserPasswordAuthenticationService,
		DebugLoggable {

	private static final String ID = "auth0";
	private static final String SCOPES = "openid";

	private final Logger log = LoggerFactory.getLogger(OAuthUserAuthenticationServiceImpl.class);

	private final String issuerUrl;
	private final String realm;
	private final String scopes = SCOPES;

	private final int rateLimitedBucketSize;
	private final int rateLimitedRefillRate;

	private volatile JWTVerifier verifier;
	private volatile JwkProvider jwkProvider;
	private final Object verifierLock = new Object();

	private record ConnectState(String codeVerifier, String redirectUri, String oem) {
	}

	private final Map<String, ConnectState> codeVerifiers = new ConcurrentHashMap<>();

	private final BridgeHttpFactory bridgeHttpFactory;
	private final BridgeHttp bridgeHttp;
	private final Metadata metadata;

	private final Map<String, OAuthOemConfig> oemsConfigs = new ConcurrentHashMap<>();

	private volatile OidcDiscovery oidcDiscovery;
	private volatile OidcClient oidcClient;

	private final Executor executor =
			Executors.newScheduledThreadPool(100, Thread.ofVirtual().name("Auth").factory());

	@Reference(
			cardinality = ReferenceCardinality.MULTIPLE,
			policy = ReferencePolicy.DYNAMIC,
			policyOption = ReferencePolicyOption.GREEDY
	)
	private void bindOAuthOemConfig(OAuthOemService service) {
		final var config = service.getConfig();
		final var prev = this.oemsConfigs.put(config.oem(), config);
		if (prev != null) {
			this.log.warn("Overriding existing OAuth OEM config for OEM: {}", config.oem());
		}
	}

	@Activate
	public OAuthUserAuthenticationServiceImpl(
			OAuthUserAuthenticationServiceConfig config,
			@Reference BridgeHttpFactory bridgeHttpFactory,
			@Reference Metadata metadata
	) {
		this.bridgeHttpFactory = bridgeHttpFactory;
		this.bridgeHttp = bridgeHttpFactory.get();
		this.bridgeHttp.setMaximumPoolSize(config.maxConcurrentRequests());
		this.bridgeHttp.setDebugMode(config.debugMode());

		this.metadata = metadata;
		this.realm = config.realm();
		this.issuerUrl = normalizeIssuerUrl(config.issuerUrl());
		this.rateLimitedBucketSize = config.rateLimitedBucketSize();
		this.rateLimitedRefillRate = config.rateLimitedRefillRate();

		this.log.info("OAuth Authentication Service initialized with issuer: {}", this.issuerUrl);
	}

	private static String normalizeIssuerUrl(String issuerUrl) {
		if (issuerUrl == null) {
			return "";
		}
		return issuerUrl.endsWith("/") ? issuerUrl.substring(0, issuerUrl.length() - 1) : issuerUrl;
	}

	@Deactivate
	public void deactivate() {
		this.bridgeHttpFactory.unget(this.bridgeHttp);
	}

	private void initializeJwtVerifier(OidcDiscovery discovery) {
		synchronized (this.verifierLock) {
			if (this.verifier != null) {
				return;
			}
			try {
				final var jwksUri = new URL(discovery.getJwksUri());
				this.jwkProvider = new JwkProviderBuilder(jwksUri)
						.rateLimited(this.rateLimitedBucketSize, this.rateLimitedRefillRate, TimeUnit.SECONDS)
						.build();

				final var keyProvider = new RSAKeyProvider() {
					@Override
					public RSAPublicKey getPublicKeyById(String kid) {
						try {
							return (RSAPublicKey) jwkProvider.get(kid).getPublicKey();
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

				this.verifier = JWT.require(Algorithm.RSA256(keyProvider))
						.withIssuer(discovery.getIssuer())
						.build();

				this.log.info("JWT verifier initialized with jwks_uri: {}", jwksUri);
			} catch (MalformedURLException e) {
				throw new RuntimeException("Invalid JWKS URI", e);
			}
		}
	}

	private static String generateCodeVerifier() {
		byte[] bytes = new byte[32];
		new SecureRandom().nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private static String generateCodeChallenge(String verifier) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] digest = md.digest(verifier.getBytes(StandardCharsets.US_ASCII));
			return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}
}
