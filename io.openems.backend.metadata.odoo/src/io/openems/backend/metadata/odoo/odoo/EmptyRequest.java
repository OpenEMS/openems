package io.openems.backend.metadata.odoo.odoo;

import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.base.JsonrpcRequest;

public class EmptyRequest extends JsonrpcRequest {

	public EmptyRequest() {
		super("");
	}

	@Override
	public JsonObject getParams() {
		return new JsonObject();
	}

}
