package io.openems.common.jsonrpc.base;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Response Error.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "error": {
 *     "code": {@link OpenemsError#getCode()},
 *     "message" string
 *     "data": any[]
 *   }
 * }
 * </pre>
 *
 * @see <a href="https://www.jsonrpc.org/specification#error_object">JSON-RPC
 *      specification</a>
 */
public class JsonrpcResponseError extends JsonrpcResponse {

	private static final Logger LOG = LoggerFactory.getLogger(JsonrpcResponseError.class);

	/**
	 * Parses a JSON String to a {@link JsonrpcResponseError}.
	 *
	 * @param json the JSON String
	 * @return the {@link JsonrpcResponseError}
	 * @throws OpenemsNamedException on error
	 */
	public static JsonrpcResponseError from(String json) throws OpenemsNamedException {
		return JsonrpcResponseError.from(JsonUtils.parseToJsonObject(json));
	}

	/**
	 * Parses a {@link JsonObject} to a {@link JsonrpcResponseError}.
	 *
	 * @param j the {@link JsonObject}
	 * @return the {@link JsonrpcResponseError}
	 * @throws OpenemsNamedException on error
	 */
	public static JsonrpcResponseError from(JsonObject j) throws OpenemsNamedException {
		var id = UUID.fromString(JsonUtils.getAsString(j, "id"));
		var error = JsonUtils.getAsJsonObject(j, "error");
		var code = JsonUtils.getAsInt(error, "code");
		OpenemsError openemsError;
		try {
			openemsError = OpenemsError.fromCode(code);
		} catch (OpenemsException e) {
			JsonrpcResponseError.LOG
					.warn("Falling back to Generic Error for JSON-RPC " + j.toString() + "; " + e.getMessage());
			openemsError = OpenemsError.GENERIC;
		}
		if (openemsError == OpenemsError.GENERIC) {
			var message = JsonUtils.getAsString(error, "message");
			return new JsonrpcResponseError(id, message);
		}
		var params = JsonUtils.getAsJsonArray(error, "data");
		return new JsonrpcResponseError(id, openemsError, params);
	}

	private final Logger log = LoggerFactory.getLogger(JsonrpcResponseError.class);

	private final OpenemsError openemsError;
	private final JsonArray params;

	public JsonrpcResponseError(UUID id, OpenemsError openemsError, JsonArray params) {
		super(id);
		this.openemsError = openemsError;
		this.params = params;
	}

	/**
	 * Creates a generic JSON-RPC Error response.
	 *
	 * @param id      the request ID
	 * @param message the error message
	 */
	public JsonrpcResponseError(UUID id, String message) {
		super(id);
		this.openemsError = OpenemsError.GENERIC;
		this.params = new JsonArray();
		this.params.add(message);
	}

	public JsonrpcResponseError(UUID id, OpenemsNamedException exception) {
		super(id);
		this.openemsError = exception.getError();
		this.params = (JsonArray) JsonUtils.getAsJsonElement(exception.getParams());
	}

	@Override
	public JsonObject toJsonObject() {
		var params = new Object[this.params.size()];
		for (var i = 0; i < params.length; i++) {
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
		return this.openemsError;
	}

	public JsonArray getParams() {
		return this.params;
	}

	/**
	 * Gets the error message parameters as Object array.
	 *
	 * @return the array of error message parameters
	 */
	public Object[] getParamsAsObjectArray() {
		try {
			return (Object[]) JsonUtils.getAsBestType(this.params);
		} catch (OpenemsNamedException e) {
			this.log.warn("Unable to convert JSON-RPC error params [" + this.params + "]: " + e.getMessage());
			return new Object[0];
		}
	}

}