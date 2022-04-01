package io.openems.edge.core.host.jsonrpc;

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
 *     "stderr": string[],
 *     "exitcode": number (exit code of application: 0 = successful; otherwise error)
 *   }
 * }
 * </pre>
 */
public class ExecuteSystemCommandResponse extends JsonrpcResponseSuccess {

	private final String[] stdout;
	private final String[] stderr;
	private final int exitcode;

	public ExecuteSystemCommandResponse(UUID id, String[] stdout, String[] stderr, int exitcode) {
		super(id);
		this.stdout = stdout;
		this.stderr = stderr;
		this.exitcode = exitcode;
	}

	@Override
	public JsonObject getResult() {
		var stdout = new JsonArray();
		for (String line : this.stdout) {
			stdout.add(line);
		}
		var stderr = new JsonArray();
		for (String line : this.stderr) {
			stderr.add(line);
		}
		return JsonUtils.buildJsonObject() //
				.add("stdout", stdout) //
				.add("stderr", stderr) //
				.addProperty("exitcode", this.exitcode) //
				.build();
	}

	public String[] getStdout() {
		return this.stdout;
	}

	public String[] getStderr() {
		return this.stderr;
	}

	public int getExitCode() {
		return this.exitcode;
	}

}
