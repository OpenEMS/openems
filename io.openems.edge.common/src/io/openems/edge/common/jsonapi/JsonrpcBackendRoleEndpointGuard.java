package io.openems.edge.common.jsonapi;

import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponse;
import io.openems.common.session.Role;

public final class JsonrpcBackendRoleEndpointGuard implements JsonrpcEndpointGuard {

	private final JsonrpcEndpointGuard roleGuard;
	private final boolean checkIfBackend;

	public JsonrpcBackendRoleEndpointGuard(Role role, boolean checkIfBackend) {
		this.roleGuard = EdgeGuards.roleIsAtleast(role);
		this.checkIfBackend = checkIfBackend;
	}

	@Override
	public void test(Call<JsonrpcRequest, JsonrpcResponse> call) throws Exception {
		final var isFromBackend = call.get(EdgeKeys.IS_FROM_BACKEND_KEY);

		if ((isFromBackend != null && isFromBackend) != this.checkIfBackend) {
			return;
		}

		this.roleGuard.test(call);
	}

}