package io.openems.common.jsonrpc.request;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingFunction;
import io.openems.common.jsonrpc.base.GenericJsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.session.AbstractUser;
import io.openems.common.session.Role;
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
	 * <p>
	 * This method is called by OpenEMS Edge after it receives a message from
	 * OpenEMS Backend. As such the `edgeId` is empty here.
	 *
	 * @param <USER>      the type of User
	 * @param r           the template {@link JsonrpcRequest}
	 * @param userFactory a {@link ThrowingFunction} that parses the following
	 *                    {@link JsonObject} structure to a {@link AbstractUser}
	 *
	 *                    <pre>
	 *     {
	 *       "id": string,
	 *       "name": string,
	 *       "role": {@link Role}
	 *     }
	 *                    </pre>
	 *
	 * @return the {@link AuthenticatedRpcRequest}
	 * @throws OpenemsNamedException on parse error
	 */
	public static <USER extends AbstractUser> AuthenticatedRpcRequest<USER> from(JsonrpcRequest r,
			ThrowingFunction<JsonObject, USER, OpenemsNamedException> userFactory) throws OpenemsNamedException {
		var p = r.getParams();
		final var userObj = JsonUtils.getAsJsonObject(p, "user");
		var user = userFactory.apply(userObj);
		final var role = Role.getRole(JsonUtils.getAsString(userObj, "role"));
		JsonrpcRequest payload = GenericJsonrpcRequest.from(JsonUtils.getAsJsonObject(p, "payload"));
		return new AuthenticatedRpcRequest<>(r, user, role, payload);
	}

	public static final String METHOD = "authenticatedRpc";

	private final USER user;
	private final Role role;
	private final JsonrpcRequest payload;

	public AuthenticatedRpcRequest(USER user, Role role, JsonrpcRequest payload) {
		super(AuthenticatedRpcRequest.METHOD);
		this.user = user;
		this.role = role;
		this.payload = payload;
	}

	private AuthenticatedRpcRequest(JsonrpcRequest request, USER user, Role role, JsonrpcRequest payload) {
		super(request, AuthenticatedRpcRequest.METHOD);
		this.user = user;
		this.role = role;
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

	/**
	 * This method is called in OpenEMS Backend and formats the
	 * {@link AuthenticatedRpcRequest}, especially the `user` property so, that it
	 * contains only the required information for OpenEMS Edge and can be parsed via
	 * {@link #from(JsonrpcRequest, ThrowingFunction)}.
	 */
	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
				.add("user", JsonUtils.buildJsonObject() //
						.addProperty("id", this.user.getId()) //
						.addProperty("name", this.user.getName()) //
						.addPropertyIfNotNull("language", this.user.getLanguage()) //
						.add("role", this.role.asJson()) //
						.build()) //
				.add("payload", this.payload.toJsonObject()) //
				.build();
	}

}
