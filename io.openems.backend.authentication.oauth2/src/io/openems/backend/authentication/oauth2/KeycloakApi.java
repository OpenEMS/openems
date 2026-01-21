package io.openems.backend.authentication.oauth2;

import java.util.List;
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
	 * Creates a new user in Keycloak.
	 *
	 * @param bridgeHttp the {@link BridgeHttp} instance to use for the HTTP
	 * @param baseUrl    the base URL of the Keycloak server (without realm path)
	 * @param realm      the realm in which to create the user
	 * @param token      the access token for authentication
	 * @param username   the username of the new user
	 * @param email      the email of the new user
	 * @param firstName  the firstname of the new user
	 * @param lastName   the lastname of the new user
	 * @param enabled    whether the user should be enabled
	 * @return a {@link CompletableFuture} that completes with the user ID
	 */
	public static CompletableFuture<String> createUser(BridgeHttp bridgeHttp, String baseUrl, String realm,
			String token, String username, String email, String firstName, String lastName, boolean enabled) {
		return bridgeHttp.request(BridgeHttp.Endpoint.create(baseUrl + "/admin/realms/" + realm + "/users") //
				.setHeader("Authorization", "Bearer " + token) //
				.setBodyJson(JsonUtils.buildJsonObject() //
						.addProperty("username", username) //
						.addProperty("email", email) //
						.addProperty("firstName", firstName) //
						.addProperty("lastName", lastName) //
						.addProperty("enabled", enabled) //
						.build()) //
				.build()) //
				.thenApply(response -> {
					final var location = response.header().get("Location");
					if (location != null && !location.isEmpty()) {
						var locationHeader = location.getFirst();
						var parts = locationHeader.split("/");
						if (parts.length > 0) {
							return parts[parts.length - 1];
						}
					}
					throw new RuntimeException("Failed to create user: No Location header found in response");
				});
	}

	/**
	 * Fetches the realm roles from Keycloak.
	 *
	 * @param bridgeHttp the {@link BridgeHttp} instance to use for the HTTP
	 * @param baseUrl    the base URL of the Keycloak server (without realm path)
	 * @param realm      the realm from which to fetch the roles
	 * @param token      the access token for authentication
	 * @param search     an optional search term to filter roles by name
	 * @return a {@link CompletableFuture} that completes with a list of roles
	 */
	public static CompletableFuture<List<RealmRole>> getRealmRoles(BridgeHttp bridgeHttp, String baseUrl,
			String realm, String token, String search) {
		return bridgeHttp
				.requestJson(BridgeHttp.Endpoint
						.create(UrlBuilder.parse(baseUrl + "/admin/realms/" + realm + "/roles") //
								.withQueryParam("search", search) //
								.toEncodedString()) //
						.setHeader("Authorization", "Bearer " + token) //
						.build()) //
				.thenApply(response -> {
					return RealmRole.serializer().toListSerializer().deserialize(response.data());
				});
	}

	public record RealmRole(String id, String name, String description, boolean composite, boolean clientRole,
			String containerId) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link RealmRole}.
		 *
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<RealmRole> serializer() {
			return JsonSerializerUtil.jsonObjectSerializer(RealmRole.class, json -> {
				return new RealmRole(//
						json.getString("id"), //
						json.getString("name"), //
						json.getString("description"), //
						json.getBoolean("composite"), //
						json.getBoolean("clientRole"), //
						json.getString("containerId") //
				);
			}, obj -> {
				return JsonUtils.buildJsonObject() //
						.addProperty("id", obj.id()) //
						.addProperty("name", obj.name()) //
						.addProperty("description", obj.description()) //
						.addProperty("composite", obj.composite()) //
						.addProperty("clientRole", obj.clientRole()) //
						.addProperty("containerId", obj.containerId()) //
						.build();
			});
		}

	}

	/**
	 * Creates a realm role mapping for a user in Keycloak.
	 *
	 * @param bridgeHttp the {@link BridgeHttp} instance to use for the HTTP
	 * @param baseUrl    the base URL of the Keycloak server (without realm path)
	 * @param realm      the realm in which the user exists
	 * @param token      the access token for authentication
	 * @param userId     the ID of the user to map roles to
	 * @param roles      the list of {@link RealmRole} to map to the user
	 * @return a {@link CompletableFuture} that completes when the mapping is
	 *         created
	 */
	public static CompletableFuture<Void> createRealmRoleMapping(BridgeHttp bridgeHttp, String baseUrl, String realm,
			String token, String userId, List<RealmRole> roles) {
		return bridgeHttp
				.request(BridgeHttp.Endpoint
						.create(baseUrl + "/admin/realms/" + realm + "/users/" + userId + "/role-mappings/realm") //
						.setHeader("Authorization", "Bearer " + token) //
						.setBodyJson(RealmRole.serializer().toListSerializer().serialize(roles)) //
						.build()) //
				.thenAccept(FunctionUtils::doNothing);
	}

	/**
	 * Resets the password of a user in Keycloak.
	 *
	 * @param bridgeHttp the {@link BridgeHttp} instance to use for the HTTP
	 * @param baseUrl    the base URL of the Keycloak server (without realm path)
	 * @param realm      the realm in which the user exists
	 * @param token      the access token for authentication
	 * @param userId     the ID of the user whose password is to be reset
	 * @param temporary  whether the password is temporary
	 * @param password   the new password to set for the user
	 * @return a {@link CompletableFuture} that completes when the password is reset
	 */
	public static CompletableFuture<Void> resetPassword(BridgeHttp bridgeHttp, String baseUrl, String realm,
			String token, String userId, boolean temporary, String password) {
		return bridgeHttp
				.request(BridgeHttp.Endpoint
						.create(baseUrl + "/admin/realms/" + realm + "/users/" + userId + "/reset-password") //
						.setHeader("Authorization", "Bearer " + token) //
						.setBodyJson(JsonUtils.buildJsonObject() //
								.addProperty("type", "password") //
								.addProperty("value", password) //
								.addProperty("temporary", temporary) //
								.build()) //
						.setMethod(HttpMethod.PUT) //
						.build()) //
				.thenAccept(FunctionUtils::doNothing);
	}

	private KeycloakApi() {
	}

}
