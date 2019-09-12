package io.openems.edge.core.host;

import java.util.Optional;
import java.util.UUID;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.utils.JsonUtils;
import io.openems.common.utils.JsonUtils.JsonObjectBuilder;

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

	public static ExecuteSystemCommandRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		JsonObject p = r.getParams();
		String command = JsonUtils.getAsString(p, "command");
		boolean runInBackground = JsonUtils.getAsOptionalBoolean(p, "runInBackground")
				.orElse(DEFAULT_RUN_IN_BACKGROUND);
		int timeoutSeconds = JsonUtils.getAsOptionalInt(p, "timeoutSeconds").orElse(DEFAULT_TIMEOUT_SECONDS);
		Optional<String> username = JsonUtils.getAsOptionalString(p, "username");
		Optional<String> password = JsonUtils.getAsOptionalString(p, "password");
		return new ExecuteSystemCommandRequest(r.getId(), command, runInBackground, timeoutSeconds, username, password);
	}

	private final String command;
	private final boolean runInBackground;
	private final int timeoutSeconds;
	private final Optional<String> username;
	private final Optional<String> password;

	/**
	 * Factory without Username + Password; run in background without timeout.
	 * 
	 * @param id      the Request-ID
	 * @param command the command
	 */
	public static ExecuteSystemCommandRequest runInBackgroundWithoutAuthentication(String command) {
		return new ExecuteSystemCommandRequest(UUID.randomUUID(), command, true, 0, Optional.empty(), Optional.empty());
	}

	/**
	 * Factory without Username + Password.
	 * 
	 * @param command         the command
	 * @param runInBackground run the command in background (true) or in foreground
	 *                        (false)
	 * @param timeoutSeconds  interrupt the command after ... seconds
	 */
	public static ExecuteSystemCommandRequest withoutAuthentication(String command, boolean runInBackground,
			int timeoutSeconds) {
		return new ExecuteSystemCommandRequest(UUID.randomUUID(), command, runInBackground, timeoutSeconds, Optional.empty(),
				Optional.empty());
	}

	public ExecuteSystemCommandRequest(String command, boolean runInBackground, int timeoutSeconds, Optional<String> username,
			Optional<String> password) {
		this(UUID.randomUUID(), command, runInBackground, timeoutSeconds, username, password);
	}

	public ExecuteSystemCommandRequest(UUID id, String command, boolean runInBackground, int timeoutSeconds,
			Optional<String> username, Optional<String> password) {
		super(id, METHOD);
		this.command = command;
		this.runInBackground = runInBackground;
		this.timeoutSeconds = timeoutSeconds;
		this.username = username;
		this.password = password;
	}

	@Override
	public JsonObject getParams() {
		JsonObjectBuilder result = JsonUtils.buildJsonObject() //
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

	public String getCommand() {
		return this.command;
	}

	public boolean isRunInBackground() {
		return this.runInBackground;
	}

	public int getTimeoutSeconds() {
		return this.timeoutSeconds;
	}

	public Optional<String> getUsername() {
		return this.username;
	}

	public Optional<String> getPassword() {
		return this.password;
	}

}
