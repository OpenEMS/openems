package io.openems.edge.controller.api.modbus;

import java.util.UUID;

import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.base.JsonrpcRequest;

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
