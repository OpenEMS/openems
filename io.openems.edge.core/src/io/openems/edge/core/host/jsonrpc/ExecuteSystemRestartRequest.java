package io.openems.edge.core.host.jsonrpc;

import static io.openems.common.utils.EnumUtils.toEnum;
import static io.openems.common.utils.JsonUtils.getAsString;

import java.util.UUID;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Request to execute a system restart.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "executeSystemRestart",
 *   "params": {
 *   	"type": "SOFT" | "HARD"
 *   }
 * }
 * </pre>
 */
public class ExecuteSystemRestartRequest extends JsonrpcRequest {

	public static final String METHOD = "executeSystemRestart";

	public enum Type {
		/**
		 * SOFT: restart only the Java OpenEMS Edge process.
		 */
		SOFT,
		/**
		 * HARD: reboot the device.
		 */
		HARD;
	}

	/**
	 * Parses a generic {@link JsonrpcRequest} to a
	 * {@link ExecuteSystemRestartRequest}.
	 *
	 * @param r the {@link JsonrpcRequest}
	 * @return the {@link ExecuteSystemRestartRequest}
	 * @throws OpenemsNamedException on error
	 */
	public static ExecuteSystemRestartRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		var p = r.getParams();
		var type = toEnum(Type.class, getAsString(p, "type"));
		if (type == null) {
			throw new OpenemsException("Unknown type for " + p.toString());
		}
		return new ExecuteSystemRestartRequest(r.getId(), type);
	}

	public final Type type;

	public ExecuteSystemRestartRequest(Type type) {
		this(UUID.randomUUID(), type);
	}

	public ExecuteSystemRestartRequest(UUID id, Type type) {
		super(id, METHOD, JsonrpcRequest.DEFAULT_TIMEOUT_SECONDS);
		this.type = type;
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
				.addProperty("type", this.type.name()) //
				.build();
	}
}
