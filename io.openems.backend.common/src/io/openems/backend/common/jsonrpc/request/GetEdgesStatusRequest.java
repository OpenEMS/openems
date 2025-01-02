package io.openems.backend.common.jsonrpc.request;

import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.common.utils.JsonUtils.toJsonArray;

import java.util.ArrayList;
import java.util.stream.Stream;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Request for 'getEdgesStatus'.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "getEdgesStatus",
 *   "params": {
 *     "edgeIds": string[]
 *   }
 * }
 * </pre>
 */
public class GetEdgesStatusRequest extends JsonrpcRequest {

	public static final String METHOD = "getEdgesStatus";

	/**
	 * Create {@link GetEdgesStatusRequest} from a {@link JsonrpcRequest}.
	 *
	 * @param request the {@link JsonrpcRequest}
	 * @return the {@link GetEdgesStatusRequest}
	 * @throws OpenemsNamedException on parse error
	 */
	public static GetEdgesStatusRequest from(JsonrpcRequest request) throws OpenemsNamedException {
		var params = request.getParams();
		var edgeIds = new ArrayList<String>();
		var jEdgeIds = JsonUtils.getAsJsonArray(params, "edgeIds");
		for (var jEdgeId : jEdgeIds) {
			edgeIds.add(JsonUtils.getAsString(jEdgeId));
		}
		return new GetEdgesStatusRequest(request, edgeIds.toArray(String[]::new));
	}

	public final String[] edgeIds;

	public GetEdgesStatusRequest(String... edgeIds) {
		super(GetEdgesStatusRequest.METHOD);
		this.edgeIds = edgeIds;
	}

	private GetEdgesStatusRequest(JsonrpcRequest request, String... edgeIds) {
		super(request, GetEdgesStatusRequest.METHOD);
		this.edgeIds = edgeIds;
	}

	@Override
	public JsonObject getParams() {
		return buildJsonObject() //
				.add("edgeIds", Stream.of(this.edgeIds) //
						.map(JsonPrimitive::new) //
						.collect(toJsonArray())) //
				.build();
	}
}
