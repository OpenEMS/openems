
package io.openems.edge.core.host.jsonrpc;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.utils.JsonUtils;

/**
 * Executes a System Update.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "executeSystemUpdate",
 *   "params": {
 *     "isDebug": boolean
 *   }
 * }
 * </pre>
 */
public class ExecuteSystemUpdateRequest extends JsonrpcRequest {

	public static final String METHOD = "executeSystemUpdate";

	/**
	 * Parses a generic {@link JsonrpcRequest} to a
	 * {@link ExecuteSystemUpdateRequest}.
	 *
	 * @param r the {@link JsonrpcRequest}
	 * @return the {@link ExecuteSystemUpdateRequest}
	 * @throws OpenemsNamedException on error
	 */
	public static ExecuteSystemUpdateRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		var p = r.getParams();
		var isDebug = JsonUtils.getAsBoolean(p, "isDebug");
		return new ExecuteSystemUpdateRequest(r, isDebug);
	}

	private final boolean isDebug;

	public ExecuteSystemUpdateRequest(boolean isDebug) {
		super(METHOD);
		this.isDebug = isDebug;
	}

	private ExecuteSystemUpdateRequest(JsonrpcRequest request, boolean isDebug) {
		super(request, METHOD);
		this.isDebug = isDebug;
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
				.addProperty("isDebug", this.isDebug) //
				.build();
	}

	public boolean isDebug() {
		return this.isDebug;
	}

}
