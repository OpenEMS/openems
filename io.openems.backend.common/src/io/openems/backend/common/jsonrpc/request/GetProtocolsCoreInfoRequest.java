package io.openems.backend.common.jsonrpc.request;

import com.google.gson.JsonObject;

import io.openems.backend.common.metadata.Edge;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.utils.JsonUtils;

public class GetProtocolsCoreInfoRequest extends JsonrpcRequest {

	public static final String METHOD = "getProtocolsCoreInfo";

	/**
	 * Create {@link GetProtocolsCoreInfoRequest} from a template
	 * {@link JsonrpcRequest}.
	 *
	 * @param request the template {@link JsonrpcRequest}
	 * @return Created {@link GetProtocolsCoreInfoRequest}
	 */
	public static GetProtocolsCoreInfoRequest from(JsonrpcRequest request) throws OpenemsNamedException {
		var params = request.getParams();

		return new GetProtocolsCoreInfoRequest(request, JsonUtils.getAsString(params, "edgeId"));
	}

	private final String edgeId;

	private GetProtocolsCoreInfoRequest(JsonrpcRequest request, String edgeId) {
		super(request, METHOD);
		this.edgeId = edgeId;
	}

	/**
	 * Get the {@link Edge} ID.
	 *
	 * @return the {@link Edge} ID
	 */
	public String getEdgeId() {
		return this.edgeId;
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
				.addProperty("edgeId", this.edgeId) //
				.build();
	}
}
