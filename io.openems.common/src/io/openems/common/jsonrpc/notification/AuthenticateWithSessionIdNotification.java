package io.openems.common.jsonrpc.notification;

import java.util.List;
import java.util.UUID;

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
 *     "token": UUID,
 *     "edges": {@link EdgeMetadata#toJson(java.util.Collection)}
 *   }
 * }
 * </pre>
 */
public class AuthenticateWithSessionIdNotification extends JsonrpcNotification {

	public static final String METHOD = "authenticatedWithSessionId";

	private final UUID token;
	private final List<EdgeMetadata> metadatas;

	public AuthenticateWithSessionIdNotification(UUID token, List<EdgeMetadata> metadatas) {
		super(METHOD);
		this.token = token;
		this.metadatas = metadatas;
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
				.addProperty("token", this.token.toString()) //
				.add("edges", EdgeMetadata.toJson(this.metadatas)) //
				.build();
	}

	public UUID getToken() {
		return token;
	}

	public List<EdgeMetadata> getMetadatas() {
		return metadatas;
	}

}
