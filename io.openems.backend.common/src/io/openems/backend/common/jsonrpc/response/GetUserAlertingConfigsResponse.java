package io.openems.backend.common.jsonrpc.response;

import java.util.List;
import java.util.UUID;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.backend.common.alerting.UserAlertingSettings;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Response for 'getAlertingConfig'.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {
 *      "currentUserSettings": {
 *           "userLogin": "string",
 *           "faultEdgeDelay": "number",
 *           "offlineEdgeDelay": "number",
 *           "warningEdgeDelay": "number"
 *          }
 *      "otherUsersSettings?": [
 *          {
 *           "userLogin": "string",
 *           "faultEdgeDelay": "number",
 *           "offlineEdgeDelay": "number",
 *           "warningEdgeDelay": "number"
 *          }
 *      ]
 *   }
 * }
 * </pre>
 */
public class GetUserAlertingConfigsResponse extends JsonrpcResponseSuccess {

	private final UserAlertingSettings currentUserSettings;
	private final List<UserAlertingSettings> otherUsersSettings;

	public GetUserAlertingConfigsResponse(UUID id, UserAlertingSettings currentUserSettings, //
			List<UserAlertingSettings> otherUsersSettings) {
		super(id);
		this.currentUserSettings = currentUserSettings;
		this.otherUsersSettings = otherUsersSettings;
	}

	@Override
	public JsonObject getResult() {
		return JsonUtils.buildJsonObject() //
				.add("currentUserSettings", this.toJson(currentUserSettings)) //
				.add("otherUsersSettings", JsonUtils.generateJsonArray(this.otherUsersSettings, this::toJson)) //
				.build();
	}

	private JsonElement toJson(UserAlertingSettings setting) {
		return JsonUtils.buildJsonObject() //
				.addProperty("userLogin", setting.userLogin()) //
				.addProperty("offlineEdgeDelay", setting.edgeOfflineDelay()) //
				.addProperty("faultEdgeDelay", setting.edgeFaultDelay()) //
				.addProperty("warningEdgeDelay", setting.edgeWarningDelay()) //
				.build(); //
	}

}
