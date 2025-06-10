package io.openems.edge.controller.api.modbus.jsonrpc;

import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.base.JsonrpcRequest;

/**
 * Exports the Modbus Protocol to an Excel (xlsx) file.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "getModbusProtocolExportXlsx",
 *   "params": {}
 * }
 * </pre>
 */
public class GetModbusProtocolExportXlsxRequest extends JsonrpcRequest {

	public static final String METHOD = "getModbusProtocolExportXlsx";

	public GetModbusProtocolExportXlsxRequest() {
		super(METHOD);
	}

	private GetModbusProtocolExportXlsxRequest(JsonrpcRequest request) {
		super(request, METHOD);
	}

	@Override
	public JsonObject getParams() {
		return new JsonObject();
	}

}
