package io.openems.backend.uiwebsocket.impl;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonObject;

import io.openems.backend.authentication.api.AuthUserAuthorizationCodeFlowService;
import io.openems.backend.authentication.api.model.response.InitiateConnectResponse;
import io.openems.backend.common.metadata.Metadata;
import io.openems.backend.common.metadata.User;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.AuthenticateWithOAuthRequest;
import io.openems.common.utils.JsonUtils;

public final class OAuthAuthenticationHandler {

	/**
	 * Handles the OAuth authentication requests.
	 * 
	 * @param metadata    the {@link Metadata} to use for user lookup
	 * @param authService the {@link AuthUserAuthorizationCodeFlowService} to use
	 *                    for
	 * @param re          the {@link AuthenticateWithOAuthRequest} containing the
	 *                    request
	 * @param wsData      the {@link WsData} to store the token
	 * @return a {@link CompletableFuture} that completes with the result
	 * @throws OpenemsError.OpenemsNamedException on parse error
	 */
	public static CompletableFuture<JsonrpcResponseSuccess> handleRequest(//
			Metadata metadata, //
			AuthUserAuthorizationCodeFlowService authService, //
			AuthenticateWithOAuthRequest re, //
			WsData wsData //
	) throws OpenemsError.OpenemsNamedException {
		final var request = re.getPayload();
		return switch (request.getMethod()) {
		case "initiateConnect" -> {
			final var p = request.getParams();

			final var oem = JsonUtils.getAsStringOrElse(p, "oem", null);
			final var redirectUri = JsonUtils.getAsStringOrElse(p, "redirectUri", null);

			yield authService.initiateConnect(oem, redirectUri) //
					.thenApply(response -> new InitiateConnectResponseJson(re.getId(), response));
		}
		case GetTokenByCodeRequest.METHOD -> {
			final var r = GetTokenByCodeRequest.from(request);
			yield authService.tokenByCode(r.getOem(), r.getIdentifier(), r.getCode()).thenCompose(token -> {
				wsData.setToken(token.accessToken());

				return metadata.getUserByExternalId(token.sub()).thenApply(user -> {
					wsData.setUser(user);
					return new TokenResponse(re.getId(), token.accessToken(), token.refreshToken(), user);
				});
			});
		}
		case GetTokenByRefreshTokenRequest.METHOD -> {
			final var r = GetTokenByRefreshTokenRequest.from(request);
			yield authService.tokenByRefreshToken(r.getOem(), r.getRefreshToken()).thenCompose(token -> {
				wsData.setToken(token.accessToken());

				return metadata.getUserByExternalId(token.sub()).thenApply(user -> {
					wsData.setUser(user);
					return new TokenResponse(re.getId(), token.accessToken(), token.refreshToken(), user);
				});
			});
		}

		default -> null;
		};
	}

	private OAuthAuthenticationHandler() {
	}

	private static class InitiateConnectResponseJson extends JsonrpcResponseSuccess {

		private final InitiateConnectResponse initiateConnectResponse;

		public InitiateConnectResponseJson(UUID id, InitiateConnectResponse initiateConnectResponse) {
			super(id);
			this.initiateConnectResponse = initiateConnectResponse;
		}

		@Override
		public JsonObject getResult() {
			return JsonUtils.buildJsonObject() //
					.addProperty("identifier", this.initiateConnectResponse.identifier()) //
					.addProperty("state", this.initiateConnectResponse.state()) //
					.addProperty("loginUrl", this.initiateConnectResponse.loginUrl()) //
					.build();
		}
	}

	private static class TokenResponse extends JsonrpcResponseSuccess {

		private final String accessToken;
		private final String refreshToken;

		// TODO should be removed
		private final User user;

		public TokenResponse(UUID id, String accessToken, String refreshToken, User user) {
			super(id);
			this.accessToken = accessToken;
			this.refreshToken = refreshToken;
			this.user = user;
		}

		@Override
		public JsonObject getResult() {
			return JsonUtils.buildJsonObject() //
					.addProperty("accessToken", this.accessToken) //
					.addProperty("refreshToken", this.refreshToken) //
					.add("user", JsonUtils.buildJsonObject() //
							.addProperty("id", this.user.getId()) //
							.addProperty("name", this.user.getName()) //
							.addProperty("language", this.user.getLanguage().name())//
							.addProperty("hasMultipleEdges", this.user.hasMultipleEdges())//
							.add("settings", this.user.getSettings()) //
							.add("globalRole", this.user.getGlobalRole().asJson()) //
							.build()) //
					.build();
		}
	}

	private static class GetTokenByCodeRequest extends JsonrpcRequest {

		public static final String METHOD = "getTokenByCode";

		public static GetTokenByCodeRequest from(JsonrpcRequest request) throws OpenemsError.OpenemsNamedException {
			final var p = request.getParams();
			return new GetTokenByCodeRequest(//
					JsonUtils.getAsString(p, "oem"), //
					JsonUtils.getAsString(p, "identifier"), //
					JsonUtils.getAsString(p, "code") //
			);
		}

		private final String oem;
		private final String identifier;
		private final String code;

		public GetTokenByCodeRequest(String oem, String identifier, String code) {
			super(GetTokenByCodeRequest.METHOD);
			this.oem = oem;
			this.identifier = identifier;
			this.code = code;
		}

		public String getOem() {
			return this.oem;
		}

		public String getIdentifier() {
			return this.identifier;
		}

		public String getCode() {
			return this.code;
		}

		@Override
		public JsonObject getParams() {
			return JsonUtils.buildJsonObject() //
					.addProperty("oem", this.oem) //
					.addProperty("identifier", this.identifier) //
					.addProperty("code", this.code) //
					.build();
		}
	}

	private static class GetTokenByRefreshTokenRequest extends JsonrpcRequest {

		public static final String METHOD = "getTokenByRefreshToken";

		public static GetTokenByRefreshTokenRequest from(JsonrpcRequest request)
				throws OpenemsError.OpenemsNamedException {
			final var p = request.getParams();
			return new GetTokenByRefreshTokenRequest(//
					JsonUtils.getAsString(p, "oem"), //
					JsonUtils.getAsString(p, "refreshToken") //
			);
		}

		private final String oem;
		private final String refreshToken;

		public GetTokenByRefreshTokenRequest(String oem, String refreshToken) {
			super(GetTokenByCodeRequest.METHOD);
			this.oem = oem;
			this.refreshToken = refreshToken;
		}

		public String getOem() {
			return this.oem;
		}

		public String getRefreshToken() {
			return this.refreshToken;
		}

		@Override
		public JsonObject getParams() {
			return JsonUtils.buildJsonObject() //
					.addProperty("oem", this.oem) //
					.addProperty("refreshToken", this.refreshToken) //
					.build();
		}
	}

}
