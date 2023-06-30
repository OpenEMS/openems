package io.openems.common.jsonrpc.response;

import java.util.Base64;
import java.util.UUID;

import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Response for a Base64-encoded payload.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {
 *     "payload": Base64-String
 *   }
 * }
 * </pre>
 */
public class Base64PayloadResponse extends JsonrpcResponseSuccess {

	private final String payload;

	public Base64PayloadResponse(UUID id, byte[] payload) {
		super(id);
		this.payload = Base64.getEncoder().encodeToString(payload);
	}

	public String getPayload() {
		return this.payload;
	}

	@Override
	public JsonObject getResult() {
		return JsonUtils.buildJsonObject() //
				.addProperty("payload", this.payload) //
				.build();
	}

}
