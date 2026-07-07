package io.openems.backend.oauthregistry;

import static java.util.stream.Collectors.joining;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.google.gson.JsonElement;

import io.openems.backend.common.debugcycle.DebugLoggable;
import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttpFactory;
import io.openems.common.bridge.http.oauth.HttpBridgeOAuthService;
import io.openems.common.bridge.http.oauth.HttpBridgeOAuthServiceDefinition;
import io.openems.common.bridge.http.oauth.model.GetTokens;

@Component
public class OAuthRegistryImpl implements OAuthRegistry, DebugLoggable {

	private final Map<String, OAuthClient> oAuthClients = new ConcurrentHashMap<>();

	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
	private void bindOAuthClient(OAuthClient oAuthClient) {
		this.oAuthClients.put(oAuthClient.identifier(), oAuthClient);
	}

	@SuppressWarnings("unused")
	private void unbindOAuthClient(OAuthClient oAuthClient) {
		this.oAuthClients.remove(oAuthClient.identifier());
	}

	@Reference
	private BridgeHttpFactory bridgeHttpFactory;
	private BridgeHttp bridgeHttp;
	private HttpBridgeOAuthService oAuthService;

	private final Map<String, AtomicInteger> metrics = new ConcurrentHashMap<>();

	@Activate
	private void activate() {
		this.bridgeHttp = this.bridgeHttpFactory.get();
		this.oAuthService = this.bridgeHttp.createService(HttpBridgeOAuthServiceDefinition.INSTANCE);
	}

	@Deactivate
	private void deactivate() {
		this.bridgeHttpFactory.unget(this.bridgeHttp);
		this.bridgeHttp = null;
	}

	@Override
	public CompletableFuture<OAuthInitMetadata> getInitMetadata(String identifier) {
		this.incrementMetric("getInitMetadata");
		try {
			final var oauthClient = this.getClientSecretsOrThrow(identifier);

			return CompletableFuture.completedFuture(new OAuthInitMetadata(oauthClient.authenticationUrl(),
					oauthClient.clientId(), oauthClient.redirectUri()));
		} catch (Exception e) {
			return CompletableFuture.failedFuture(e);
		}
	}

	@Override
	public CompletableFuture<OAuthTokens> fetchTokensFromRefreshToken(String identifier, String refreshToken,
			List<String> scopes) {
		this.incrementMetric("fetchTokensFromRefreshToken");
		return this.fetchTokensHttpBridge(identifier, scopes, new GetTokens.Grant.RefreshTokenGrant(refreshToken));
	}

	@Override
	public CompletableFuture<OAuthTokens> fetchTokensFromCode(String identifier, String code, List<String> scopes,
			String codeVerifier) {
		this.incrementMetric("fetchTokensFromCode");
		return this.fetchTokensHttpBridge(identifier, scopes,
				new GetTokens.Grant.AuthorizationCodeGrant(code, codeVerifier));
	}

	@Override
	public String debugLog() {
		return "[OAuthRegistry] Sum-Tasks: " + this.metrics.entrySet().stream() //
				.map(t -> t.getKey() + ":" + t.getValue().get()) //
				.collect(joining(", "));
	}

	@Override
	public Map<String, JsonElement> debugMetrics() {
		return Map.of();
	}

	private void incrementMetric(String name) {
		this.metrics.computeIfAbsent(name, k -> new AtomicInteger(0)).incrementAndGet();
	}

	private CompletableFuture<OAuthTokens> fetchTokensHttpBridge(String identifier, List<String> scopes,
			GetTokens.Grant grant) {
		final var oauthClient = this.getClientSecretsOrThrow(identifier);

		return this.oAuthService.requestTokens(new GetTokens.Request(new GetTokens.OAuthClient(oauthClient.clientId(),
				oauthClient.clientSecret(), oauthClient.redirectUri(), oauthClient.codeToTokenUrl()), scopes, grant))
				.thenApply(response -> {
					return new OAuthTokens(response.accessToken(), response.refreshToken());
				});
	}

	private OAuthClient getClientSecretsOrThrow(String identifier) {
		final var oAuthClient = this.oAuthClients.get(identifier);
		if (oAuthClient == null) {
			throw new RuntimeException("Unable to find OAuth client with identifier '" + identifier + "'");
		}
		return oAuthClient;
	}

}
