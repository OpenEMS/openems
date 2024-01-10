package io.openems.backend.common.jsonrpc.request;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.backend.common.alerting.UserAlertingSettings;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Request for 'getEdgeConfig'.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "setUserAlertingConfigs",
 *   "params": {
 *     "edgeId": string,
 *      "userSettings": [
 *          {
 *           userLogin: string,
 *           offlineEdgeDelay: number,
 *           faultEdgeDelay: number,
 *           warningEdgeDelay: number
 *          }
 *      ]
 *   }
 * </pre>
 */
public class SetUserAlertingConfigsRequest extends JsonrpcRequest {

	public static final String METHOD = "setUserAlertingConfigs";

	/**
	 * Create {@link SetUserAlertingConfigsRequest} from a template
	 * {@link JsonrpcRequest}.
	 *
	 * @param request the template {@link JsonrpcRequest}
	 * @return the {@link SetUserAlertingConfigsRequest}
	 * @throws OpenemsNamedException on parse error
	 */
	public static SetUserAlertingConfigsRequest from(JsonrpcRequest request) throws OpenemsNamedException {
		return new SetUserAlertingConfigsRequest(request);
	}

	private final String edgeId;
	private final List<UserAlertingSettings> userSettings = new ArrayList<>();

	private SetUserAlertingConfigsRequest(JsonrpcRequest request) throws OpenemsNamedException {
		super(request, SetUserAlertingConfigsRequest.METHOD);
		var params = request.getParams();

		this.edgeId = JsonUtils.getAsString(params, "edgeId");
		JsonUtils.getAsJsonArray(params, "userSettings").forEach(user -> {
			var userJsonObject = user.getAsJsonObject();
			try {
				var userLogin = JsonUtils.getAsString(userJsonObject, "userLogin");
				var offlineEdgeDelay = JsonUtils.getAsInt(userJsonObject, "offlineEdgeDelay");
				var faultEdgeDelay = JsonUtils.getAsInt(userJsonObject, "faultEdgeDelay");
				var warningEdgeDelay = JsonUtils.getAsInt(userJsonObject, "warningEdgeDelay");

				this.userSettings
						.add(new UserAlertingSettings(userLogin, offlineEdgeDelay, faultEdgeDelay, warningEdgeDelay));
			} catch (OpenemsNamedException e) {
				e.printStackTrace();
			}
		});
	}

	/**
	 * Get the Edge-ID.
	 *
	 * @return the Edge-ID
	 */
	public String getEdgeId() {
		return this.edgeId;
	}

	/**
	 * Get list of {@link UserAlertingSetting}.
	 *
	 * @return list of {@link UserAlertingSetting}
	 */
	public List<UserAlertingSettings> getUserSettings() {
		return this.userSettings;
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
				.addProperty("edgeId", this.edgeId) //
				.add("userSettings", JsonUtils.generateJsonArray(this.userSettings, this::toJson)) //
				.build();
	}

	private JsonElement toJson(UserAlertingSettings setting) {
		return JsonUtils.buildJsonObject() //
				.addProperty("userLogin", setting.userLogin()) //
				.addProperty("offlineEdgeDelay", setting.edgeOfflineDelay()) //
				.addProperty("faultEdgeDelay", setting.edgeFaultDelay()) //
				.addProperty("warningEdgeDelay", setting.edgeWarningDelay()) //
				.build();
	}

}
