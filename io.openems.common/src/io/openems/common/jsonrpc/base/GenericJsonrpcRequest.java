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
		return GenericJsonrpcRequest.from(JsonUtils.parseToJsonObject(json));
	}

	/**
	 * Parses the {@link JsonObject} to a {@link GenericJsonrpcRequest}.
	 *
	 * @param j the {@link JsonObject}
	 * @return the {@link GenericJsonrpcRequest}
	 * @throws OpenemsNamedException on error
	 */
	public static GenericJsonrpcRequest from(JsonObject j) throws OpenemsNamedException {
		var id = JsonUtils.getAsUUID(j, "id");
		var method = JsonUtils.getAsString(j, "method");
		var params = JsonUtils.getAsJsonObject(j, "params");
		var timeoutOpt = JsonUtils.getAsOptionalInt(j, "timeout");
		return new GenericJsonrpcRequest(id, method, params, timeoutOpt);
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
		return GenericJsonrpcRequest.fromIgnoreId(JsonUtils.parseToJsonObject(json));
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
		var id = JsonUtils.getAsOptionalUUID(j, "id").orElse(new UUID(0L, 0L) /* dummy UUID */);
		var method = JsonUtils.getAsString(j, "method");
		var params = JsonUtils.getAsJsonObject(j, "params");
		var timeoutOpt = JsonUtils.getAsOptionalInt(j, "timeout");
		return new GenericJsonrpcRequest(id, method, params, timeoutOpt);
	}

	private final JsonObject params;

	public GenericJsonrpcRequest(UUID id, String method, JsonObject params, int timeout) {
		super(id, method, timeout);
		this.params = params;
	}

	public GenericJsonrpcRequest(UUID id, String method, JsonObject params, Optional<Integer> timeoutOpt) {
		super(id, method, timeoutOpt);
		this.params = params;
	}

	public GenericJsonrpcRequest(String method, JsonObject params) {
		super(method);
		this.params = params;
	}

	@Override
	public JsonObject getParams() {
		return this.params;
	}

}
