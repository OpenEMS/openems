package io.openems.edge.core.oauth;

import java.util.concurrent.CompletableFuture;

import org.osgi.service.component.annotations.Activate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import io.openems.common.jsonrpc.request.OAuthRegistryGetInitMetadataRequest;
import io.openems.common.jsonrpc.request.OAuthRegistryGetTokenByCodeRequest;
import io.openems.common.jsonrpc.request.OAuthRegistryGetTokenByRefreshTokenRequest;
import io.openems.common.jsonrpc.request.OAuthRegistryRequest;
import io.openems.common.jsonrpc.response.OAuthRegistryGetInitMetadataResponse;
import io.openems.common.jsonrpc.response.OAuthRegistryTokenResponse;
import io.openems.edge.common.oauth.OAuthBackend;
import io.openems.edge.controller.api.backend.api.ControllerApiBackend;

@Component
public class OAuthBackendImpl implements OAuthBackend {

	private final Logger log = LoggerFactory.getLogger(OAuthBackendImpl.class);

	@Reference(//
			policy = ReferencePolicy.DYNAMIC, //
			policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.OPTIONAL //
	)
	private volatile ControllerApiBackend backend;

	@Activate
	private void activate() {
	}

	@Override
	public CompletableFuture<OAuthRegistryGetInitMetadataResponse.OAuthInitMetadata> getInitMetadata(
			String identifier) {
		var backend = this.backend;
		if (backend == null) {
			this.log.warn("Backend API is not available for OAuth operations");
			return CompletableFuture.failedFuture(
					new IllegalStateException("Backend API is not configured or not enabled"));
		}
		return backend
				.sendRequest(null,
						new OAuthRegistryRequest(new OAuthRegistryGetInitMetadataRequest(
								new OAuthRegistryGetInitMetadataRequest.OAuthInitRequest(identifier)))) //
				.thenApply(response -> {
					return OAuthRegistryGetInitMetadataResponse.from(response).getMetadata();
				});
	}

	@Override
	public CompletableFuture<OAuthRegistryTokenResponse.OAuthToken> fetchTokensFromRefreshToken(
			OAuthClientBackendRegistration backendRegistration, String refreshToken) {
		var backend = this.backend;
		if (backend == null) {
			this.log.warn("Backend API is not available for OAuth operations");
			return CompletableFuture.failedFuture(
					new IllegalStateException("Backend API is not configured or not enabled"));
		}
		return backend
				.sendRequest(null,
						new OAuthRegistryRequest(new OAuthRegistryGetTokenByRefreshTokenRequest(
								new OAuthRegistryGetTokenByRefreshTokenRequest.OAuthGetTokenByRefreshTokenRequest(
										backendRegistration.identifier(), refreshToken, backendRegistration.scopes())))) //
				.thenApply(response -> {
					return OAuthRegistryTokenResponse.from(response).getMetadata();
				});
	}

	@Override
	public CompletableFuture<OAuthRegistryTokenResponse.OAuthToken> fetchTokensFromCode(
			OAuthClientBackendRegistration backendRegistration, String code, String codeVerifier) {
		var backend = this.backend;
		if (backend == null) {
			this.log.warn("Backend API is not available for OAuth operations");
			return CompletableFuture.failedFuture(
					new IllegalStateException("Backend API is not configured or not enabled"));
		}
		return backend
				.sendRequest(null, new OAuthRegistryRequest(new OAuthRegistryGetTokenByCodeRequest(
						new OAuthRegistryGetTokenByCodeRequest.OAuthGetTokenByCodeRequest(
								backendRegistration.identifier(), code, backendRegistration.scopes(), codeVerifier)))) //
				.thenApply(response -> {
					return OAuthRegistryTokenResponse.from(response).getMetadata();
				});
	}

}
