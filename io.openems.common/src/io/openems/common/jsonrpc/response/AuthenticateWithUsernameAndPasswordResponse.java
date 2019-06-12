package io.openems.common.jsonrpc.response;

import com.google.gson.JsonObject;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.shared.EdgeMetadata;
import io.openems.common.utils.JsonUtils;

import java.util.List;
import java.util.UUID;

/**
 * Represents a JSON-RPC Response for 'authenticateWithUsernameAndPassword'.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {
 *     "token": UUID,
 *     "edges": {@link EdgeMetadata#toJson(java.util.Collection)}
 *   }
 * }
 * </pre>
 */
public class AuthenticateWithUsernameAndPasswordResponse extends JsonrpcResponseSuccess {

	private final UUID token;
	private final List<EdgeMetadata> metadatas;

	public AuthenticateWithUsernameAndPasswordResponse(UUID id, UUID token, List<EdgeMetadata> metadatas) {
		super(id);
		this.token = token;
		this.metadatas = metadatas;
	}

	public UUID getToken() {
		return token;
	}

	public List<EdgeMetadata> getMetadatas() {
		return metadatas;
	}

	@Override
	public JsonObject getResult() {
		return JsonUtils.buildJsonObject() //
				.addProperty("token", this.token.toString()) //
				.add("edges", EdgeMetadata.toJson(this.metadatas)) //
				.build();
	}

}
