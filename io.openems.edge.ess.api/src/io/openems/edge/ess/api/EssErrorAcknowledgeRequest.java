package io.openems.edge.ess.api;

import io.openems.common.jsonrpc.serialization.EmptyObject;
import io.openems.common.jsonrpc.serialization.EndpointRequestType;
import io.openems.common.jsonrpc.serialization.JsonSerializer;

/**
 * Wraps a JSON-RPC Request to clear ess faults.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "essErrorAcknowledge",
 *   "params": {}
 * }
 * </pre>
 */
public class EssErrorAcknowledgeRequest implements EndpointRequestType<EmptyObject, EmptyObject> {

	@Override
	public String getMethod() {
		return "essErrorAcknowledge";
	}

	@Override
	public JsonSerializer<EmptyObject> getRequestSerializer() {
		return EmptyObject.serializer();
	}

	@Override
	public JsonSerializer<EmptyObject> getResponseSerializer() {
		return EmptyObject.serializer();
	}

}