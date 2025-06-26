package io.openems.common.jsonrpc.response;

import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Response for getting all registed keys.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {
 *     "keys": [ { key: "XXXX-XXXX-XXXX-XXXX" }, ...]
 *   }
 * }
 * </pre>
 */
public class AppCenterGetRegisteredKeysResponse extends JsonrpcResponseSuccess {

	private final JsonArray keys;

	/**
	 * Creates a {@link AppCenterGetRegisteredKeysResponse} from a
	 * {@link JsonrpcResponseSuccess}.
	 *
	 * @param r the {@link JsonrpcResponseSuccess}
	 * @return a {@link AppCenterGetRegisteredKeysResponse}
	 */
	public static final AppCenterGetRegisteredKeysResponse from(JsonrpcResponseSuccess r) {
		final var result = r.getResult();
		return new AppCenterGetRegisteredKeysResponse(r.getId(), //
				JsonUtils.getAsOptionalJsonArray(result, "keys") //
						.orElse(new JsonArray()) //
		);
	}

	public AppCenterGetRegisteredKeysResponse(UUID id, JsonArray keys) {
		super(id);
		this.keys = keys;
	}

	@Override
	public JsonObject getResult() {
		return JsonUtils.buildJsonObject() //
				.add("keys", this.keys) //
				.build();
	}

}
