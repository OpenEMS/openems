package io.openems.common.jsonrpc.request;

import java.util.UUID;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.GenericJsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.utils.JsonUtils;

public class AuthenticateRequest extends JsonrpcRequest {

	public final static String METHOD = "authenticate";

	public static AuthenticateRequest from(JsonrpcRequest r) throws OpenemsException {
		JsonObject p = r.getParams();
		String name = JsonUtils.getAsString(p, "name");
		return new AuthenticateRequest(r.getId(), name);
	}

	public static AuthenticateRequest from(JsonObject j) throws OpenemsException {
		return from(GenericJsonrpcRequest.from(j));
	}

	private final String name;

	public AuthenticateRequest(UUID id, String name) {
		super(id, METHOD);
		this.name = name;
	}

	public AuthenticateRequest(String name) {
		this(UUID.randomUUID(), name);
	}

	public String getName() {
		return name;
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
				.addProperty("name", name) //
				.build();
	}
}
