package io.openems.common.jsonrpc.request;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.utils.JsonUtils;

/**
 * JSON-RPC Request for getting a edge.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "getEdge",
 *   "params": {
 *     "edgeId": string
 *   }
 * }
 * </pre>
 */
public class GetEdgeRequest extends JsonrpcRequest {

	/**
	 * Creates a {@link GetEdgeRequest} from a {@link JsonrpcRequest}.
	 *
	 * @param request the {@link JsonrpcRequest}
	 * @return the {@link GetEdgeRequest}
	 * @throws OpenemsNamedException on parse error
	 */
	public static GetEdgeRequest from(JsonrpcRequest request) throws OpenemsNamedException {
		var params = request.getParams();
		var edgeId = JsonUtils.getAsString(params, "edgeId");
		return new GetEdgeRequest(request, edgeId);
	}

	public static final String METHOD = "getEdge";

	public final String edgeId;

	protected GetEdgeRequest(JsonrpcRequest request, String edgeId) {
		super(request, METHOD);
		this.edgeId = edgeId;
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
				.addProperty("edgeId", this.edgeId)//
				.build();
	}

}
