package io.openems.backend.common.jsonrpc.request;

import java.util.Set;
import java.util.stream.Collectors;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Request to subscribe to Edges.
 *
 * <p>
 * This is used by UI to get regular updates on specific channels.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "subscribeEdges",
 *   "params": {
 *     "edges": string[]
 *   }
 * }
 * </pre>
 */
public class SubscribeEdgesRequest extends JsonrpcRequest {

	public static final String METHOD = "subscribeEdges";

	/**
	 * Create {@link SubscribeEdgesRequest} from a template {@link JsonrpcRequest}.
	 *
	 * @param r the template {@link JsonrpcRequest}
	 * @return the {@link SubscribeEdgesRequest}
	 * @throws OpenemsNamedException on parse error
	 */
	public static SubscribeEdgesRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		var p = r.getParams();
		return new SubscribeEdgesRequest(r, JsonUtils.stream(JsonUtils.getAsJsonArray(p, "edges"))
				.map(t -> t.getAsString()).collect(Collectors.toSet()));
	}

	private final Set<String> edges;

	private SubscribeEdgesRequest(JsonrpcRequest request, Set<String> edges) {
		super(request, SubscribeEdgesRequest.METHOD);
		this.edges = edges;
	}

	/**
	 * Gets the set of Edges.
	 *
	 * @return the Edges
	 */
	public Set<String> getEdges() {
		return this.edges;
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
				.add("edges", this.edges.stream() //
						.map(t -> new JsonPrimitive(t)) //
						.collect(JsonUtils.toJsonArray())) //
				.build();
	}
}
