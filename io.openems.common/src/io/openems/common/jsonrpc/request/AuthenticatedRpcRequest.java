package io.openems.common.jsonrpc.request;

import java.util.UUID;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.GenericJsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.utils.JsonUtils;

// FIXME: Whats the sense of this Request if you drop the User completely?
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
		JsonrpcRequest payload = GenericJsonrpcRequest.from(JsonUtils.getAsJsonObject(p, "payload"));
		return new AuthenticatedRpcRequest(r.getId(), payload);
	}

	private final JsonrpcRequest payload;

	public AuthenticatedRpcRequest(JsonrpcRequest payload) {
		this(UUID.randomUUID(), payload);
	}

	public AuthenticatedRpcRequest(UUID id, JsonrpcRequest payload) {
		super(id, METHOD);
		this.payload = payload;
	}

	public JsonrpcRequest getPayload() {
		return payload;
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
				.add("payload", this.payload.toJsonObject()) //
				.build();
	}
}
