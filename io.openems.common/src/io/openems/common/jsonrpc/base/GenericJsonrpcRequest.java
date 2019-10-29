package io.openems.common.jsonrpc.base;

import java.util.UUID;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a generic JSON-RPC Request.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": string,
 *   "params": {}
 * }
 * </pre>
 * 
 * @see <a href="https://www.jsonrpc.org/specification#request_object">JSON-RPC
 *      specification</a>
 */
public class GenericJsonrpcRequest extends JsonrpcRequest {

	/**
	 * Parses the String to a {@link GenericJsonrpcRequest}.
	 * 
	 * @param j the String
	 * @return the {@link GenericJsonrpcRequest}
	 * @throws OpenemsNamedException on error
	 */
	public static GenericJsonrpcRequest from(String json) throws OpenemsNamedException {
		return from(JsonUtils.parseToJsonObject(json));
	}

	/**
	 * Parses the String to a {@link GenericJsonrpcRequest}. If the request UUID is
	 * missing, it is replaced by a random UUID.
	 * 
	 * @param j the String
	 * @return the {@link GenericJsonrpcRequest}
	 * @throws OpenemsNamedException on error
	 */
	public static GenericJsonrpcRequest fromIgnoreId(String json) throws OpenemsNamedException {
		return from(JsonUtils.parseToJsonObject(json));
	}

	/**
	 * Parses the {@link JsonObject} to a {@link GenericJsonrpcRequest}.
	 * 
	 * @param j the {@link JsonObject}
	 * @return the {@link GenericJsonrpcRequest}
	 * @throws OpenemsNamedException on error
	 */
	public static GenericJsonrpcRequest from(JsonObject j) throws OpenemsNamedException {
		UUID id = JsonUtils.getAsUUID(j, "id");
		String method = JsonUtils.getAsString(j, "method");
		JsonObject params = JsonUtils.getAsJsonObject(j, "params");
		return new GenericJsonrpcRequest(id, method, params);
	}

	/**
	 * Parses the {@link JsonObject} to a {@link GenericJsonrpcRequest}. If the
	 * request UUID is missing, it is replaced by a random UUID.
	 * 
	 * @param j the {@link JsonObject}
	 * @return the {@link GenericJsonrpcRequest}
	 * @throws OpenemsNamedException on error
	 */
	public static GenericJsonrpcRequest fromIgnoreId(JsonObject j) throws OpenemsNamedException {
		UUID id = JsonUtils.getAsOptionalUUID(j, "id").orElse(UUID.randomUUID());
		String method = JsonUtils.getAsString(j, "method");
		JsonObject params = JsonUtils.getAsJsonObject(j, "params");
		return new GenericJsonrpcRequest(id, method, params);
	}

	private final JsonObject params;

	public GenericJsonrpcRequest(UUID id, String method, JsonObject params) {
		super(id, method);
		this.params = params;
	}

	@Override
	public JsonObject getParams() {
		return this.params;
	}

}
