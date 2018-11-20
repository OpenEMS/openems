package io.openems.common.jsonrpc.base;

import java.util.Optional;
import java.util.UUID;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;

public class JsonrpcResponseError extends JsonrpcResponse {

	public static JsonrpcResponseError from(String json) throws OpenemsException {
		return from(JsonUtils.parseToJsonObject(json));
	}

	public static JsonrpcResponseError from(JsonObject j) throws OpenemsException {
		UUID id = UUID.fromString(JsonUtils.getAsString(j, "id"));
		JsonObject error = JsonUtils.getAsJsonObject(j, "error");
		int code = JsonUtils.getAsInt(error, "code");
		String message = JsonUtils.getAsString(error, "message");
		if (error.has("data")) {
			return new JsonrpcResponseError(id, code, message, error.get("data"));
		} else {
			return new JsonrpcResponseError(id, code, message);
		}
	}

	/**
	 * A Number that indicates the error type that occurred.
	 */
	private final int code;
	/**
	 * A String providing a short description of the error.
	 */
	private final String message;
	/**
	 * A Primitive or Structured value that contains additional information about
	 * the error. This may be omitted.
	 */
	private final Optional<JsonElement> data;

	public JsonrpcResponseError(UUID id, int code, String message) {
		this(id, code, message, null);
	}

	public JsonrpcResponseError(UUID id, int code, String message, JsonElement data) {
		super(id);
		this.code = code;
		this.message = message;
		this.data = Optional.ofNullable(data);
	}

	@Override
	public JsonObject toJsonObject() {
		JsonObject error = JsonUtils.buildJsonObject() //
				.addProperty("code", this.code) //
				.addProperty("message", this.message) //
				.build();
		if (this.data.isPresent()) {
			error.add("data", this.data.get());
		}
		return JsonUtils.buildJsonObject(super.toJsonObject()) //
				.add("error", error) //
				.build();
	}

}