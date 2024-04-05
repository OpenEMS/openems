package io.openems.edge.bridge.onewire.jsonrpc;

import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.base.JsonrpcRequest;

/**
 * Wraps a JSON-RPC Request to query the available OneWire devices.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "getDevices",
 *   "params": {}
 * }
 * </pre>
 */
public class GetDevicesRequest extends JsonrpcRequest {

	public static final String METHOD = "getDevices";

	public GetDevicesRequest() {
		super(METHOD);
	}

	private GetDevicesRequest(JsonrpcRequest request) {
		super(request, METHOD);
	}

	@Override
	public JsonObject getParams() {
		return new JsonObject();
	}

}
