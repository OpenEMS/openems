package io.openems.common.jsonrpc.request;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.utils.JsonUtils;

public class UpdateEdgeSettingsRequest extends JsonrpcRequest {

	public static final String METHOD = "updateEdgeSettings";

	/**
	 * Create {@link UpdateEdgeSettingsRequest} from a template
	 * {@link JsonrpcRequest}.
	 *
	 * @param request the template {@link JsonrpcRequest}
	 * @return the {@link UpdateEdgeSettingsRequest}
	 * @throws OpenemsError.OpenemsNamedException on parse error
	 */
	public static UpdateEdgeSettingsRequest from(JsonrpcRequest request) throws OpenemsError.OpenemsNamedException {
		var params = request.getParams();
		var settings = JsonUtils.getAsJsonObject(params, "settings");
		var edgeId = JsonUtils.getAsString(params, "edgeId");
		return new UpdateEdgeSettingsRequest(request, settings, edgeId);
	}

	private final JsonObject settings;
	private final String edgeId;

	private UpdateEdgeSettingsRequest(JsonrpcRequest request, JsonObject settings, String edgeId) {
		super(request, UpdateEdgeSettingsRequest.METHOD);
		this.settings = settings;
		this.edgeId = edgeId;
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
				.add("settings", this.settings) //
				.addProperty("edgeId", this.edgeId) //
				.build();
	}

	public JsonObject getSettings() {
		return this.settings;
	}

	public String getEdgeId() {
		return this.edgeId;
	}
}
