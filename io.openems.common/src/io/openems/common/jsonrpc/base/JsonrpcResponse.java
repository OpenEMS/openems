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

	public static JsonrpcResponse from(String json) throws OpenemsNamedException {
		return from(JsonUtils.parseToJsonObject(json));
	}

	public static JsonrpcResponse from(JsonObject j) throws OpenemsNamedException {
		UUID id = UUID.fromString(JsonUtils.getAsString(j, "id"));
		if (j.has("result")) {
			return new GenericJsonrpcResponseSuccess(id, JsonUtils.getAsJsonObject(j, "result"));
		} else if (j.has("error")) {
			return JsonrpcResponseError.from(j);
		}
		throw new OpenemsException("Unable to parse JsonrpcResponse from " + StringUtils.toShortString(j, 100));
	}

	private final UUID id;

	protected JsonrpcResponse(UUID id) {
		this.id = id;
	}

	public UUID getId() {
		return id;
	}

	@Override
	public JsonObject toJsonObject() {
		return JsonUtils.buildJsonObject(super.toJsonObject()) //
				.addProperty("id", this.getId().toString()) //
				.build();
	}

}