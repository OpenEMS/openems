package io.openems.edge.meter.discovergy.jsonrpc;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Request for 'getMeters'.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "getMeters",
 *   "params": {
 *     "meterId": String
 *   }
 * }
 * </pre>
 */
public class GetFieldNamesRequest extends JsonrpcRequest {

	public static final String METHOD = "getFieldNames";

	/**
	 * Create {@link GetFieldNamesRequest} from a template {@link JsonrpcRequest}.
	 *
	 * @param r the template {@link JsonrpcRequest}
	 * @return the {@link GetFieldNamesRequest}
	 * @throws OpenemsNamedException on parse error
	 */
	public static GetFieldNamesRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		var meterId = JsonUtils.getAsString(r.getParams(), "meterId");
		return new GetFieldNamesRequest(r, meterId);
	}

	private final String meterId;

	public GetFieldNamesRequest(String meterId) {
		super(METHOD);
		this.meterId = meterId;
	}

	private GetFieldNamesRequest(JsonrpcRequest request, String meterId) {
		super(request, METHOD);
		this.meterId = meterId;
	}

	/**
	 * Gets the Meter-ID.
	 *
	 * @return Meter-ID
	 */
	public String getMeterId() {
		return this.meterId;
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
				.addProperty("meterId", this.meterId) //
				.build();
	}

}
