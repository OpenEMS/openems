package io.openems.common.jsonrpc.base;

import java.util.UUID;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;

public abstract class JsonrpcMessage {

	public final static String JSONRPC_VERSION = "2.0";

	public static JsonrpcMessage from(String json) throws OpenemsException {
		return from(JsonUtils.parseToJsonObject(json));
	}

	public static JsonrpcMessage from(JsonObject j) throws OpenemsException {
		if (j.has("method") && j.has("params")) {
			return GenericJsonrpcRequest.from(j);

		} else if (j.has("result")) {
			return GenericJsonrpcResponseSuccess.from(j);

		} else if (j.has("error")) {
			return GenericJsonrpcResponseError.from(j);
		}
		throw new OpenemsException("JsonrpcMessage is neither a Request nor a Result!");
	}

	private final UUID id;

	protected JsonrpcMessage(UUID id) {
		this.id = id;
	}

	public UUID getId() {
		return id;
	}

	public JsonObject toJsonObject() {
		return JsonUtils.buildJsonObject() //
				.addProperty("jsonrpc", JSONRPC_VERSION) //
				.addProperty("id", this.getId().toString()) //
				.build();
	}

	/**
	 * Returns this JsonrpcMessage as a JSON String
	 */
	@Override
	public String toString() {
		return this.toJsonObject().toString();
	}

}
