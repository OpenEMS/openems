package io.openems.common.jsonrpc.notification;

import org.ops4j.pax.logging.spi.PaxLoggingEvent;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.types.SystemLog;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Notification for sending the current system log.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "method": "systemLog",
 *   "params": {
 *     "line": {@link SystemLog#toJson()}
 *   }
 * }
 * </pre>
 */
public class SystemLogNotification extends JsonrpcNotification {

	public static final String METHOD = "systemLog";

	private final SystemLog line;

	/**
	 * Parses a {@link JsonrpcNotification} to a {@link SystemLogNotification}.
	 *
	 * @param n the {@link JsonrpcNotification}
	 * @return the {@link SystemLogNotification}
	 * @throws OpenemsNamedException on error
	 */
	public static SystemLogNotification from(JsonrpcNotification n) throws OpenemsNamedException {
		var j = n.getParams();
		var line = SystemLog.fromJsonObject(JsonUtils.getAsJsonObject(j, "line"));
		return new SystemLogNotification(line);
	}

	/**
	 * Creates a {@link SystemLogNotification} from a {@link PaxLoggingEvent}.
	 *
	 * @param event the {@link PaxLoggingEvent}
	 * @return the {@link SystemLogNotification}
	 */
	public static SystemLogNotification fromPaxLoggingEvent(PaxLoggingEvent event) {
		return new SystemLogNotification(SystemLog.fromPaxLoggingEvent(event));
	}

	public SystemLogNotification(SystemLog line) {
		super(SystemLogNotification.METHOD);
		this.line = line;
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
				.add("line", this.line.toJson()) //
				.build();
	}

}
