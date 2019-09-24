package io.openems.edge.core.host;

import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.utils.JsonUtils;

/**
 * JSON-RPC Response to "executeSystemCommand" Request.
 * 
 * <p>
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {
 *     "stdout": string[],
 *     "stderr": string[]
 *   }
 * }
 * </pre>
 */
public class ExecuteSystemCommandResponse extends JsonrpcResponseSuccess {

	private final String[] stdout;
	private final String[] stderr;

	public ExecuteSystemCommandResponse(UUID id, String[] stdout, String[] stderr) {
		super(id);
		this.stdout = stdout;
		this.stderr = stderr;
	}

	@Override
	public JsonObject getResult() {
		JsonArray stdout = new JsonArray();
		for (String line : this.stdout) {
			stdout.add(line);
		}
		JsonArray stderr = new JsonArray();
		for (String line : this.stderr) {
			stderr.add(line);
		}
		return JsonUtils.buildJsonObject() //
				.add("stdout", stdout) //
				.add("stderr", stderr) //
				.build();
	}

}
