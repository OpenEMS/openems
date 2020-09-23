package io.openems.common.jsonrpc.base;

import java.util.UUID;
import com.google.gson.JsonObject;

import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Request.
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
public abstract class JsonrpcRequest extends AbstractJsonrpcRequest {

	private final UUID id;

	public JsonrpcRequest(String method) {
		this(UUID.randomUUID(), method);
	}

	public JsonrpcRequest(UUID id, String method) {
		super(method);
		this.id = id;
	}

	public UUID getId() {
		return this.id;
	}

	@Override
	public JsonObject toJsonObject() {
		return JsonUtils.buildJsonObject(super.toJsonObject()) //
				.addProperty("id", this.getId().toString()) //
				.build();
	}
}