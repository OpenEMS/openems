package io.openems.backend.common.jsonrpc.request;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.GenericJsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Request for 'SimulationRequest'.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "simulation",
 *   "params": {
 *     "payload": {@link JsonrpcRequest},
 *   }
 * }
 * </pre>
 */
public class SimulationRequest extends JsonrpcRequest {

	public static final String METHOD = "simulation";

	/**
	 * Create {@link SimulationRequest} from a template {@link JsonrpcRequest}.
	 *
	 * @param r the template {@link JsonrpcRequest}
	 * @return the {@link SimulationRequest}
	 * @throws OpenemsNamedException on parse error
	 */
	public static SimulationRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		var p = r.getParams();
		JsonrpcRequest payload = GenericJsonrpcRequest.from(JsonUtils.getAsJsonObject(p, "payload"));
		return new SimulationRequest(r, payload);
	}

	private final JsonrpcRequest payload;

	public SimulationRequest(JsonrpcRequest payload) {
		super(SimulationRequest.METHOD, payload.getTimeout() /* inherit timeout from payload */);
		this.payload = payload;
	}

	public SimulationRequest(JsonrpcRequest request, JsonrpcRequest payload) {
		super(request, SimulationRequest.METHOD);
		this.payload = payload;
	}

	/**
	 * Gets the Payload {@link JsonrpcRequest}.
	 *
	 * @return Payload
	 */
	public JsonrpcRequest getPayload() {
		return this.payload;
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
				.add("payload", this.payload.toJsonObject()) //
				.build();
	}
}
