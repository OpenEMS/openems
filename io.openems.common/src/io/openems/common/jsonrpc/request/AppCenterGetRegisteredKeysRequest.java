package io.openems.common.jsonrpc.request;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Request to get all registered keys.
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
 *   "method": "getRegisteredKeys",
 *   "params": {
 *     "appId": String
 *   }
 * }
 * </pre>
 */
public class AppCenterGetRegisteredKeysRequest extends JsonrpcRequest {

	public static final String METHOD = "getRegisteredKeys";

	/**
	 * Creates a {@link AppCenterGetRegisteredKeysRequest} from a
	 * {@link JsonrpcRequest}.
	 *
	 * @param r the {@link JsonrpcRequest}
	 * @return a {@link AppCenterGetRegisteredKeysRequest}
	 * @throws OpenemsNamedException on error
	 */
	public static final AppCenterGetRegisteredKeysRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		final var p = r.getParams();
		return new AppCenterGetRegisteredKeysRequest(r, //
				JsonUtils.getAsOptionalString(p, "appId").orElse(null) //
		);
	}

	public final String appId;

	private AppCenterGetRegisteredKeysRequest(JsonrpcRequest request, String appId) {
		super(request, AppCenterGetRegisteredKeysRequest.METHOD);
		this.appId = appId;
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
				.addPropertyIfNotNull("appId", this.appId) //
				.build();
	}

}
