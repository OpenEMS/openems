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

		return new RegisterUserRequest(request, JsonUtils.getAsJsonObject(params, "user"));
	}

	private final JsonObject jsonObject;

	private RegisterUserRequest(JsonrpcRequest request, JsonObject jsonObject) {
		super(request, RegisterUserRequest.METHOD);
		this.jsonObject = jsonObject;
	}

	@Override
	public JsonObject getParams() {
		return this.jsonObject;
	}

	/**
	 * Gets the User Registration information as {@link JsonObject}.
	 *
	 * @return the {@link JsonObject}
	 */
	public JsonObject getJsonObject() {
		return this.jsonObject;
	}

}
