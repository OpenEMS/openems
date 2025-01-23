package io.openems.edge.core.host.jsonrpc;

import java.util.Optional;
import java.util.UUID;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Request to execute a system command on OpenEMS Edge.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "executeSystemCommand",
 *   "params": {
 *   	"command": string,
 *   	"runInBackground"?: boolean = false, // run the command in background (true) or in foreground (false)
 *   	"timeoutSeconds"?: number = 5, // interrupt the command after ... seconds
 *   	"username"?: string,
 *   	"password"?: string,
 *   }
 * }
 * </pre>
 */
public class ExecuteSystemCommandRequest extends JsonrpcRequest {

	public static final String METHOD = "executeSystemCommand";
	public static final boolean DEFAULT_RUN_IN_BACKGROUND = false;
	public static final int DEFAULT_TIMEOUT_SECONDS = 5;

	/**
	 * Holds common parameters for a {@link SystemCommand}.
	 */
	public static record SystemCommand(String command, boolean runInBackground, int timeoutSeconds,
			Optional<String> username, Optional<String> password) {

		private JsonObject toJsonObject() {
			var result = JsonUtils.buildJsonObject() //
					.addProperty("command", this.command) //
					.addProperty("runInBackground", this.runInBackground) //
					.addProperty("timeoutSeconds", this.timeoutSeconds); //
			if (this.username.isPresent()) {
				result.addProperty("username", this.username.get()); //
			}
			if (this.password.isPresent()) {
				result.addProperty("password", this.password.get()); //
			}
			return result.build();
		}
	}

	/**
	 * Parses a generic {@link JsonrpcRequest} to a
	 * {@link ExecuteSystemCommandRequest}.
	 *
	 * @param r the {@link JsonrpcRequest}
	 * @return the {@link ExecuteSystemCommandRequest}
	 * @throws OpenemsNamedException on error
	 */
	public static ExecuteSystemCommandRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		var p = r.getParams();
		var command = JsonUtils.getAsString(p, "command");
		boolean runInBackground = JsonUtils.getAsOptionalBoolean(p, "runInBackground")
				.orElse(DEFAULT_RUN_IN_BACKGROUND);
		int timeoutSeconds = JsonUtils.getAsOptionalInt(p, "timeoutSeconds").orElse(DEFAULT_TIMEOUT_SECONDS);
		var username = JsonUtils.getAsOptionalString(p, "username");
		var password = JsonUtils.getAsOptionalString(p, "password");
		return new ExecuteSystemCommandRequest(r.getId(),
				new SystemCommand(command, runInBackground, timeoutSeconds, username, password));
	}

	/**
	 * Factory without Username + Password; run in background without timeout.
	 *
	 * @param command the command
	 * @return the {@link ExecuteSystemCommandRequest}
	 */
	public static ExecuteSystemCommandRequest runInBackgroundWithoutAuthentication(String command) {
		return new ExecuteSystemCommandRequest(UUID.randomUUID(),
				new SystemCommand(command, true, 0, Optional.empty(), Optional.empty()));
	}

	/**
	 * Factory without Username + Password.
	 *
	 * @param command         the command
	 * @param runInBackground run the command in background (true) or in foreground
	 *                        (false)
	 * @param timeoutSeconds  interrupt the command after ... seconds
	 * @return the {@link ExecuteSystemCommandRequest}
	 */
	public static ExecuteSystemCommandRequest withoutAuthentication(String command, boolean runInBackground,
			int timeoutSeconds) {
		return new ExecuteSystemCommandRequest(UUID.randomUUID(),
				new SystemCommand(command, runInBackground, timeoutSeconds, Optional.empty(), Optional.empty()));
	}

	public final SystemCommand systemCommand;

	public ExecuteSystemCommandRequest(String command, boolean runInBackground, int timeoutSeconds,
			Optional<String> username, Optional<String> password) {
		this(UUID.randomUUID(), new SystemCommand(command, runInBackground, timeoutSeconds, username, password));
	}

	public ExecuteSystemCommandRequest(UUID id, SystemCommand systemCommand) {
		super(id, METHOD, systemCommand.timeoutSeconds
				+ JsonrpcRequest.DEFAULT_TIMEOUT_SECONDS /* reuse timeoutSeconds with some buffer */);
		this.systemCommand = systemCommand;
	}

	@Override
	public JsonObject getParams() {
		return this.systemCommand.toJsonObject();
	}
}
