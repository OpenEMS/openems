package io.openems.common.jsonrpc.request;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Request to get all possible apps to install with a key.
 *
 * <p>
 * NOTE: in order to get this request handled by the backend the request needs
 * to be wrapped in a {@link AppCenterRequest}.
 * 
 * <p>
 * This is used by Edge and UI.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "getPossibleApps",
 *   "params": {
 *     "key": String
 *   }
 * }
 * </pre>
 */
public class AppCenterGetPossibleAppsRequest extends JsonrpcRequest {

	public static final String METHOD = "getPossibleApps";

	/**
	 * Creates a {@link AppCenterGetPossibleAppsRequest} from a
	 * {@link JsonrpcRequest}.
	 *
	 * @param r the {@link JsonrpcRequest}
	 * @return a {@link AppCenterGetPossibleAppsRequest}
	 * @throws OpenemsNamedException on error
	 */
	public static final AppCenterGetPossibleAppsRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		final var p = r.getParams();
		return new AppCenterGetPossibleAppsRequest(r, //
				JsonUtils.getAsString(p, "key") //
		);
	}

	public final String key;

	private AppCenterGetPossibleAppsRequest(JsonrpcRequest request, String key) {
		super(request, AppCenterGetPossibleAppsRequest.METHOD);
		this.key = key;
	}

	public AppCenterGetPossibleAppsRequest(String key) {
		super(AppCenterGetPossibleAppsRequest.METHOD);
		this.key = key;
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
				.addProperty("key", this.key) //
				.build();
	}

}
