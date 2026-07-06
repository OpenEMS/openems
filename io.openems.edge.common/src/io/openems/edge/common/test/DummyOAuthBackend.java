package io.openems.edge.common.test;

import java.util.concurrent.CompletableFuture;

import io.openems.common.jsonrpc.response.OAuthRegistryGetInitMetadataResponse;
import io.openems.common.jsonrpc.response.OAuthRegistryGetInitMetadataResponse.OAuthInitMetadata;
import io.openems.common.jsonrpc.response.OAuthRegistryTokenResponse;
import io.openems.common.jsonrpc.response.OAuthRegistryTokenResponse.OAuthToken;
import io.openems.edge.common.oauth.OAuthBackend;

public class DummyOAuthBackend implements OAuthBackend {

	@Override
	public CompletableFuture<OAuthInitMetadata> getInitMetadata(String identifier) {
		return CompletableFuture.completedFuture(//
				new OAuthRegistryGetInitMetadataResponse.OAuthInitMetadata(//
						"https://dummy-auth.com", //
						"dummy-client-id", //
						"http://localhost:4200" //
				));
	}

	@Override
	public CompletableFuture<OAuthToken> fetchTokensFromRefreshToken(OAuthClientBackendRegistration backendRegistration,
			String refreshToken) {
		return CompletableFuture.completedFuture(//
				new OAuthRegistryTokenResponse.OAuthToken(//
						"dummy_access", //
						"dummy_refresh" //
				));
	}

	@Override
	public CompletableFuture<OAuthToken> fetchTokensFromCode(OAuthClientBackendRegistration backendRegistration,
			String code, String codeVerifier) {
		return CompletableFuture.completedFuture(//
				new OAuthRegistryTokenResponse.OAuthToken(//
						"dummy_access", //
						"dummy_refresh" //
				));
	}

}
