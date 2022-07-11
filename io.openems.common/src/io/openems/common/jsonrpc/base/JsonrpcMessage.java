package io.openems.common.jsonrpc.base;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;
import io.openems.common.utils.StringUtils;

/**
 * Represents a JSON-RPC Message.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   ...
 * }
 * </pre>
 *
 * @see <a href="https://www.jsonrpc.org/specification">JSON-RPC
 *      specification</a>
 */
public abstract class JsonrpcMessage {

	public static final String JSONRPC_VERSION = "2.0";

	/**
	 * Parses a JSON String to a {@link JsonrpcMessage}.
	 *
	 * @param json the JSON String
	 * @return the {@link JsonrpcMessage}
	 * @throws OpenemsNamedException on error
	 */
	public static JsonrpcMessage from(String json) throws OpenemsNamedException {
		return JsonrpcMessage.from(JsonUtils.parseToJsonObject(json));
	}

	/**
	 * Parses a {@link JsonObject} to a {@link JsonrpcMessage}.
	 *
	 * @param j the {@link JsonObject}
	 * @return the {@link JsonrpcMessage}
	 * @throws OpenemsNamedException on error
	 */
	public static JsonrpcMessage from(JsonObject j) throws OpenemsNamedException {
		if (j.has("method") && j.has("params")) {
			if (j.has("id")) {
				return GenericJsonrpcRequest.from(j);
			}
			return GenericJsonrpcNotification.from(j);

		}
		if (j.has("result")) {
			return JsonrpcResponseSuccess.from(j);

		}
		if (j.has("error")) {
			return JsonrpcResponseError.from(j);
		}
		throw new OpenemsException(
				"JsonrpcMessage is not a valid Request, Result or Notification: " + StringUtils.toShortString(j, 100));
	}

	/**
	 * Gets the {@link JsonObject} representation of this {@link JsonrpcMessage}.
	 *
	 * @return a {@link JsonObject}
	 */
	public JsonObject toJsonObject() {
		return JsonUtils.buildJsonObject() //
				.addProperty("jsonrpc", JsonrpcMessage.JSONRPC_VERSION) //
				.build();
	}

	/**
	 * Returns this JsonrpcMessage as a JSON String.
	 */
	@Override
	public String toString() {
		return this.toJsonObject().toString();
	}

}
