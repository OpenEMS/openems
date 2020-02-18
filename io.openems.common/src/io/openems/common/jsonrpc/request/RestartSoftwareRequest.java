package io.openems.common.jsonrpc.request;

import java.util.UUID;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;

public class RestartSoftwareRequest extends JsonrpcRequest {
	
	public static RestartSoftwareRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		return new RestartSoftwareRequest(r.getId());
	}
	public final static String METHOD = "restartSoftware";

	public RestartSoftwareRequest(UUID id) {
		super(id, METHOD);
		// TODO Auto-generated constructor stub
	}

	@Override
	public JsonObject getParams() {
		// TODO Auto-generated method stub
		return null;
	}

}
