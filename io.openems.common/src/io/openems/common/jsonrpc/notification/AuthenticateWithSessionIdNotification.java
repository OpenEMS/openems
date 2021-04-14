package io.openems.common.jsonrpc.notification;

import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.session.AbstractUser;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Notification for UI authentication with a `session_id`
 * or `token`.
 * 
 * <p>
 * 
 * See {@link AbstractUser#toJsonObject()} for details.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "method": "authenticatedWithToken",
 *   "params": {
 *     "token": String,
 *     "user": {}
 *     "edges": {}
 *   }
 * }
 * </pre>
 */
public class AuthenticateWithSessionIdNotification extends JsonrpcNotification {

	public final static String METHOD = "authenticatedWithSessionId";

	private final String token;
	private final AbstractUser user;

	public AuthenticateWithSessionIdNotification(String token, AbstractUser user) {
		super(METHOD);
		this.token = token;
		this.user = user;
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject(this.user.toJsonObject()) //
				.addProperty("token", this.token) //
				.build();
	}

}
