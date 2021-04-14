package io.openems.common.jsonrpc.response;

import java.util.UUID;

import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.shared.EdgeMetadata;
import io.openems.common.session.AbstractUser;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Response for 'authenticateWithPassword'.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {
 *     "token": String,
 *     "user": {@link UserMetadata#toJsonObject()}
 *     "edges": {@link EdgeMetadata#toJson(java.util.Collection)}
 *   }
 * }
 * </pre>
 */
public class AuthenticateWithPasswordResponse extends JsonrpcResponseSuccess {

	private final String token;
	private final AbstractUser user;

	public AuthenticateWithPasswordResponse(UUID id, String token, AbstractUser user) {
		super(id);
		this.token = token;
		this.user = user;
	}

	@Override
	public JsonObject getResult() {
		return JsonUtils.buildJsonObject(this.user.toJsonObject()) //
				.addProperty("token", this.token) //
				.build();
	}

}
