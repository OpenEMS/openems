package io.openems.backend.metadata.odoo.odoo.jsonrpc;

import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.backend.metadata.odoo.EdgeCache;
import io.openems.backend.metadata.odoo.MyUser;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.session.Role;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a successful JSON-RPC Response for 'EmptyRequest' to
 * '/openems_backend/info'.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {
 *     "user": {
 *       "id": number,
 *       "name": string
 *     },
 *     "devices": [{
 *       "id": number,
 *       "role": string
 *     }]
 *   }
 * }
 * </pre>
 */
public class AuthenticateWithSessionIdResponse extends JsonrpcResponseSuccess {

	public static AuthenticateWithSessionIdResponse from(JsonrpcResponseSuccess response, String sessionId,
			EdgeCache edgeCache, boolean isMetadataServiceInitialized) throws OpenemsNamedException {
		JsonObject r = response.getResult();

		// Parse Device-Roles
		JsonArray devices = JsonUtils.getAsJsonArray(r, "devices");
		NavigableMap<String, Role> roles = new TreeMap<>();
		for (JsonElement device : devices) {
			String edgeId = JsonUtils.getAsString(device, "name");
			Role role = Role.getRole(JsonUtils.getAsString(device, "role"));
			roles.put(edgeId, role);
		}

		JsonObject user = JsonUtils.getAsJsonObject(r, "user");
		return new AuthenticateWithSessionIdResponse(response.getId(), //
				new MyUser(//
						JsonUtils.getAsInt(user, "id"), //
						JsonUtils.getAsString(user, "name"), //
						Role.getRole(JsonUtils.getAsString(user, "global_role")), //
						roles));
	}

	private final MyUser user;

	public AuthenticateWithSessionIdResponse(UUID id, MyUser user) {
		super(id);
		this.user = user;
	}

	public MyUser getUser() {
		return user;
	}

	@Override
	public JsonObject getResult() {
		// TODO
		return new JsonObject();
	}

}
