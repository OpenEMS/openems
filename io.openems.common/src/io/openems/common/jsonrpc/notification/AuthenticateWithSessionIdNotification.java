package io.openems.common.jsonrpc.notification;

import java.util.List;

import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.jsonrpc.shared.EdgeMetadata;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Notification for UI authentication with session_id.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "method": "authenticatedWithSessionId",
 *   "params": {
 *     "token": String,
 *     "edges": {@link EdgeMetadata#toJson(java.util.Collection)}
 *   }
 * }
 * </pre>
 */
public class AuthenticateWithSessionIdNotification extends JsonrpcNotification {

	public final static String METHOD = "authenticatedWithSessionId";

	private final String token;
	private final List<EdgeMetadata> metadatas;

	public AuthenticateWithSessionIdNotification(String token, List<EdgeMetadata> metadatas) {
		super(METHOD);
		this.token = token;
		this.metadatas = metadatas;
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
				.addProperty("token", this.token) //
				.add("edges", EdgeMetadata.toJson(this.metadatas)) //
				.build();
	}

	public String getToken() {
		return this.token;
	}

	public List<EdgeMetadata> getMetadatas() {
		return this.metadatas;
	}

}
