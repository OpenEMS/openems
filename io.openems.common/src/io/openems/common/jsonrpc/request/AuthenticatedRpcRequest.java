package io.openems.common.jsonrpc.request;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingFunction;
import io.openems.common.jsonrpc.base.GenericJsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.session.AbstractUser;
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
public class AuthenticatedRpcRequest<USER extends AbstractUser> extends JsonrpcRequest {

	/**
	 * Create {@link AuthenticatedRpcRequest} from a template
	 * {@link JsonrpcRequest}.
	 * 
	 * @param r           the template {@link JsonrpcRequest}
	 * @param userFactory a {@link ThrowingFunction} that parses a
	 *                    {@link JsonObject} to a {@link AbstractUser}
	 * @return the {@link AuthenticatedRpcRequest}
	 * @throws OpenemsNamedException on parse error
	 */
	public static <USER extends AbstractUser> AuthenticatedRpcRequest<USER> from(JsonrpcRequest r,
			ThrowingFunction<JsonObject, USER, OpenemsNamedException> userFactory) throws OpenemsNamedException {
		JsonObject p = r.getParams();
		USER user = userFactory.apply(JsonUtils.getAsJsonObject(p, "user"));
		JsonrpcRequest payload = GenericJsonrpcRequest.from(JsonUtils.getAsJsonObject(p, "payload"));
		return new AuthenticatedRpcRequest<USER>(r, user, payload);
	}

	public static final String METHOD = "authenticatedRpc";

	private final USER user;
	private final JsonrpcRequest payload;

	public AuthenticatedRpcRequest(USER user, JsonrpcRequest payload) {
		super(METHOD);
		this.user = user;
		this.payload = payload;
	}

	private AuthenticatedRpcRequest(JsonrpcRequest request, USER user, JsonrpcRequest payload) {
		super(request, METHOD);
		this.user = user;
		this.payload = payload;
	}

	/**
	 * Gets the {@link AbstractUser}.
	 * 
	 * @return User
	 */
	public USER getUser() {
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
