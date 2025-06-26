package io.openems.backend.common.jsonrpc.response;

import java.util.UUID;

import com.google.gson.JsonObject;

import io.openems.backend.common.jsonrpc.request.AddEdgeToUserRequest;
import io.openems.backend.common.metadata.Edge;
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
 *     "edge": Edge
 *     "serialNumber": String
 *   }
 * }
 * </pre>
 */
public class AddEdgeToUserResponse extends JsonrpcResponseSuccess {

	private final Edge edge;
	private final String serialNumber;

	public AddEdgeToUserResponse(UUID id, Edge edge, String serialNumber) {
		super(id);
		this.edge = edge;
		this.serialNumber = serialNumber;
	}

	@Override
	public JsonObject getResult() {
		return JsonUtils.buildJsonObject() //
				.add("edge", this.edge.toJsonObject()) //
				.addPropertyIfNotNull("serialNumber", this.serialNumber) //
				.build();
	}

}
