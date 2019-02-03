package io.openems.edge.controller.api.modbus;

import java.util.UUID;

import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.base.JsonrpcRequest;

/**
 * Wraps a JSON-RPC Request to query the Modbus Protocol from Modbus/TCP
 * Api-Controller
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

	public final static String METHOD = "getModbusProtocol";

	public GetModbusProtocolRequest() {
		this(UUID.randomUUID());
	}

	public GetModbusProtocolRequest(UUID id) {
		super(id, METHOD);
	}

	@Override
	public JsonObject getParams() {
		return new JsonObject();
	}

}
