package io.openems.backend.common.jsonrpc.request;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.utils.JsonUtils;

/**
 * Sets the User Information.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": UUID,
 *   "method": "setUserInformation",
 *   "params": {
 *     "user": {
 *       "firstname": string,
 *       "lastname": string,
 *       "email": string,
 *       "phone": string,
 *       "address": {
 *         "street": string,
 *         "city": string,
 *         "zip": string,
 *         "country": string
 *       },
 *       "company": {
 *         "name": string
 *       }
 *     }
 *   }
 * }
 * </pre>
 */
public class SetUserInformationRequest extends JsonrpcRequest {

	public static final String METHOD = "setUserInformation";

	/**
	 * Create {@link SetUserInformationRequest} from a template
	 * {@link JsonrpcRequest}.
	 *
	 * @param request the template {@link JsonrpcRequest}
	 * @return Created {@link SetUserInformationRequest}
	 * @throws OpenemsNamedException on parse error
	 */
	public static SetUserInformationRequest from(JsonrpcRequest request) throws OpenemsNamedException {
		var params = request.getParams();

		return new SetUserInformationRequest(request, JsonUtils.getAsJsonObject(params, "user"));
	}

	private final JsonObject jsonObject;

	private SetUserInformationRequest(JsonrpcRequest request, JsonObject jsonObject) {
		super(request, SetUserInformationRequest.METHOD);
		this.jsonObject = jsonObject;
	}

	@Override
	public JsonObject getParams() {
		return this.jsonObject;
	}

	/**
	 * Gets the User Information as {@link JsonObject}.
	 *
	 * @return the {@link JsonObject}
	 */
	public JsonObject getJsonObject() {
		return this.jsonObject;
	}

}
