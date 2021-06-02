package io.openems.edge.bridge.modbus.jsonrpc;

import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.base.JsonrpcRequest;

/**
 * Exports the Modbus Registers and current Channel-Value to an Excel (xlsx)
 * file.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "modbusRegistersExportXlsx",
 *   "params": {}
 * }
 * </pre>
 */
public class ModbusRegistersExportXlsxRequest extends JsonrpcRequest {

	public static final String METHOD = "modbusRegistersExportXlsx";

	public ModbusRegistersExportXlsxRequest() {
		super(METHOD);
	}

	private ModbusRegistersExportXlsxRequest(JsonrpcRequest request) {
		super(request, METHOD);
	}

	@Override
	public JsonObject getParams() {
		return new JsonObject();
	}

}
