package io.openems.common.jsonrpc.notification;

import java.util.List;
import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.session.Role;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Notification for UI authentication with session_id.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "method": "authenticatedWithSessionId",
 *   "params": {
 *     "token": UUID,
 *     "edges": [{
 *       "id": String,
 *       "comment": String,
 *       "producttype: String,
 *       "version: String,
 *       "role: "admin" | "installer" | "owner" | "guest",
 *       "online: boolean
 *     }]
 *   }
 * }
 * </pre>
 */
public class UiAuthenticateWithSessionId extends JsonrpcNotification {

	public final static String METHOD = "authenticatedWithSessionId";

	private final UUID token;
	private final List<EdgeMetadata> metadatas;

	public UiAuthenticateWithSessionId(UUID token, List<EdgeMetadata> metadatas) {
		super(METHOD);
		this.token = token;
		this.metadatas = metadatas;
	}

	@Override
	public JsonObject getParams() {
		JsonArray edges = new JsonArray();
		for (EdgeMetadata metadata : this.metadatas) {
			edges.add(metadata.toJsonObject());
		}
		return JsonUtils.buildJsonObject() //
				.addProperty("token", this.token.toString()) //
				.add("edges", edges) //
				.build();
	}

	public UUID getToken() {
		return token;
	}

	public List<EdgeMetadata> getMetadatas() {
		return metadatas;
	}

	public static class EdgeMetadata {
		private final String id;
		private final String comment;
		private final String producttype;
		private final String version;
		private final Role role;
		private final boolean isOnline;

		public EdgeMetadata(String id, String comment, String producttype, String version, Role role,
				boolean isOnline) {
			this.id = id;
			this.comment = comment;
			this.producttype = producttype;
			this.version = version;
			this.role = role;
			this.isOnline = isOnline;
		}

		public JsonObject toJsonObject() {
			return JsonUtils.buildJsonObject() //
					.addProperty("id", this.id) //
					.addProperty("comment", this.comment) //
					.addProperty("producttype", this.producttype) //
					.addProperty("version", this.version) //
					.add("role", this.role.asJson()) //
					.addProperty("isOnline", this.isOnline) //
					.build();
		}
	}

}
