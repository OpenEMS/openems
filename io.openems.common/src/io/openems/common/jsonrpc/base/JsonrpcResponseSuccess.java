package io.openems.common.jsonrpc.base;

import java.util.UUID;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Success Response.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {}
 * }
 * </pre>
 *
 * @see <a href="https://www.jsonrpc.org/specification#response_object">JSON-RPC
 *      specification</a>
 */
public abstract class JsonrpcResponseSuccess extends JsonrpcResponse {

	/**
	 * Parses the JSON-Object to a JSON-RPC Success Response.
	 *
	 * @param j the JSON-Object
	 * @return the JSON-RPC Success Response
	 * @throws OpenemsNamedException if it was not a Success Response
	 */
	public static JsonrpcResponseSuccess from(JsonObject j) throws OpenemsNamedException {
		var response = JsonrpcResponse.from(j);
		if (!(response instanceof JsonrpcResponseSuccess)) {
			throw OpenemsError.GENERIC.exception("Expected a JSON-RPC Success Response");
		}
		return (JsonrpcResponseSuccess) response;
	}

	public JsonrpcResponseSuccess(UUID id) {
		super(id);
	}

	@Override
	public JsonObject toJsonObject() {
		return JsonUtils.buildJsonObject(super.toJsonObject()) //
				.add("result", this.getResult()) //
				.build();
	}

	/**
	 * Gets the result of this {@link JsonrpcResponseSuccess}.
	 *
	 * @return a JsonObject with the 'result' property of the response
	 */
	public abstract JsonObject getResult();

}