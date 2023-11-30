package io.openems.common.jsonrpc.request;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.utils.JsonUtils;

/**
 * Updates the User Settings.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": UUID,
 *   "method": "updateUserSettings",
 *   "params": {
 *      "settings": {}
 *   }
 * }
 * </pre>
 */
public class UpdateUserSettingsRequest extends JsonrpcRequest {

	public static final String METHOD = "updateUserSettings";

	/**
	 * Create {@link UpdateUserSettingsRequest} from a template
	 * {@link JsonrpcRequest}.
	 *
	 * @param request the template {@link JsonrpcRequest}
	 * @return the {@link UpdateUserSettingsRequest}
	 * @throws OpenemsNamedException on parse error
	 */
	public static UpdateUserSettingsRequest from(JsonrpcRequest request) throws OpenemsNamedException {
		var params = request.getParams();
		var settings = JsonUtils.getAsJsonObject(params, "settings");
		return new UpdateUserSettingsRequest(request, settings);
	}

	private final JsonObject settings;

	private UpdateUserSettingsRequest(JsonrpcRequest request, JsonObject settings) throws OpenemsException {
		super(request, UpdateUserSettingsRequest.METHOD);
		this.settings = settings;
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
				.add("settings", this.settings) //
				.build();
	}
	
	public JsonObject getSettings() {
		return this.settings;
	}
}
