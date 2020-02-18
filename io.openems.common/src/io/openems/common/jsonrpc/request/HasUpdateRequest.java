package io.openems.common.jsonrpc.request;

import java.util.UUID;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;

public class HasUpdateRequest extends JsonrpcRequest{
	
	public static HasUpdateRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		return new HasUpdateRequest(r.getId());
	}
	public final static String METHOD = "hasUpdate";
	public HasUpdateRequest(UUID id) {
		super(id, METHOD);
	}

	@Override
	public JsonObject getParams() {
		// TODO Auto-generated method stub
		return null;
	}

}
