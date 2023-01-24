package io.openems.common.jsonrpc.response;

import java.util.UUID;

import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.GetEdgeRequest;
import io.openems.common.jsonrpc.response.GetEdgesResponse.EdgeMetadata;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Response for {@link GetEdgeRequest}.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {
 *     "edge": {@link EdgeMetadata}
 *   }
 * }
 * </pre>
 */
public class GetEdgeResponse extends JsonrpcResponseSuccess {

	private final EdgeMetadata edgeMetadata;

	public GetEdgeResponse(UUID id, EdgeMetadata edgeMetadata) {
		super(id);
		this.edgeMetadata = edgeMetadata;
	}

	@Override
	public JsonObject getResult() {
		return JsonUtils.buildJsonObject() //
				.add("edge", this.edgeMetadata.toJsonObject()) //
				.build();
	}

}
