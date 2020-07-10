package io.openems.edge.simulator.app;

import java.util.UUID;

import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;

/**
 * Represents a JSON-RPC Response for 'executeSimulation'.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {   
 *   }
 * }
 * </pre>
 */
public class ExecuteSimulationResponse extends JsonrpcResponseSuccess {

	public ExecuteSimulationResponse() {
		this(UUID.randomUUID());
	}

	public ExecuteSimulationResponse(UUID id) {
		super(id);
	}

	@Override
	public JsonObject getResult() {
		return new JsonObject();
	}

}
