package io.openems.backend.common.jsonrpc.response;

import java.util.List;
import java.util.UUID;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.backend.common.alerting.UserAlertingSettings;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.session.Role;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Response for 'getAlertingConfig'.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {
 *      userSettings: [
 *          {
 *           userLogin: string,
 *           role: {@link Role},
 *           offlineEdgeDelay: number,
 *           faultEdgeDelay: number,
 *           warningEdgeDelay: number
 *          }
 *      ]
 *   }
 * }
 * </pre>
 */
public class GetUserAlertingConfigsResponse extends JsonrpcResponseSuccess {

	private final List<UserAlertingSettings> settings;

	public GetUserAlertingConfigsResponse(UUID id, List<UserAlertingSettings> settings) {
		super(id);
		this.settings = settings;
	}

	@Override
	public JsonObject getResult() {
		return JsonUtils.buildJsonObject() //
				.add("userSettings", JsonUtils.generateJsonArray(this.settings, this::toJson)) //
				.build();
	}

	private JsonElement toJson(UserAlertingSettings setting) {
		return JsonUtils.buildJsonObject() //
				.addProperty("userLogin", setting.userLogin()) //
				.add("role", setting.userRole().asJson()) //
				.addProperty("offlineEdgeDelay", setting.edgeOfflineDelay()) //
				.addProperty("faultEdgeDelay", setting.edgeFaultDelay()) //
				.addProperty("warningEdgeDelay", setting.edgeWarningDelay()) //
				.build(); //
	}

}
