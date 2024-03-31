package io.openems.edge.core.host.jsonrpc;

import java.util.UUID;

import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.edge.core.host.jsonrpc.ExecuteSystemCommandResponse.SystemCommandResponse;

/**
 * JSON-RPC Response to {@link ExecuteSystemRestartRequest}.
 *
 * <p>
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {
 *     "stdout": string[],
 *     "stderr": string[],
 *     "exitcode": number (exit code of application: 0 = successful; otherwise error)
 *   }
 * }
 * </pre>
 */
public class ExecuteSystemRestartResponse extends JsonrpcResponseSuccess {

	public final SystemCommandResponse scr;

	public ExecuteSystemRestartResponse(UUID id, SystemCommandResponse scr) {
		super(id);
		this.scr = scr;
	}

	@Override
	public JsonObject getResult() {
		return this.scr.toJsonObject();
	}

}
