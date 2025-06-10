package io.openems.common.jsonrpc.request;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.GenericJsonrpcRequest;
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
 *   "method": "installAppWithSuppliedKey",
 *   "params": {
 *     "installRequest": {@link JsonObject}
 *   }
 * }
 * </pre>
 */
public class AppCenterInstallAppWithSuppliedKeyRequest extends JsonrpcRequest {

	public static final String METHOD = "installAppWithSuppliedKey";

	/**
	 * Creates a {@link AppCenterInstallAppWithSuppliedKeyRequest} from a
	 * {@link JsonrpcRequest}.
	 *
	 * @param r the {@link JsonrpcRequest}
	 * @return a {@link AppCenterInstallAppWithSuppliedKeyRequest}
	 * @throws OpenemsNamedException on error
	 */
	public static final AppCenterInstallAppWithSuppliedKeyRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		final var p = r.getParams();
		return new AppCenterInstallAppWithSuppliedKeyRequest(r, //
				GenericJsonrpcRequest.from(JsonUtils.getAsJsonObject(p, "installRequest")) //
		);
	}

	public final JsonrpcRequest installRequest;

	private AppCenterInstallAppWithSuppliedKeyRequest(JsonrpcRequest request, JsonrpcRequest installRequest) {
		super(request, AppCenterInstallAppWithSuppliedKeyRequest.METHOD);
		this.installRequest = installRequest;
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
				.add("installRequest", this.installRequest.toJsonObject()) //
				.build();
	}

}
