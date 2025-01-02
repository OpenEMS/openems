package io.openems.edge.common.jsonapi;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;

import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponse;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.session.Role;
import io.openems.common.utils.JsonUtils;

public final class JsonrpcRoleEndpointGuard implements JsonrpcEndpointGuard {

	private final Role role;

	public JsonrpcRoleEndpointGuard(Role role) {
		this.role = role;
	}

	@Override
	public void test(Call<JsonrpcRequest, JsonrpcResponse> call) throws Exception {
		call.get(EdgeKeys.USER_KEY).getRole().assertIsAtLeast(call.getRequest().getMethod(), this.role);
	}

	public Role getRole() {
		return this.role;
	}

	/**
	 * Returns a {@link JsonSerializer} for a {@link JsonrpcRoleEndpointGuard}.
	 * 
	 * @return the created {@link JsonSerializer}
	 */
	public static JsonSerializer<JsonrpcRoleEndpointGuard> serializer() {
		return jsonObjectSerializer(JsonrpcRoleEndpointGuard.class,
				t -> new JsonrpcRoleEndpointGuard(Role.getRole(t.getStringPath("role") //
						.get())),
				t -> JsonUtils.buildJsonObject() //
						.addProperty("name", "role") //
						.addProperty("description", "User-Role has to be at least " + t.getRole()) //
						.addProperty("role", t.getRole()) //
						.build());
	}

}