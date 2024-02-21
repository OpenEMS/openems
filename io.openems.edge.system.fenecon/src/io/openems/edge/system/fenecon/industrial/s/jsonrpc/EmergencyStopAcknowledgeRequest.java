package io.openems.edge.system.fenecon.industrial.s.jsonrpc;

import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.base.JsonrpcRequest;

/**
 * Wraps a JSON-RPC Request to get emergency stop acknowledge.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "emergencyStopAcknowledge",
 *   "params": {}
 * }
 * </pre>
 */
public class EmergencyStopAcknowledgeRequest extends JsonrpcRequest {

	public static final String METHOD = "emergencyStopAcknowledge";

	public EmergencyStopAcknowledgeRequest() {
		super(METHOD);
	}

	private EmergencyStopAcknowledgeRequest(JsonrpcRequest request) {
		super(request, METHOD);
	}

	@Override
	public JsonObject getParams() {
		return new JsonObject();
	}
}
