package io.openems.backend.oauthregistry;

import java.util.concurrent.CompletableFuture;

import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.OAuthRegistryGetInitMetadataRequest;
import io.openems.common.jsonrpc.request.OAuthRegistryGetTokenByCodeRequest;
import io.openems.common.jsonrpc.request.OAuthRegistryGetTokenByRefreshTokenRequest;
import io.openems.common.jsonrpc.request.OAuthRegistryRequest;
import io.openems.common.jsonrpc.response.OAuthRegistryGetInitMetadataResponse;
import io.openems.common.jsonrpc.response.OAuthRegistryTokenResponse;

public class OAuthRegistryRequestHandler {

	/**
	 * Handles an incoming JSON-RPC request.
	 * 
	 * @param oAuthBackend the {@link OAuthRegistry}
	 * @param request      the incoming request
	 * @return a future with the response
	 */
	public static CompletableFuture<JsonrpcResponseSuccess> handleRequest(//
			final OAuthRegistry oAuthBackend, //
			final OAuthRegistryRequest request //
	) {
		final var payload = request.getPayload();

		return switch (payload.getMethod()) {
		case OAuthRegistryGetInitMetadataRequest.METHOD -> {
			final var r = OAuthRegistryGetInitMetadataRequest.from(payload);
			yield oAuthBackend.getInitMetadata(r.getMetadata().identifier()) //
					.thenApply(oAuthInitMetadata -> {
						return new OAuthRegistryGetInitMetadataResponse(request.id,
								new OAuthRegistryGetInitMetadataResponse.OAuthInitMetadata(
										oAuthInitMetadata.authenticationUrl(), oAuthInitMetadata.clientId(),
										oAuthInitMetadata.redirectUrl()));
					});
		}
		case OAuthRegistryGetTokenByCodeRequest.METHOD -> {
			final var r = OAuthRegistryGetTokenByCodeRequest.from(payload);
			yield oAuthBackend.fetchTokensFromCode(r.getMetadata().identifier(), r.getMetadata().code(),
					r.getMetadata().scopes(), r.getMetadata().codeVerifier()).thenApply(oAuthTokens -> {
						return new OAuthRegistryTokenResponse(request.id, new OAuthRegistryTokenResponse.OAuthToken(
								oAuthTokens.accessToken(), oAuthTokens.refreshToken()));
					});
		}
		case OAuthRegistryGetTokenByRefreshTokenRequest.METHOD -> {
			final var r = OAuthRegistryGetTokenByRefreshTokenRequest.from(payload);
			yield oAuthBackend.fetchTokensFromRefreshToken(r.getMetadata().identifier(), r.getMetadata().refreshToken(),
					r.getMetadata().scopes()).thenApply(oAuthTokens -> {
						return new OAuthRegistryTokenResponse(request.id, new OAuthRegistryTokenResponse.OAuthToken(
								oAuthTokens.accessToken(), oAuthTokens.refreshToken()));
					});
		}
		default -> null;
		};
	}

	private OAuthRegistryRequestHandler() {
	}
}
