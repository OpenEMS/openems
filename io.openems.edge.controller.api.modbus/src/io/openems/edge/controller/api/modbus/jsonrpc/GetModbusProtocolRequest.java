package io.openems.edge.controller.api.modbus.jsonrpc;

import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.base.JsonrpcRequest;

/**
 * Wraps a JSON-RPC Request to query the Modbus Protocol from Modbus/TCP
 * Api-Controller.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "getModbusProtocol",
 *   "params": {}
 * }
 * </pre>
 */
public class GetModbusProtocolRequest extends JsonrpcRequest {

	public static final String METHOD = "getModbusProtocol";

	public GetModbusProtocolRequest() {
		super(METHOD);
	}

	private GetModbusProtocolRequest(JsonrpcRequest request) {
		super(request, METHOD);
	}

	@Override
	public JsonObject getParams() {
		return new JsonObject();
	}

}
