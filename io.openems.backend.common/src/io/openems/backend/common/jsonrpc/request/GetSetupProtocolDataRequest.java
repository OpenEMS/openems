package io.openems.backend.common.jsonrpc.request;

import com.google.gson.JsonObject;

import io.openems.backend.common.metadata.Edge;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.utils.JsonUtils;

public class GetSetupProtocolDataRequest extends JsonrpcRequest {

	public static final String METHOD = "getSetupProtocolData";

	/**
	 * Create {@link GetSetupProtocolDataRequest} from a template
	 * {@link JsonrpcRequest}.
	 *
	 * @param request the template {@link JsonrpcRequest}
	 * @return Created {@link GetSetupProtocolDataRequest}
	 */
	public static GetSetupProtocolDataRequest from(JsonrpcRequest request) throws OpenemsNamedException {
		var params = request.getParams();

		return new GetSetupProtocolDataRequest(request, JsonUtils.getAsString(params, "edgeId"));
	}

	private final String edgeId;

	private GetSetupProtocolDataRequest(JsonrpcRequest request, String edgeId) {
		super(request, METHOD);
		this.edgeId = edgeId;
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
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
