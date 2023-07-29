package io.openems.backend.uiwebsocket.jsonrpc.notification;

import java.lang.System.Logger.Level;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.utils.JsonUtils;

/**
 * Log message.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": UUID,
 *   "method": "logMessage",
 *   "params": {
 *     "level": string,
 *     "msg": string
 *   }
 * }
 * </pre>
 */
public class LogMessageNotification extends JsonrpcNotification {

	public static final String METHOD = "logMessage";

	/**
	 * The log level as {@link Level}.
	 */
	public final Level level;

	/**
	 * The log message.
	 */
	public final String msg;

	/**
	 * Create {@link LogMessageNotification} from a template
	 * {@link JsonrpcNotification}.
	 *
	 * @param notification the template {@link JsonrpcNotification}
	 * @return Created {@link LogMessageNotification}
	 * @throws OpenemsNamedException on parse error
	 */
	public static LogMessageNotification from(JsonrpcNotification notification) throws OpenemsNamedException {
		var params = notification.getParams();
		var level = Level.valueOf(JsonUtils.getAsString(params, "level").toUpperCase());
		var msg = JsonUtils.getAsString(params, "msg");
		return new LogMessageNotification(notification, level, msg);
	}

	private LogMessageNotification(JsonrpcNotification notification, Level level, String msg)
			throws OpenemsNamedException {
		super(LogMessageNotification.METHOD);
		this.level = level;
		this.msg = msg;
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
				.addProperty("level", this.level.getName()) //
				.addProperty("msg", this.msg) //
				.build();
	}

}
