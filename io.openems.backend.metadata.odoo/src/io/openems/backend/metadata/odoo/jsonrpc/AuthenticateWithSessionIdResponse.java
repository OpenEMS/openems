package io.openems.backend.metadata.odoo.jsonrpc;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.backend.metadata.odoo.EdgeCache;
import io.openems.backend.metadata.odoo.MyEdge;
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

	private static final Logger log = LoggerFactory.getLogger(AuthenticateWithSessionIdResponse.class);

	public static AuthenticateWithSessionIdResponse from(JsonrpcResponseSuccess response, String sessionId,
			EdgeCache edges) throws OpenemsNamedException {
		JsonObject r = response.getResult();
		JsonObject jUser = JsonUtils.getAsJsonObject(r, "user");
		MyUser user = new MyUser(//
				JsonUtils.getAsInt(jUser, "id"), //
				JsonUtils.getAsString(jUser, "name"), //
				sessionId);
		JsonArray jDevices = JsonUtils.getAsJsonArray(r, "devices");
		List<String> notAvailableEdges = new ArrayList<>();
		for (JsonElement jDevice : jDevices) {
			int odooId = JsonUtils.getAsInt(jDevice, "id");
			MyEdge edge = edges.getEdgeFromOdooId(odooId);
			if (edge == null) {
				notAvailableEdges.add(String.valueOf(odooId));
			} else {
				user.addEdgeRole(edge.getId(), Role.getRole(JsonUtils.getAsString(jDevice, "role")));
			}
		}
		if (!notAvailableEdges.isEmpty()) {
			log.warn("For User [" + user.getId() + "] following Edges are not available: "
					+ String.join(",", notAvailableEdges));
		}
		return new AuthenticateWithSessionIdResponse(response.getId(), user);
	}

	public static AuthenticateWithSessionIdResponse from(JsonObject j, String sessionId, EdgeCache edges)
			throws OpenemsNamedException {
		return from(JsonrpcResponseSuccess.from(j), sessionId, edges);
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
