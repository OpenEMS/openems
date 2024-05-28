package io.openems.backend.simulation.engine.possiblebatterycapacityextension;

import java.util.UUID;

import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;

/**
 * Represents a JSON-RPC Response for
 * {@link PossibleBatteryCapacityExtensionRequest}.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {
 *     "pdf": Base64
 *   }
 * }
 * </pre>
 */
public class PossibleBatteryCapacityExtensionResponse extends JsonrpcResponseSuccess {

	private final JsonObject result;

	public PossibleBatteryCapacityExtensionResponse(JsonObject result) {
		this(UUID.randomUUID(), result);
	}

	public PossibleBatteryCapacityExtensionResponse(UUID id, JsonObject result) {
		super(id);
		this.result = result;
	}

	@Override
	public JsonObject getResult() {
		return this.result;
	}

}
