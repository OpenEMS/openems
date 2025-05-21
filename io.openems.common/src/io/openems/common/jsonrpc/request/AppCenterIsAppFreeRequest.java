package io.openems.common.jsonrpc.request;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Request to get if a app is free and can be installed
 * with a {@link AppCenterInstallAppWithSuppliedKeyRequest}.
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
 *   "method": "isAppFree",
 *   "params": {
 *     "appId": String
 *   }
 * }
 * </pre>
 */
public class AppCenterIsAppFreeRequest extends JsonrpcRequest {

	public static final String METHOD = "isAppFree";

	/**
	 * Creates a {@link AppCenterIsAppFreeRequest} from a {@link JsonrpcRequest}.
	 *
	 * @param r the {@link JsonrpcRequest}
	 * @return a {@link AppCenterIsAppFreeRequest}
	 * @throws OpenemsNamedException on error
	 */
	public static final AppCenterIsAppFreeRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		final var p = r.getParams();
		return new AppCenterIsAppFreeRequest(r, //
				JsonUtils.getAsString(p, "appId") //
		);
	}

	public final String appId;

	private AppCenterIsAppFreeRequest(JsonrpcRequest request, String appId) {
		super(request, AppCenterIsAppFreeRequest.METHOD);
		this.appId = appId;
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
				.addProperty("appId", this.appId) //
				.build();
	}

}
