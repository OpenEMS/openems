package io.openems.common.jsonrpc.request;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Request to get the installed apps that are defined in
 * the backend metadata on the edge that send the request.
 *
 * <p>
 * NOTE: in order to get this request handled by the backend the request needs
 * to be wrapped in a {@link AppCenterRequest}.
 * 
 * <p>
 * This is used by Edge.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "getInstalledApps",
 *   "params": {
 *   }
 * }
 * </pre>
 */
public class AppCenterGetInstalledAppsRequest extends JsonrpcRequest {

	public static final String METHOD = "getInstalledApps";

	/**
	 * Creates a {@link AppCenterGetInstalledAppsRequest} from a
	 * {@link JsonrpcRequest}.
	 *
	 * @param r the {@link JsonrpcRequest}
	 * @return a {@link AppCenterGetInstalledAppsRequest}
	 * @throws OpenemsNamedException on error
	 */
	public static final AppCenterGetInstalledAppsRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		return new AppCenterGetInstalledAppsRequest(r);
	}

	private AppCenterGetInstalledAppsRequest(JsonrpcRequest request) {
		super(request, AppCenterGetInstalledAppsRequest.METHOD);
	}

	public AppCenterGetInstalledAppsRequest() {
		super(AppCenterGetInstalledAppsRequest.METHOD);
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
				.build();
	}

}
