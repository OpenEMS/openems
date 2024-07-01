package io.openems.common.jsonrpc.base;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a generic JSON-RPC Notification.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "method": string,
 *   "params": {}
 * }
 * </pre>
 *
 * @see <a href="https://www.jsonrpc.org/specification#notification">JSON-RPC
 *      specification</a>
 */
public class GenericJsonrpcNotification extends JsonrpcNotification {

	/**
	 * Parses a JSON String to a {@link GenericJsonrpcNotification}.
	 *
	 * @param json the JSON String
	 * @return the {@link GenericJsonrpcNotification}
	 * @throws OpenemsNamedException on error
	 */
	public static GenericJsonrpcNotification from(String json) throws OpenemsNamedException {
		return GenericJsonrpcNotification.from(JsonUtils.parseToJsonObject(json));
	}

	/**
	 * Parses a {@link JsonObject} to a {@link GenericJsonrpcNotification}.
	 *
	 * @param j the {@link JsonObject}
	 * @return the {@link GenericJsonrpcNotification}
	 * @throws OpenemsNamedException on error
	 */
	public static GenericJsonrpcNotification from(JsonObject j) throws OpenemsNamedException {
		var method = JsonUtils.getAsString(j, "method");
		var params = JsonUtils.getAsJsonObject(j, "params");
		return new GenericJsonrpcNotification(method, params);
	}

	private final JsonObject params;

	public GenericJsonrpcNotification(String method, JsonObject params) {
		super(method);
		this.params = params;
	}

	@Override
	public JsonObject getParams() {
		return this.params;
	}

}
