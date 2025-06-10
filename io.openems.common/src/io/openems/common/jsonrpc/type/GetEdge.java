package io.openems.common.jsonrpc.type;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;

import io.openems.common.jsonrpc.response.GetEdgesResponse.EdgeMetadata;
import io.openems.common.jsonrpc.serialization.EndpointRequestType;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.type.GetEdge.Request;
import io.openems.common.jsonrpc.type.GetEdge.Response;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Request for 'createComponentConfig'.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "createComponentConfig",
 *   "params": {
 *     "factoryPid": string,
 *     "properties": [{
 *       "name": string,
 *       "value": any
 *     }]
 *   }
 * }
 * </pre>
 */
public class GetEdge implements EndpointRequestType<Request, Response> {

	@Override
	public String getMethod() {
		return "getEdge";
	}

	@Override
	public JsonSerializer<Request> getRequestSerializer() {
		return Request.serializer();
	}

	@Override
	public JsonSerializer<Response> getResponseSerializer() {
		return Response.serializer();
	}

	public record Request(//
			String edgeId //
	) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link GetEdge.Request}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Request> serializer() {
			return jsonObjectSerializer(Request.class, //
					json -> new Request(//
							json.getString("edgeId")), //
					obj -> JsonUtils.buildJsonObject() //
							.addProperty("edgeId", obj.edgeId()) //
							.build());
		}

	}

	public record Response(//
			EdgeMetadata edge //
	) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link GetEdge.Request}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Response> serializer() {
			return jsonObjectSerializer(Response.class, //
					json -> new Response(//
							json.getObject("edge", EdgeMetadata.serializer())), //
					obj -> JsonUtils.buildJsonObject() //
							.add("edge", EdgeMetadata.serializer().serialize(obj.edge())) //
							.build());
		}

	}

}
