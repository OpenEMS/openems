package io.openems.edge.core.appmanager;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.GenericJsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponse;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.serialization.EmptyObject;
import io.openems.common.session.Role;
import io.openems.edge.common.jsonapi.Call;
import io.openems.edge.common.jsonapi.EdgeKeys;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.common.test.DummyUser;
import io.openems.edge.common.user.User;
import io.openems.edge.core.appmanager.jsonrpc.AddAppInstance;
import io.openems.edge.core.appmanager.jsonrpc.DeleteAppInstance;
import io.openems.edge.core.appmanager.jsonrpc.GetApps;

/**
 * Fluent permission assertions for an {@link OpenemsApp} under test.
 *
 * <p>
 * Every {@code can{Action}WithOnlyRoles} method asserts that the given
 * {@link Role}s are allowed to perform the action and that every other
 * {@link Role} is denied.
 */
public final class AppPermissionAssertions {

	private static final List<User> ALL_ROLE_USERS = List.of(DummyUser.DUMMY_GUEST, DummyUser.DUMMY_OWNER,
			DummyUser.DUMMY_INSTALLER, DummyUser.DUMMY_ADMIN);

	private final AppManagerTestBundle testBundle;
	private final OpenemsApp app;
	private final JsonObject properties;

	AppPermissionAssertions(AppManagerTestBundle testBundle, OpenemsApp app, JsonObject properties) {
		this.testBundle = testBundle;
		this.app = app;
		this.properties = properties;
	}

	/**
	 * Verifies that only users with one of the given {@code allowedRoles} can see
	 * the app via the {@link GetApps} JSON-RPC endpoint; every other {@link Role}
	 * is expected to not see the app.
	 *
	 * @param allowedRoles the roles that are expected to see the app
	 * @return this
	 */
	public AppPermissionAssertions canSeeWithOnlyRoles(Role... allowedRoles) {
		final var allowed = Set.of(allowedRoles);
		final var routes = new JsonApiBuilder();
		this.testBundle.sut.buildJsonApiRoutes(routes);

		for (var user : ALL_ROLE_USERS) {
			final var canSee = this.canSeeApp(routes, user);
			if (allowed.contains(user.getRole())) {
				assertTrue(canSee, () -> "Expected role [" + user.getRole() + "] to see app [" + this.app.getAppId()
						+ "] in getApps result");
			} else {
				assertTrue(!canSee, () -> "Expected role [" + user.getRole() + "] to not see app ["
						+ this.app.getAppId() + "] in getApps result");
			}
		}
		return this;
	}

	private boolean canSeeApp(JsonApiBuilder routes, User user) {
		final var call = new Call<JsonrpcRequest, JsonrpcResponse>(
				GenericJsonrpcRequest.createRequest(new GetApps(), EmptyObject.INSTANCE));
		call.put(EdgeKeys.USER_KEY, user);
		routes.handle(call);

		final var response = call.getResponse();
		if (!(response instanceof JsonrpcResponseSuccess success)) {
			return false;
		}
		final var apps = success.getResult().getAsJsonArray("apps");
		for (var appJson : apps) {
			if (this.app.getAppId().equals(appJson.getAsJsonObject().get("appId").getAsString())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Verifies that only users with one of the given {@code allowedRoles} are
	 * allowed to install the app; every other {@link Role} is expected to be denied
	 * with a {@link OpenemsNamedException}.
	 *
	 * @param allowedRoles the roles that are expected to be allowed to install the
	 *                     app
	 * @return this
	 */
	public AppPermissionAssertions canInstallWithOnlyRoles(Role... allowedRoles) {
		final var allowed = Set.of(allowedRoles);
		for (var user : ALL_ROLE_USERS) {
			final var request = new AddAppInstance.Request(this.app.getAppId(), "key", "alias", this.properties);
			if (allowed.contains(user.getRole())) {
				assertDoesNotThrow(() -> this.testBundle.sut.handleAddAppInstanceRequest(user, request), //
						() -> "Expected role [" + user.getRole() + "] to be allowed to install app ["
								+ this.app.getAppId() + "]");
			} else {
				assertThrows(OpenemsNamedException.class,
						() -> this.testBundle.sut.handleAddAppInstanceRequest(user, request), //
						() -> "Expected role [" + user.getRole() + "] to be denied installing app ["
								+ this.app.getAppId() + "]");
			}
		}
		return this;
	}

	/**
	 * Verifies that only users with one of the given {@code allowedRoles} are
	 * allowed to delete an installed instance of the app; every other {@link Role}
	 * is expected to be denied with a {@link OpenemsNamedException}. A fresh
	 * instance is installed and cleaned up for every checked role, bypassing
	 * install permissions (via a {@code null} user) so this method works
	 * independently of {@link #canInstallWithOnlyRoles}.
	 *
	 * @param allowedRoles the roles that are expected to be allowed to delete the
	 *                     app
	 * @return this
	 */
	public AppPermissionAssertions canDeleteWithOnlyRoles(Role... allowedRoles) {
		final var allowed = Set.of(allowedRoles);
		for (var user : ALL_ROLE_USERS) {
			final var instance = assertDoesNotThrow(() -> this.testBundle.sut.handleAddAppInstanceRequest(null,
					new AddAppInstance.Request(this.app.getAppId(), "key", "alias", this.properties))).instance();
			final var deleteRequest = new DeleteAppInstance.Request(instance.instanceId);
			if (allowed.contains(user.getRole())) {
				assertDoesNotThrow(() -> this.testBundle.sut.handleDeleteAppInstanceRequest(user, deleteRequest), //
						() -> "Expected role [" + user.getRole() + "] to be allowed to delete app ["
								+ this.app.getAppId() + "]");
			} else {
				assertThrows(OpenemsNamedException.class,
						() -> this.testBundle.sut.handleDeleteAppInstanceRequest(user, deleteRequest), //
						() -> "Expected role [" + user.getRole() + "] to be denied deleting app [" + this.app.getAppId()
								+ "]");
				// clean up, bypassing delete permissions, so state does not leak between roles
				assertDoesNotThrow(() -> this.testBundle.sut.handleDeleteAppInstanceRequest(null, deleteRequest));
			}
		}
		return this;
	}

}
