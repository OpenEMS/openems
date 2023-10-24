package io.openems.edge.controller.ess.timeofusetariff.jsonrpc;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;

/**
 * Represents a JSON-RPC Request for 'getSchedule'.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "getSchedule",
 *   "params": {}
 * }
 * </pre>
 */
public class GetScheduleRequest extends JsonrpcRequest {

	public static final String METHOD = "getSchedule";

	/**
	 * Create {@link GetScheduleRequest} from a template {@link JsonrpcRequest}.
	 *
	 * @param r the template {@link JsonrpcRequest}
	 * @return the {@link GetScheduleRequest}
	 * @throws OpenemsNamedException on parse error
	 */
	public static GetScheduleRequest from(JsonrpcRequest r) throws OpenemsException {
		return new GetScheduleRequest(r);
	}

	public GetScheduleRequest() {
		super(METHOD);
	}

	private GetScheduleRequest(JsonrpcRequest request) {
		super(request, METHOD);
	}

	@Override
	public JsonObject getParams() {
		return new JsonObject();
	}

}
