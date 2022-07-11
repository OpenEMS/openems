package io.openems.common.jsonrpc.base;

import java.util.UUID;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;
import io.openems.common.utils.StringUtils;

/**
 * Represents a JSON-RPC Response.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   ...
 * }
 * </pre>
 *
 * @see <a href="https://www.jsonrpc.org/specification#response_object">JSON-RPC
 *      specification</a>
 */
public abstract class JsonrpcResponse extends JsonrpcMessage {

	/**
	 * Parses a JSON String to a {@link JsonrpcResponse}.
	 *
	 * @param json the JSON String
	 * @return the {@link JsonrpcResponse}
	 * @throws OpenemsNamedException on error
	 */
	public static JsonrpcResponse from(String json) throws OpenemsNamedException {
		return JsonrpcResponse.from(JsonUtils.parseToJsonObject(json));
	}

	/**
	 * Parses a {@link JsonObject} to a {@link JsonrpcResponse}.
	 *
	 * @param j the {@link JsonObject}
	 * @return the {@link JsonrpcResponse}
	 * @throws OpenemsNamedException on error
	 */
	public static JsonrpcResponse from(JsonObject j) throws OpenemsNamedException {
		var id = UUID.fromString(JsonUtils.getAsString(j, "id"));
		if (j.has("result")) {
			return new GenericJsonrpcResponseSuccess(id, JsonUtils.getAsJsonObject(j, "result"));
		}
		if (j.has("error")) {
			return JsonrpcResponseError.from(j);
		}
		throw new OpenemsException("Unable to parse JsonrpcResponse from " + StringUtils.toShortString(j, 100));
	}

	private final UUID id;

	protected JsonrpcResponse(UUID id) {
		this.id = id;
	}

	public UUID getId() {
		return this.id;
	}

	@Override
	public JsonObject toJsonObject() {
		return JsonUtils.buildJsonObject(super.toJsonObject()) //
				.addProperty("id", this.getId().toString()) //
				.build();
	}

}