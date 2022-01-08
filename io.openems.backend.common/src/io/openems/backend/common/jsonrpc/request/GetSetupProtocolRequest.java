package io.openems.backend.common.jsonrpc.request;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.utils.JsonUtils;

public class GetSetupProtocolRequest extends JsonrpcRequest {

	public static final String METHOD = "getSetupProtocol";

	/**
	 * Create {@link GetSetupProtocolRequest} from a template
	 * {@link JsonrpcRequest}.
	 *
	 * @param request the template {@link JsonrpcRequest}
	 * @return Created {@link GetSetupProtocolRequest}
	 */
	public static GetSetupProtocolRequest from(JsonrpcRequest request) throws OpenemsNamedException {
		var params = request.getParams();

		return new GetSetupProtocolRequest(request, JsonUtils.getAsInt(params, "setupProtocolId"));
	}

	private final int setupProtocolId;

	private GetSetupProtocolRequest(JsonrpcRequest request, int setupProtocolId) {
		super(request, GetSetupProtocolRequest.METHOD);
		this.setupProtocolId = setupProtocolId;
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
				.addProperty("setupProtocolId", this.setupProtocolId) //
				.build();
	}

	/**
	 * Gets the Setup Protocol ID.
	 *
	 * @return the Setup Protocol ID
	 */
	public int getSetupProtocolId() {
		return this.setupProtocolId;
	}

}
