package io.openems.common.bridge.http.oauth.model;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;

import java.util.List;

import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.utils.JsonUtils;

public final class GetTokens {

	public record OAuthClient(String clientId, String clientSecret, String redirectUri, String codeToTokenUrl) {
	}

	public sealed interface Grant {

		public record AuthorizationCodeGrant(String code, String codeVerifier) implements Grant {

		}

		public record RefreshTokenGrant(String refreshToken) implements Grant {

		}

	}

	public record OAuthTokens(String accessToken, String refreshToken) {

	}

	public record Request(OAuthClient oAuthClient, List<String> scopes, Grant grant) {

	}

	public record Response(String accessToken, Integer expiresIn, String refreshToken, Integer refreshExpiresIn,
			String tokenType, String idToken, Integer notBeforePolicy, String sessionState, String scope) {

		/**
		 * Returns a {@link JsonSerializer} for a
		 * {@link GetTokens.Response}.
		 *
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<GetTokens.Response> serializer() {
			return jsonObjectSerializer(json -> {
				return new GetTokens.Response(//
						json.getString("access_token"), //
						json.getOptionalInt("expires_in").orElse(null), //
						json.getString("refresh_token"), //
						json.getOptionalInt("refresh_expires_in").orElse(null), //
						json.getStringOrNull("token_type"), //
						json.getStringOrNull("id_token"), //
						json.getOptionalInt("not-before-policy").orElse(null), //
						json.getStringOrNull("session_state"), //
						json.getStringOrNull("scope") //
				);
			}, obj -> {
				return JsonUtils.buildJsonObject() //
						.addProperty("access_token", obj.accessToken()) //
						.addPropertyIfNotNull("expires_in", obj.expiresIn()) //
						.addProperty("refresh_token", obj.refreshToken()) //
						.addPropertyIfNotNull("refresh_expires_in", obj.refreshExpiresIn()) //
						.addPropertyIfNotNull("token_type", obj.tokenType()) //
						.addPropertyIfNotNull("id_token", obj.idToken()) //
						.addPropertyIfNotNull("not-before-policy", obj.notBeforePolicy()) //
						.addPropertyIfNotNull("session_state", obj.sessionState()) //
						.addPropertyIfNotNull("scope", obj.scope()) //
						.build();
			});
		}

	}

	private GetTokens() {
	}
}
