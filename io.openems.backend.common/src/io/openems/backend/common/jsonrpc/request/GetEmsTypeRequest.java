package io.openems.backend.common.jsonrpc.request;

import com.google.gson.JsonObject;

import io.openems.backend.common.metadata.Edge;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.utils.JsonUtils;

/**
 * Gets the ems type of a edge.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": UUID,
 *   "method": "getEmsType",
 *   "params": {
 *      "edgeId": string
 *   }
 * }
 * </pre>
 */
public class GetEmsTypeRequest extends JsonrpcRequest {

	public static final String METHOD = "getEmsType";

	/**
	 * Create {@link GetEmsTypeRequest} from a template {@link JsonrpcRequest}.
	 *
	 * @param request the template {@link JsonrpcRequest}
	 * @return Created {@link GetEmsTypeRequest}
	 */
	public static GetEmsTypeRequest from(JsonrpcRequest request) throws OpenemsNamedException {
		var params = request.getParams();

		return new GetEmsTypeRequest(request, JsonUtils.getAsString(params, "edgeId"));
	}

	private final String edgeId;

	private GetEmsTypeRequest(JsonrpcRequest request, String edgeId) {
		super(request, METHOD);
		this.edgeId = edgeId;
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
				.addProperty("edgeId", this.edgeId) //
				.build();
	}

	/**
	 * Get the {@link Edge} ID.
	 *
	 * @return the {@link Edge} ID
	 */
	public String getEdgeId() {
		return this.edgeId;
	}

}
