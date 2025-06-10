package io.openems.common.jsonrpc.request;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Request to unregister a registered key.
 *
 * <p>
 * NOTE: in order to get this request handled by the backend the request needs
 * to be wrapped in a {@link AppCenterRequest}.
 * 
 * <p>
 * This is used by UI.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "addUnregisterKeyHistory",
 *   "params": {
 *     "key": String,
 *     "appId": String
 *   }
 * }
 * </pre>
 */
public class AppCenterAddUnregisterKeyHistoryRequest extends JsonrpcRequest {

	public static final String METHOD = "addUnregisterKeyHistory";

	/**
	 * Creates a {@link AppCenterAddUnregisterKeyHistoryRequest} from a
	 * {@link JsonrpcRequest}.
	 *
	 * @param r the {@link JsonrpcRequest}
	 * @return a {@link AppCenterAddUnregisterKeyHistoryRequest}
	 * @throws OpenemsNamedException on error
	 */
	public static final AppCenterAddUnregisterKeyHistoryRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		var p = r.getParams();
		return new AppCenterAddUnregisterKeyHistoryRequest(r, //
				JsonUtils.getAsString(p, "key"), //
				JsonUtils.getAsOptionalString(p, "appId").orElse(null) //
		);
	}

	public final String key;
	public final String appId;

	private AppCenterAddUnregisterKeyHistoryRequest(JsonrpcRequest request, String key, String appId) {
		super(request, AppCenterAddUnregisterKeyHistoryRequest.METHOD);
		this.key = key;
		this.appId = appId;
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
				.addProperty("key", this.key) //
				.addPropertyIfNotNull("appId", this.appId) //
				.build();
	}

}
