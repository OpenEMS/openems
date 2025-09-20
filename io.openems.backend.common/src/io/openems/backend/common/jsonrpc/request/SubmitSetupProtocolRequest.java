package io.openems.backend.common.jsonrpc.request;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.utils.JsonUtils;

/**
 * Submits the Setup Protocol.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": UUID,
 *   "method": "submitSetupProtocol",
 *   "params": {
 *     "protocol": {
 *       "edge": {
 *         "id": string
 *       },
 *       "customer": {
 *         "firstname": string,
 *         "lastname": string,
 *         "email": string,
 *         "phone": string,
 *         "address": {
 *           "street": string,
 *           "city": string,
 *           "zip": string,
 *           "country": string
 *         },
 *         "company": {
 *          "name": string
 *         }
 *       },
 *       "location": {
 *         "firstname": string,
 *         "lastname": string,
 *         "email": string,
 *         "phone": string,
 *         "address": {
 *           "street": string,
 *           "city": string,
 *           "zip": string,
 *           "country": string
 *         },
 *         "company": {
 *          "name": string
 *         }
 *       },
 *       "lots": [{
 *         "category": string,
 *         "name": string,
 *         "serialNumber": string
 *       }],
 *       "items": [{
 *         "category": string,
 *         "name": string,
 *         "value": string
 *       }],
 *       "oem": string
 *     }
 *   }
 * }
 * </pre>
 */
public class SubmitSetupProtocolRequest extends JsonrpcRequest {

	public static final String METHOD = "submitSetupProtocol";

	/**
	 * Create {@link SubmitSetupProtocolRequest} from a template
	 * {@link JsonrpcRequest}.
	 *
	 * @param request the template {@link JsonrpcRequest}
	 * @return Created {@link SubmitSetupProtocolRequest}
	 * @throws OpenemsNamedException on parse error
	 */
	public static SubmitSetupProtocolRequest from(JsonrpcRequest request) throws OpenemsNamedException {
		var params = request.getParams();
		return new SubmitSetupProtocolRequest(request, JsonUtils.getAsJsonObject(params, "protocol"));
	}

	private final JsonObject jsonObject;

	private SubmitSetupProtocolRequest(JsonrpcRequest request, JsonObject jsonObject) {
		super(request, SubmitSetupProtocolRequest.METHOD);
		this.jsonObject = jsonObject;
	}

	@Override
	public JsonObject getParams() {
		return this.jsonObject;
	}

	/**
	 * Gets the Setup Protocol information as {@link JsonObject}.
	 *
	 * @return the {@link JsonObject}
	 */
	public JsonObject getJsonObject() {
		return this.jsonObject;
	}

}
