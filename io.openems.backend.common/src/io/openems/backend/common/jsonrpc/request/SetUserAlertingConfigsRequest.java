package io.openems.backend.common.jsonrpc.request;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.backend.common.metadata.AlertingSetting;
import io.openems.common.channel.Level;
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
 *           userId: string,
 *           offlineAlertDelayTime: number,
 *           offlineAlertEnabled: boolean,
 *           sumStateAlertDelayTime: number,
 *           sumStateAlertEnabled: boolean,
 *           sumStateAlertLevel: number
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
    private final List<AlertingSetting> userSettings = new ArrayList<>();

    private SetUserAlertingConfigsRequest(JsonrpcRequest request) throws OpenemsNamedException {
	super(request, SetUserAlertingConfigsRequest.METHOD);
	var params = request.getParams();

	this.edgeId = JsonUtils.getAsString(params, "edgeId");
	JsonUtils.getAsJsonArray(params, "userSettings").forEach(user -> {
	    var userJsonObject = user.getAsJsonObject();
	    try {
		var userId = JsonUtils.getAsString(userJsonObject, "userId");
		var offlineAlertDelayTime = JsonUtils.getAsInt(userJsonObject, "offlineAlertDelayTime");
		var sumStateAlertDelayTime = JsonUtils.getAsInt(userJsonObject, "sumStateAlertDelayTime");
		var sumStateAlertLevel = JsonUtils.getAsInt(userJsonObject, "sumStateAlertLevel");
		var sumLevel = Level.fromValue(sumStateAlertLevel).get();
		this.userSettings.add(new AlertingSetting(0, userId, null, null,
			offlineAlertDelayTime, null, sumStateAlertDelayTime, sumLevel));
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
    public List<AlertingSetting> getUserSettings() {
	return this.userSettings;
    }

    @Override
    public JsonObject getParams() {
	return JsonUtils.buildJsonObject() //
		.addProperty("edgeId", this.edgeId) //
		.add("userSettings", JsonUtils.generateJsonArray(this.userSettings, this::toJson)) //
		.build();
    }

    private JsonElement toJson(AlertingSetting setting) {
	return JsonUtils.buildJsonObject() //
		.addProperty("userId", setting.getUserId()) //
		.addProperty("offlineAlertDelayTime", setting.getOfflineAlertDelayTime()) //
		.addProperty("sumStateAlertDelayTime", setting.getSumStateAlertDelayTime()) //
		.addProperty("sumStateAlertLevel", setting.getSumStateAlertLevel().getValue()) //
		.build();
    }

}
