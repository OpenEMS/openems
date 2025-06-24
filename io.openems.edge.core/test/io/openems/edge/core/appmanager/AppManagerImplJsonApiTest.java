package io.openems.edge.core.appmanager;

import static io.openems.edge.common.test.DummyUser.DUMMY_ADMIN;
import static io.openems.edge.common.test.DummyUser.DUMMY_GUEST;
import static io.openems.edge.common.test.DummyUser.DUMMY_INSTALLER;
import static io.openems.edge.common.test.DummyUser.DUMMY_OWNER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.jsonrpc.base.GenericJsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponse;
import io.openems.common.jsonrpc.base.JsonrpcResponseError;
import io.openems.common.jsonrpc.serialization.EmptyObject;
import io.openems.common.session.Role;
import io.openems.edge.common.jsonapi.Call;
import io.openems.edge.common.jsonapi.EdgeKeys;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.common.user.User;
import io.openems.edge.core.appmanager.jsonrpc.AddAppInstance;
import io.openems.edge.core.appmanager.jsonrpc.DeleteAppInstance;
import io.openems.edge.core.appmanager.jsonrpc.GetApp;
import io.openems.edge.core.appmanager.jsonrpc.GetAppAssistant;
import io.openems.edge.core.appmanager.jsonrpc.GetAppDescriptor;
import io.openems.edge.core.appmanager.jsonrpc.GetAppInstances;
import io.openems.edge.core.appmanager.jsonrpc.GetApps;
import io.openems.edge.core.appmanager.jsonrpc.GetEstimatedConfiguration;
import io.openems.edge.core.appmanager.jsonrpc.UpdateAppConfig;
import io.openems.edge.core.appmanager.jsonrpc.UpdateAppInstance;

public class AppManagerImplJsonApiTest {

	private JsonApiBuilder routes;

	@Before
	public void before() {
		final var appManager = new AppManagerImpl();
		this.routes = new JsonApiBuilder();

		appManager.buildJsonApiRoutes(this.routes);
	}

	@Test
	public void testGetAppsEndpoint() {
		testAccess(this::setupGetAppsEndpoint, Map.of(//
				Role.ADMIN, AccessRight.ALLOWED, //
				Role.INSTALLER, AccessRight.ALLOWED, //
				Role.OWNER, AccessRight.ALLOWED, //
				Role.GUEST, AccessRight.ALLOWED //
		));
	}

	@Test
	public void testGetAppEndpoint() {
		testAccess(this::setupGetAppEndpoint, Map.of(//
				Role.ADMIN, AccessRight.ALLOWED, //
				Role.INSTALLER, AccessRight.ALLOWED, //
				Role.OWNER, AccessRight.ALLOWED, //
				Role.GUEST, AccessRight.ALLOWED //
		));
	}

	@Test
	public void testGetAppAssistantEndpoint() {
		testAccess(this::setupGetAppAssistantEndpoint, Map.of(//
				Role.ADMIN, AccessRight.ALLOWED, //
				Role.INSTALLER, AccessRight.ALLOWED, //
				Role.OWNER, AccessRight.ALLOWED, //
				Role.GUEST, AccessRight.ALLOWED //
		));
	}

	@Test
	public void testGetAppDescriptorEndpoint() {
		testAccess(this::setupGetAppDescriptorEndpoint, Map.of(//
				Role.ADMIN, AccessRight.ALLOWED, //
				Role.INSTALLER, AccessRight.ALLOWED, //
				Role.OWNER, AccessRight.ALLOWED, //
				Role.GUEST, AccessRight.ALLOWED //
		));
	}

	@Test
	public void testGetAppInstancesEndpoint() {
		testAccess(this::setupGetAppInstancesEndpoint, Map.of(//
				Role.ADMIN, AccessRight.ALLOWED, //
				Role.INSTALLER, AccessRight.ALLOWED, //
				Role.OWNER, AccessRight.ALLOWED, //
				Role.GUEST, AccessRight.ALLOWED //
		));
	}

	@Test
	public void testAddAppInstanceEndpoint() {
		testAccess(this::setupAddAppInstanceEndpoint, Map.of(//
				Role.ADMIN, AccessRight.ALLOWED, //
				Role.INSTALLER, AccessRight.ALLOWED, //
				Role.OWNER, AccessRight.ALLOWED, //
				Role.GUEST, AccessRight.NOT_ALLOWED //
		));
	}

	@Test
	public void testUpdateAppInstanceEndpoint() {
		testAccess(this::setupUpdateAppInstanceEndpoint, Map.of(//
				Role.ADMIN, AccessRight.ALLOWED, //
				Role.INSTALLER, AccessRight.ALLOWED, //
				Role.OWNER, AccessRight.ALLOWED, //
				Role.GUEST, AccessRight.NOT_ALLOWED //
		));
	}

	@Test
	public void testDeleteAppInstanceEndpoint() {
		testAccess(this::setupDeleteAppInstanceEndpoint, Map.of(//
				Role.ADMIN, AccessRight.ALLOWED, //
				Role.INSTALLER, AccessRight.ALLOWED, //
				Role.OWNER, AccessRight.ALLOWED, //
				Role.GUEST, AccessRight.NOT_ALLOWED //
		));
	}

	@Test
	public void testGetEstimatedConfigurationEndpoint() {
		testAccess(this::setupGetEstimatedConfigurationEndpoint, Map.of(//
				Role.ADMIN, AccessRight.ALLOWED, //
				Role.INSTALLER, AccessRight.ALLOWED, //
				Role.OWNER, AccessRight.ALLOWED, //
				Role.GUEST, AccessRight.ALLOWED //
		));
	}

