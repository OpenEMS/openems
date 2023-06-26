package io.openems.backend.common.jsonrpc.response;

import java.util.List;
import java.util.UUID;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.backend.common.metadata.AlertingSetting;
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
 *           userId: string,
 *           role: {@link Role},
 *           offlineAlertDelayTime: number,
 *           offlineAlertEnabled: boolean,
 *           sumStateAlertDelayTime: number,
 *           sumStateAlertEnabled: boolean,
 *           sumStateAlertLevel: number
 *          }
 *      ]
 *   }
 * }
 * </pre>
 */
public class GetUserAlertingConfigsResponse extends JsonrpcResponseSuccess {

    private final List<AlertingSetting> settings;

    public GetUserAlertingConfigsResponse(UUID id, List<AlertingSetting> settings) {
	super(id);
	this.settings = settings;
    }

    @Override
    public JsonObject getResult() {
	return JsonUtils.buildJsonObject() //
		.add("userSettings", JsonUtils.generateJsonArray(this.settings, this::toJson)) //
		.build();
    }

    private JsonElement toJson(AlertingSetting setting) {
	return JsonUtils.buildJsonObject() //
		.addProperty("userId", setting.getUserId()) //
		.add("role", setting.getUserRole().asJson()) //
		.addProperty("offlineAlertDelayTime", setting.getOfflineAlertDelayTime()) //
		.addProperty("sumStateAlertDelayTime", setting.getSumStateAlertDelayTime()) //
		.addProperty("sumStateAlertLevel", setting.getSumStateAlertLevel().getValue()) //
		.build(); //

    }

}
