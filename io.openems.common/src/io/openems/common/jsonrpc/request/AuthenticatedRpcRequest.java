package io.openems.common.jsonrpc.request;

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

	public static final String METHOD = "authenticatedRpc";

	/**
	 * Create {@link AuthenticatedRpcRequest} from a template
	 * {@link JsonrpcRequest}.
	 * 
	 * @param r the template {@link JsonrpcRequest}
	 * @return the {@link AuthenticatedRpcRequest}
	 * @throws OpenemsNamedException on parse error
	 */
	public static AuthenticatedRpcRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		JsonObject p = r.getParams();
		User user = User.from(JsonUtils.getAsJsonObject(p, "user"));
		JsonrpcRequest payload = GenericJsonrpcRequest.from(JsonUtils.getAsJsonObject(p, "payload"));
		return new AuthenticatedRpcRequest(r, user, payload);
	}

	private final User user;
	private final JsonrpcRequest payload;

	public AuthenticatedRpcRequest(User user, JsonrpcRequest payload) {
		super(METHOD);
		this.user = user;
		this.payload = payload;
	}

	private AuthenticatedRpcRequest(JsonrpcRequest request, User user, JsonrpcRequest payload) {
		super(request, METHOD);
		this.user = user;
		this.payload = payload;
	}

	/**
	 * Gets the {@link User}.
	 * 
	 * @return User
	 */
	public User getUser() {
		return this.user;
	}

	/**
	 * Gets the Payload {@link JsonrpcRequest}.
	 * 
	 * @return Payload
	 */
	public JsonrpcRequest getPayload() {
		return this.payload;
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
				.add("user", this.user.toJsonObject()) //
				.add("payload", this.payload.toJsonObject()) //
				.build();
	}

}
