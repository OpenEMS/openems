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

	public final static String METHOD = "systemLog";

	private final SystemLog line;

	public static SystemLogNotification from(JsonrpcNotification notification) throws OpenemsNamedException {
		JsonObject j = notification.getParams();
		SystemLog line = SystemLog.fromJsonObject(JsonUtils.getAsJsonObject(j, "line"));
		return new SystemLogNotification(line);
	}

	public static SystemLogNotification fromPaxLoggingEvent(PaxLoggingEvent event) {
		return new SystemLogNotification(SystemLog.fromPaxLoggingEvent(event));
	}

	public SystemLogNotification(SystemLog line) {
		super(METHOD);
		this.line = line;
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
				.add("line", this.line.toJson()) //
				.build();
	}

}
