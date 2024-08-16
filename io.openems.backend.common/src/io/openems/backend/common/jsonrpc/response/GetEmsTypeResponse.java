package io.openems.backend.common.jsonrpc.response;

import java.util.UUID;

import com.google.gson.JsonObject;

import io.openems.backend.common.jsonrpc.request.AddEdgeToUserRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Response for {@link AddEdgeToUserRequest}.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {
 *     "emsType": String
 *   }
 * }
 * </pre>
 */
public class GetEmsTypeResponse extends JsonrpcResponseSuccess {

	private final String emsType;

	public GetEmsTypeResponse(UUID id, String emsType) {
		super(id);
		this.emsType = emsType;
	}

	@Override
	public JsonObject getResult() {
		return JsonUtils.buildJsonObject() //
				.addPropertyIfNotNull("emsType", this.emsType) //
				.build();
	}

}
