package io.openems.edge.common.jsonapi;

import static io.openems.edge.common.test.DummyUser.DUMMY_ADMIN;
import static io.openems.edge.common.test.DummyUser.DUMMY_INSTALLER;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.base.GenericJsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponse;
import io.openems.common.session.Role;

public class JsonrpcRoleEndpointGuardTest {

	@Test
	public void testTest() throws Exception {
		final var guard = new JsonrpcRoleEndpointGuard(Role.ADMIN);

		final var call = new Call<JsonrpcRequest, JsonrpcResponse>(
				new GenericJsonrpcRequest("method", new JsonObject()));

		call.put(EdgeKeys.USER_KEY, DUMMY_ADMIN);
		guard.test(call);
	}

	@Test(expected = Exception.class)
	public void testTestException() throws Exception {
		final var guard = new JsonrpcRoleEndpointGuard(Role.ADMIN);

		final var call = new Call<JsonrpcRequest, JsonrpcResponse>(
				new GenericJsonrpcRequest("method", new JsonObject()));

		call.put(EdgeKeys.USER_KEY, DUMMY_INSTALLER);
		guard.test(call);
	}

	@Test
	public void testGetRole() {
		final var role = Role.ADMIN;
		final var guard = new JsonrpcRoleEndpointGuard(role);

		assertEquals(role, guard.getRole());
	}

}
