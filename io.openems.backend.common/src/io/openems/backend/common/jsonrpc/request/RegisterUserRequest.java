package io.openems.backend.common.jsonrpc.request;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.utils.JsonUtils;

public class RegisterUserRequest extends JsonrpcRequest {

	public static final String METHOD = "registerUser";

	/**
	 * Create {@link RegisterUserRequest} from a template {@link JsonrpcRequest}.
	 *
	 * @param request the template {@link JsonrpcRequest}
	 * @return Created {@link RegisterUserRequest}
	 */
	public static RegisterUserRequest from(JsonrpcRequest request) throws OpenemsNamedException {
		var params = request.getParams();
		var user = JsonUtils.getAsJsonObject(params, "user");
		var oem = JsonUtils.getAsString(params, "oem");
		return new RegisterUserRequest(request, user, oem);
	}

	private final JsonObject user;
	private final String oem;

	private RegisterUserRequest(JsonrpcRequest request, JsonObject jsonObject, String oem) {
		super(request, RegisterUserRequest.METHOD);
		this.user = jsonObject;
		this.oem = oem;
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
				.add("user", this.user) //
				.addProperty("oem", this.oem) //
				.build();
	}

	/**
	 * Gets the User Registration information as {@link JsonObject}.
	 *
	 * @return the {@link JsonObject}
	 */
	public JsonObject getUser() {
		return this.user;
	}

	public String getOem() {
		return this.oem;
	}
}
