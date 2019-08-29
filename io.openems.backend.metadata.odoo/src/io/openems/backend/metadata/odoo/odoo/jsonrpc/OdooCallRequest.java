package io.openems.backend.metadata.odoo.odoo.jsonrpc;

import java.util.UUID;

import io.openems.common.jsonrpc.base.JsonrpcRequest;

/**
 * Represents an abstract Odoo JSON-RPC "call" Request.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "call",
 *   "params": {}
 * }
 * </pre>
 */
public abstract class OdooCallRequest extends JsonrpcRequest {

	public static final String METHOD = "call";

	public OdooCallRequest(UUID id) {
		super(id, METHOD);
	}

	public OdooCallRequest() {
		this(UUID.randomUUID());
	}

}
