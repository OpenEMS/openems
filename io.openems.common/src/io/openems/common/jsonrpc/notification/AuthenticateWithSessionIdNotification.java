package io.openems.common.jsonrpc.notification;

import java.util.List;

import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.jsonrpc.response.AuthenticateWithPasswordResponse.EdgeMetadata;
import io.openems.common.session.AbstractUser;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Notification for UI authentication with a `session_id`
 * or `token`.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "method": "authenticatedWithSessionId",
 *   "params": {
 *     "token": String,
 *     "user": {@link AbstractUser#toJsonObject()}
 *     "edges": {@link EdgeMetadata#toJson(java.util.Collection)}
 *   }
 * }
 * </pre>
 */
public class AuthenticateWithSessionIdNotification extends JsonrpcNotification {

	public final static String METHOD = "authenticatedWithSessionId";

	private final String token;
	private final AbstractUser user;
	private final List<EdgeMetadata> edges;

	public AuthenticateWithSessionIdNotification(String token, AbstractUser user, List<EdgeMetadata> edges) {
		super(METHOD);
		this.token = token;
		this.user = user;
		this.edges = edges;
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
				.addProperty("token", this.token) //
				.add("user", this.user.toJsonObject()) //
				.add("edges", EdgeMetadata.toJson(this.edges)) //
				.build();
	}

}
