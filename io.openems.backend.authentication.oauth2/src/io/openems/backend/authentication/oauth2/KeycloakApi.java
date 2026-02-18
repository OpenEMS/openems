package io.openems.backend.authentication.oauth2;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.HttpMethod;
import io.openems.common.bridge.http.api.UrlBuilder;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.serialization.JsonSerializerUtil;
import io.openems.common.utils.FunctionUtils;
import io.openems.common.utils.JsonUtils;

/**
 * Keycloak Admin API client.
 *
 * <p>
 * This class provides methods for Keycloak-specific admin operations like
 * creating users, managing roles, and resetting passwords.
 * </p>
 *
 * <p>
 * For standard OIDC operations (token requests, logout, etc.), use
 * {@link OidcClient} instead.
 * </p>
 */
public final class KeycloakApi {

	/**
	 * Gets a token from Keycloak using the client credentials grant type.
	 *
	 * <p>
	 * To use this method, the client must be configured in Keycloak to allow
	 * "Service accounts roles".
	 * </p>
	 *
	 * @param bridgeHttp   the {@link BridgeHttp} instance to use for the HTTP request
	 * @param issuerUrl    the issuer URL of the Keycloak server
	 * @param clientId     the client ID registered in Keycloak
	 * @param clientSecret the client secret registered in Keycloak
	 * @return a {@link CompletableFuture} that completes with the access token
	 */
	public static CompletableFuture<String> getToken(
			BridgeHttp bridgeHttp,
			String issuerUrl,
			String clientId,
			String clientSecret) {

		return bridgeHttp.requestJson(
				BridgeHttp.Endpoint.create(issuerUrl + "/protocol/openid-connect/token")
						.setBodyFormEncoded(Map.of(
								"grant_type", "client_credentials",
								"client_id", clientId,
								"client_secret", clientSecret))
						.build())
				.thenApply(response ->
						response.data().getAsJsonObject().get("access_token").getAsString());
	}

	/**
	 * Gets a token from Keycloak using the password grant type.
	 *
	 * @param bridgeHttp   the {@link BridgeHttp} instance to use for the HTTP request
	 * @param issuerUrl    the issuer URL of the Keycloak server
	 * @param clientId     the client ID registered in Keycloak
	 * @param clientSecret the client secret registered in Keycloak
	 * @param username     the username of the user
	 * @param password     the password of the user
	 * @return a {@link CompletableFuture} that completes with the access token
	 */
	public static CompletableFuture<String> getToken(
			BridgeHttp bridgeHttp,
			String issuerUrl,
			String clientId,
			String clientSecret,
			String username,
			String password) {

		return bridgeHttp.requestJson(
				BridgeHttp.Endpoint.create(issuerUrl + "/protocol/openid-connect/token")
						.setBodyFormEncoded(Map.of(
								"grant_type", "password",
								"client_id", clientId,
								"client_secret", clientSecret,
								"username", username,
								"password", password))
						.build())
				.thenApply(response ->
						response.data().getAsJsonObject().get("access_token").getAsString());
	}

	/**
	 * Creates a new user in Keycloak.
	 */
	public static CompletableFuture<String> createUser(
			BridgeHttp bridgeHttp,
			String baseUrl,
			String realm,
			String token,
			String username,
			String email,
			String firstName,
			String lastName,
			boolean enabled) {

		return bridgeHttp.request(
				BridgeHttp.Endpoint.create(baseUrl + "/admin/realms/" + realm + "/users")
						.setHeader("Authorization", "Bearer " + token)
						.setBodyJson(JsonUtils.buildJsonObject()
								.addProperty("username", username)
								.addProperty("email", email)
								.addProperty("firstName", firstName)
								.addProperty("lastName", lastName)
								.addProperty("enabled", enabled)
								.build())
						.build())
				.thenApply(response -> {
					var location = response.header().get("Location");
					if (location != null && !location.isEmpty()) {
						var parts = location.getFirst().split("/");
						return parts[parts.length - 1];
					}
					throw new RuntimeException("Failed to create user: No Location header found");
				});
	}

	/**
	 * Fetches the realm roles from Keycloak.
	 */
	public static CompletableFuture<List<RealmRole>> getRealmRoles(
			BridgeHttp bridgeHttp,
			String baseUrl,
			String realm,
			String token,
			String search) {

		return bridgeHttp.requestJson(
				BridgeHttp.Endpoint.create(
						UrlBuilder.parse(baseUrl + "/admin/realms/" + realm + "/roles")
								.withQueryParam("search", search)
								.toEncodedString())
						.setHeader("Authorization", "Bearer " + token)
						.build())
				.thenApply(response ->
						RealmRole.serializer().toListSerializer().deserialize(response.data()));
	}

	public record RealmRole(
			String id,
			String name,
			String description,
			boolean composite,
			boolean clientRole,
			String containerId) {

		public static JsonSerializer<RealmRole> serializer() {
			return JsonSerializerUtil.jsonObjectSerializer(
					RealmRole.class,
					json -> new RealmRole(
							json.getString("id"),
							json.getString("name"),
							json.getString("description"),
							json.getBoolean("composite"),
							json.getBoolean("clientRole"),
							json.getString("containerId")),
					obj -> JsonUtils.buildJsonObject()
							.addProperty("id", obj.id())
							.addProperty("name", obj.name())
							.addProperty("description", obj.description())
							.addProperty("composite", obj.composite())
							.addProperty("clientRole", obj.clientRole())
							.addProperty("containerId", obj.containerId())
							.build());
		}
	}

	/**
	 * Creates a realm role mapping for a user.
	 */
	public static CompletableFuture<Void> createRealmRoleMapping(
			BridgeHttp bridgeHttp,
			String baseUrl,
			String realm,
			String token,
			String userId,
			List<RealmRole> roles) {

		return bridgeHttp.request(
				BridgeHttp.Endpoint.create(
						baseUrl + "/admin/realms/" + realm + "/users/" + userId + "/role-mappings/realm")
						.setHeader("Authorization", "Bearer " + token)
						.setBodyJson(RealmRole.serializer().toListSerializer().serialize(roles))
						.build())
				.thenAccept(FunctionUtils::doNothing);
	}

	/**
	 * Resets the password of a user.
	 */
	public static CompletableFuture<Void> resetPassword(
			BridgeHttp bridgeHttp,
			String baseUrl,
			String realm,
			String token,
			String userId,
			boolean temporary,
			String password) {

		return bridgeHttp.request(
				BridgeHttp.Endpoint.create(
						baseUrl + "/admin/realms/" + realm + "/users/" + userId + "/reset-password")
						.setHeader("Authorization", "Bearer " + token)
						.setMethod(HttpMethod.PUT)
						.setBodyJson(JsonUtils.buildJsonObject()
								.addProperty("type", "password")
								.addProperty("value", password)
								.addProperty("temporary", temporary)
								.build())
						.build())
				.thenAccept(FunctionUtils::doNothing);
	}

	private KeycloakApi() {
	}
}
