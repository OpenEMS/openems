package io.openems.backend.common.jsonrpc.request;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.utils.JsonUtils;

/**
 * Adds a Edge to a User.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": UUID,
 *   "method": "addEdgeToUser",
 *   "params": {
 *      "setupPassword": string
 *   }
 * }
 * </pre>
 */
public class AddEdgeToUserRequest extends JsonrpcRequest {

	public static final String METHOD = "addEdgeToUser";

	/**
	 * Create {@link AddEdgeToUserRequest} from a template {@link JsonrpcRequest}.
	 *
	 * @param r the template {@link JsonrpcRequest}
	 * @return the {@link AddEdgeToUserRequest}
	 * @throws OpenemsNamedException on parse error
	 */
	public static AddEdgeToUserRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		var p = r.getParams();
		var setupPassword = JsonUtils.getAsString(p, "setupPassword");
		return new AddEdgeToUserRequest(r, setupPassword);
	}

	private final String setupPassword;

	public AddEdgeToUserRequest(String setupPassword) {
		super(AddEdgeToUserRequest.METHOD);
		this.setupPassword = setupPassword;
	}

	private AddEdgeToUserRequest(JsonrpcRequest request, String setupPassword) {
		super(request, AddEdgeToUserRequest.METHOD);
		this.setupPassword = setupPassword;
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
				.addProperty("setupPassword", this.setupPassword) //
				.build();
	}

	public String getSetupPassword() {
		return this.setupPassword;
	}

}
