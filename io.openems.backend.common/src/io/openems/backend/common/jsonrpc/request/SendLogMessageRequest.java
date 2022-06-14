package io.openems.backend.common.jsonrpc.request;

import java.lang.System.Logger.Level;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.utils.JsonUtils;

/**
 * Log message.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": UUID,
 *   "method": "sendLogMessage",
 *   "params": {
 *     "level": string,
 *     "msg": string
 *   }
 * }
 * </pre>
 */
public class SendLogMessageRequest extends JsonrpcRequest {

	public static final String METHOD = "sendLogMessage";

	private final Level level;
	private final String msg;

	/**
	 * Create {@link SendLogMessageRequest} from a template {@link JsonrpcRequest}.
	 *
	 * @param request the template {@link JsonrpcRequest}
	 * @return Created {@link SendLogMessageRequest}
	 * @throws OpenemsNamedException on parse error
	 */
	public static SendLogMessageRequest from(JsonrpcRequest request) throws OpenemsNamedException {
		var params = request.getParams();
		return new SendLogMessageRequest(request, params);
	}

	private SendLogMessageRequest(JsonrpcRequest request, JsonObject params) throws OpenemsNamedException {
		super(request, SendLogMessageRequest.METHOD);

		String jsonLevel = JsonUtils.getAsString(params, "level");
		this.level = Level.valueOf(jsonLevel.toUpperCase());
		this.msg = JsonUtils.getAsString(params, "msg");
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
				.addProperty("level", this.level.getName()) //
				.addProperty("msg", this.msg) //
				.build();
	}

	/**
	 * Gets the log level as {@link Level}.
	 * 
	 * @return the {@link Level}
	 */
	public Level getLevel() {
		return this.level;
	}

	/**
	 * Gets the message to log.
	 * 
	 * @return The message.
	 */
	public String getMsg() {
		return this.msg;
	}

}
