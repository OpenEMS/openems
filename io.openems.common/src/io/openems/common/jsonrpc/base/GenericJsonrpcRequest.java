package io.openems.common.jsonrpc.base;

import java.util.Optional;
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
 *   "params": {},
 *   "timeout"?: number, defaults to 60 seconds; negative or zero to disable timeout
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
	 * @param json the String
	 * @return the {@link GenericJsonrpcRequest}
	 * @throws OpenemsNamedException on error
	 */
	public static GenericJsonrpcRequest from(String json) throws OpenemsNamedException {
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
		Optional<Integer> timeout = JsonUtils.getAsOptionalInt(j, "timeout");
		return new GenericJsonrpcRequest(id, method, params, timeout);
	}

	/**
	 * Parses the String to a {@link GenericJsonrpcRequest}. If the request UUID is
	 * missing, it is replaced by a random UUID.
	 * 
	 * @param json the String
	 * @return the {@link GenericJsonrpcRequest}
	 * @throws OpenemsNamedException on error
	 */
	public static GenericJsonrpcRequest fromIgnoreId(String json) throws OpenemsNamedException {
		return fromIgnoreId(JsonUtils.parseToJsonObject(json));
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
		UUID id = JsonUtils.getAsOptionalUUID(j, "id").orElse(new UUID(0L, 0L) /* dummy UUID */);
		String method = JsonUtils.getAsString(j, "method");
		JsonObject params = JsonUtils.getAsJsonObject(j, "params");
		Optional<Integer> timeout = JsonUtils.getAsOptionalInt(j, "timeout");
		return new GenericJsonrpcRequest(id, method, params, timeout);
	}

	private final JsonObject params;

	public GenericJsonrpcRequest(UUID id, String method, JsonObject params, int timeout) {
		super(id, method, timeout);
		this.params = params;
	}

	public GenericJsonrpcRequest(UUID id, String method, JsonObject params, Optional<Integer> timeout) {
		super(id, method, timeout);
		this.params = params;
	}

	@Override
	public JsonObject getParams() {
		return this.params;
	}

}
