package io.openems.common.bridge.http.oauth;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.HttpBridgeService;
import io.openems.common.bridge.http.oauth.model.GetTokens;

public class HttpBridgeOAuthService implements HttpBridgeService {

	private final BridgeHttp bridgeHttp;

	public HttpBridgeOAuthService(BridgeHttp bridgeHttp) {
		this.bridgeHttp = bridgeHttp;
	}

	/**
	 * Requests OAuth tokens using the provided {@link GetTokens.Request}.
	 * 
	 * @param request the {@link GetTokens.Request}
	 * @return a {@link CompletableFuture} of {@link GetTokens.Response}
	 */
	public CompletableFuture<GetTokens.Response> requestTokens(GetTokens.Request request) {
		final var oAuthClient = request.oAuthClient();
		final var grant = request.grant();

		final var params = new HashMap<String, String>();
		params.put("client_id", oAuthClient.clientId());
		params.put("client_secret", oAuthClient.clientSecret());
		params.put("redirect_uri", oAuthClient.redirectUri());
		params.put("scope", String.join(" ", request.scopes()));

		switch (grant) {
		case GetTokens.Grant.AuthorizationCodeGrant acg -> {
			params.put("grant_type", "authorization_code");
			params.put("code", acg.code());
			if (acg.codeVerifier() != null) {
				params.put("code_verifier", acg.codeVerifier());
			}
		}
		case GetTokens.Grant.RefreshTokenGrant rtg -> {
			params.put("grant_type", "refresh_token");
			params.put("refresh_token", rtg.refreshToken());
		}
		}

		return this.bridgeHttp.requestJson(BridgeHttp.create(oAuthClient.codeToTokenUrl()) //
				.setBodyFormEncoded(params) //
				.setHeader("Accept", "application/json") //
				.build()).thenApply(httpResponse -> {
					return GetTokens.Response.serializer().deserialize(httpResponse.data());
				});
	}

	@Override
	public void close() throws Exception {

	}
}
