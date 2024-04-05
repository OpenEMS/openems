package io.openems.edge.core.host.jsonrpc;

import java.util.UUID;
import java.util.stream.Stream;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.core.host.jsonrpc.ExecuteSystemCommandRequest.SystemCommand;

/**
 * JSON-RPC Response to {@link ExecuteSystemCommandRequest}.
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
public class ExecuteSystemCommandResponse extends JsonrpcResponseSuccess {

	/**
	 * Holds common parameters for a response to a {@link SystemCommand}.
	 */
	public static record SystemCommandResponse(String[] stdout, String[] stderr, int exitcode) {

		/**
		 * Convert to {@link JsonObject}.
		 * 
		 * @return a {@link JsonObject}
		 */
		public JsonObject toJsonObject() {
			return JsonUtils.buildJsonObject() //
					.add("stdout", Stream.of(this.stdout) //
							.map(JsonPrimitive::new) //
							.collect(JsonUtils.toJsonArray()))
					.add("stderr", Stream.of(this.stderr) //
							.map(JsonPrimitive::new) //
							.collect(JsonUtils.toJsonArray()))
					.addProperty("exitcode", this.exitcode) //
					.build();
		}
	}

	public final SystemCommandResponse scr;

	public ExecuteSystemCommandResponse(UUID id, SystemCommandResponse scr) {
		super(id);
		this.scr = scr;
	}

	@Override
	public JsonObject getResult() {
		return this.scr.toJsonObject();
	}
}
