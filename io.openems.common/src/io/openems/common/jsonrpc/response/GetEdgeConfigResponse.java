package io.openems.common.jsonrpc.response;

import java.util.UUID;

import com.google.gson.JsonElement;

import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.types.EdgeConfig;

/**
 * Represents a JSON-RPC Response for 'getEdgeConfig'.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {
 *     EdgeConfig
 *   }
 * }
 * </pre>
 */
public class GetEdgeConfigResponse extends JsonrpcResponseSuccess {

	private final EdgeConfig config;

	public GetEdgeConfigResponse(EdgeConfig config) {
		this(UUID.randomUUID(), config);
	}

	public GetEdgeConfigResponse(UUID id, EdgeConfig config) {
		super(id);
		this.config = config;
	}

	@Override
	public JsonElement getResult() {
		return this.config.toJson();
	}

}
