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

	private final static Logger LOG = LoggerFactory.getLogger(JsonrpcResponseError.class);

	public static JsonrpcResponseError from(String json) throws OpenemsNamedException {
		return from(JsonUtils.parseToJsonObject(json));
	}

	public static JsonrpcResponseError from(JsonObject j) throws OpenemsNamedException {
		UUID id = UUID.fromString(JsonUtils.getAsString(j, "id"));
		JsonObject error = JsonUtils.getAsJsonObject(j, "error");
		int code = JsonUtils.getAsInt(error, "code");
		OpenemsError openemsError;
		try {
			openemsError = OpenemsError.fromCode(code);
		} catch (OpenemsException e) {
			LOG.warn("Falling back to Generic Error for JSON-RPC " + j.toString() + "; " + e.getMessage());
			openemsError = OpenemsError.GENERIC;
		}
		if (openemsError == OpenemsError.GENERIC) {
			String message = JsonUtils.getAsString(error, "message");
			return new JsonrpcResponseError(id, message);
		} else {
			JsonArray params = JsonUtils.getAsJsonArray(error, "data");
			return new JsonrpcResponseError(id, openemsError, params);
		}
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

	public Object[] getParamsAsObjectArray() {
		try {
			return (Object[]) JsonUtils.getAsBestType(this.params);
		} catch (OpenemsNamedException e) {
			this.log.warn("Unable to convert JSON-RPC error params [" + this.params + "]: " + e.getMessage());
			return new Object[0];
		}
	}

}