package io.openems.common.jsonrpc.base;

import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.JsonUtils;

public class JsonrpcResponseError extends JsonrpcResponse {

	public static JsonrpcResponseError from(String json) throws OpenemsNamedException {
		return from(JsonUtils.parseToJsonObject(json));
	}

	public static JsonrpcResponseError from(JsonObject j) throws OpenemsNamedException {
		UUID id = UUID.fromString(JsonUtils.getAsString(j, "id"));
		JsonObject error = JsonUtils.getAsJsonObject(j, "error");
		int code = JsonUtils.getAsInt(error, "code");
		OpenemsError openemsError = OpenemsError.fromCode(code);
		if (openemsError == OpenemsError.GENERIC) {
			String message = JsonUtils.getAsString(error, "message");
			return new JsonrpcResponseError(id, message);
		} else {
			JsonArray params = JsonUtils.getAsJsonArray(error, "data");
			return new JsonrpcResponseError(id, openemsError, params);
		}
	}

	private final OpenemsError openemsError;
	private final JsonArray params;

	public JsonrpcResponseError(UUID id, OpenemsError openemsError, JsonArray params) {
		super(id);
		this.openemsError = openemsError;
		this.params = params;
	}

	/**
	 * Creates a GENERIC error.
	 * 
	 * @param id
	 * @param message
	 */
	public JsonrpcResponseError(UUID id, String message) {
		super(id);
		this.openemsError = OpenemsError.GENERIC;
		this.params = new JsonArray();
		params.add(message);
	}

	public JsonrpcResponseError(UUID id, OpenemsNamedException exception) {
		super(id);
		this.openemsError = exception.getError();
		this.params = (JsonArray) JsonUtils.getAsJsonElement(exception.getParams());
	}

	@Override
	public JsonObject toJsonObject() {
		Object[] params = new Object[this.params.size()];
		for (int i = 0; i < params.length; i++) {
			try {
				params[i] = JsonUtils.getAsBestType(this.params.get(i));
			} catch (OpenemsNamedException e) {
				e.printStackTrace();
			}
		}
		return JsonUtils.buildJsonObject(super.toJsonObject()) //
				.add("error", JsonUtils.buildJsonObject() //
						// A Number that indicates the error type that occurred.
						.addProperty("code", this.openemsError.getCode()) //
						// A String providing a short description of the error.
						.addProperty("message", this.openemsError.getMessage(params)) //
						// A Primitive or Structured value that contains additional information about
						// the error. This may be omitted.
						.add("data", this.params) //
						.build()) //
				.build();
	}

	public OpenemsError getOpenemsError() {
		return openemsError;
	}

	public JsonArray getParams() {
		return params;
	}

}