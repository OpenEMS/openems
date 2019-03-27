package io.openems.common.jsonrpc.request;

import java.util.UUID;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.GenericJsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.session.User;
import io.openems.common.utils.JsonUtils;

/**
 * Wraps a JSON-RPC Request from an authenticated User.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "authenticatedRpc",
 *   "params": {
 *     "user": {
 *       "id": string,
 *       "name: string,
 *       "role": string
 *     },
 *     "payload": {@link JsonrpcRequest}
 *   }
 * }
 * </pre>
 */
public class AuthenticatedRpcRequest extends JsonrpcRequest {

	public final static String METHOD = "authenticatedRpc";

	public static AuthenticatedRpcRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		JsonObject p = r.getParams();
		User user = User.from(JsonUtils.getAsJsonObject(p, "user"));
		JsonrpcRequest payload = GenericJsonrpcRequest.from(JsonUtils.getAsJsonObject(p, "payload"));
		return new AuthenticatedRpcRequest(r.getId(), user, payload);
	}

	private final User user;
	private final JsonrpcRequest payload;

	public AuthenticatedRpcRequest(User user, JsonrpcRequest payload) {
		this(UUID.randomUUID(), user, payload);
	}

	public AuthenticatedRpcRequest(UUID id, User user, JsonrpcRequest payload) {
		super(id, METHOD);
		this.user = user;
		this.payload = payload;
	}

	public User getUser() {
		return user;
	}

	public JsonrpcRequest getPayload() {
		return payload;
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
				.add("user", this.user.toJson()) //
				.add("payload", this.payload.toJsonObject()) //
				.build();
	}

}