	@Test
	public void testUpdateAppConfigEndpoint() {
		testAccess(this::setupUpdateAppConfigEndpoint, Map.of(//
				Role.ADMIN, AccessRight.ALLOWED, //
				Role.INSTALLER, AccessRight.ALLOWED, //
				Role.OWNER, AccessRight.ALLOWED, //
				Role.GUEST, AccessRight.NOT_ALLOWED //
		));
	}

	private enum AccessRight {
		ALLOWED, NOT_ALLOWED
	}

	private static void testAccess(Function<User, JsonrpcResponse> setup, Map<Role, AccessRight> access) {
		for (var user : List.of(DUMMY_ADMIN, DUMMY_INSTALLER, DUMMY_OWNER, DUMMY_GUEST)) {
			final var accessRight = access.get(user.getRole());
			if (accessRight == null) {
				fail("Access right for role " + user.getRole() + " is not defined");
			}
			final var response = setup.apply(user);

			if (!(response instanceof JsonrpcResponseError e)) {
				if (accessRight == AccessRight.NOT_ALLOWED) {
					fail("Role " + user.getRole() + " expected to not have access but response was successful.");
				}
				continue;
			}

			switch (accessRight) {
			case ALLOWED -> assertNotEquals(OpenemsError.COMMON_ROLE_ACCESS_DENIED, e.getOpenemsError());
			case NOT_ALLOWED -> assertEquals(OpenemsError.COMMON_ROLE_ACCESS_DENIED, e.getOpenemsError());
			}

		}
	}

	private JsonrpcResponse setupGetAppsEndpoint(User user) {
		final var call = new Call<JsonrpcRequest, JsonrpcResponse>(
				GenericJsonrpcRequest.createRequest(new GetApps(), EmptyObject.INSTANCE));
		call.put(EdgeKeys.USER_KEY, user);
		this.routes.handle(call);

		return call.getResponse();
	}

	private JsonrpcResponse setupGetAppEndpoint(User user) {
		final var call = new Call<JsonrpcRequest, JsonrpcResponse>(
				GenericJsonrpcRequest.createRequest(new GetApp(), new GetApp.Request("")));
		call.put(EdgeKeys.USER_KEY, user);
		this.routes.handle(call);

		return call.getResponse();
	}

	private JsonrpcResponse setupGetAppAssistantEndpoint(User user) {
		final var call = new Call<JsonrpcRequest, JsonrpcResponse>(
				GenericJsonrpcRequest.createRequest(new GetAppAssistant(), new GetAppAssistant.Request("")));
		call.put(EdgeKeys.USER_KEY, user);
		this.routes.handle(call);

		return call.getResponse();
	}

	private JsonrpcResponse setupGetAppDescriptorEndpoint(User user) {
		final var call = new Call<JsonrpcRequest, JsonrpcResponse>(
				GenericJsonrpcRequest.createRequest(new GetAppDescriptor(), new GetAppDescriptor.Request("")));
		call.put(EdgeKeys.USER_KEY, user);
		this.routes.handle(call);

		return call.getResponse();
	}

	private JsonrpcResponse setupGetAppInstancesEndpoint(User user) {
		final var call = new Call<JsonrpcRequest, JsonrpcResponse>(
				GenericJsonrpcRequest.createRequest(new GetAppInstances(), new GetAppInstances.Request("")));
		call.put(EdgeKeys.USER_KEY, user);
		this.routes.handle(call);

		return call.getResponse();
	}

	private JsonrpcResponse setupGetEstimatedConfigurationEndpoint(User user) {
		final var call = new Call<JsonrpcRequest, JsonrpcResponse>(GenericJsonrpcRequest.createRequest(
				new GetEstimatedConfiguration(), new GetEstimatedConfiguration.Request("", "", new JsonObject())));
		call.put(EdgeKeys.USER_KEY, user);
		this.routes.handle(call);

		return call.getResponse();
	}

	private JsonrpcResponse setupUpdateAppConfigEndpoint(User user) {
		final var call = new Call<JsonrpcRequest, JsonrpcResponse>(GenericJsonrpcRequest
				.createRequest(new UpdateAppConfig(), new UpdateAppConfig.Request("", new JsonObject())));
		call.put(EdgeKeys.USER_KEY, user);
		this.routes.handle(call);

		return call.getResponse();
	}

	private JsonrpcResponse setupAddAppInstanceEndpoint(User user) {
		final var call = new Call<JsonrpcRequest, JsonrpcResponse>(GenericJsonrpcRequest
				.createRequest(new AddAppInstance(), new AddAppInstance.Request("", "", "", new JsonObject())));
		call.put(EdgeKeys.USER_KEY, user);
		this.routes.handle(call);

		return call.getResponse();
	}

	private JsonrpcResponse setupUpdateAppInstanceEndpoint(User user) {
		final var call = new Call<JsonrpcRequest, JsonrpcResponse>(GenericJsonrpcRequest.createRequest(
				new UpdateAppInstance(), new UpdateAppInstance.Request(UUID.randomUUID(), "", new JsonObject())));
		call.put(EdgeKeys.USER_KEY, user);
		this.routes.handle(call);

		return call.getResponse();
	}

	private JsonrpcResponse setupDeleteAppInstanceEndpoint(User user) {
		final var call = new Call<JsonrpcRequest, JsonrpcResponse>(GenericJsonrpcRequest
				.createRequest(new DeleteAppInstance(), new DeleteAppInstance.Request(UUID.randomUUID())));
		call.put(EdgeKeys.USER_KEY, user);
		this.routes.handle(call);

		return call.getResponse();
	}

}
