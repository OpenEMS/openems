package io.openems.common.jsonrpc.type;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;

import java.util.List;

import io.openems.common.jsonrpc.request.GetEdgesRequest.PaginationOptions;
import io.openems.common.jsonrpc.response.GetEdgesResponse.EdgeMetadata;
import io.openems.common.jsonrpc.serialization.EndpointRequestType;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.type.GetEdges.Request;
import io.openems.common.jsonrpc.type.GetEdges.Response;
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
public class GetEdges implements EndpointRequestType<Request, Response> {

	@Override
	public String getMethod() {
		return "getEdges";
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
			PaginationOptions paginationOptions //
	) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link GetEdges.Request}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Request> serializer() {
			return jsonObjectSerializer(Request.class, //
					json -> new Request(//
							json.getObject("pagination", PaginationOptions.serializer())), //
					obj -> JsonUtils.buildJsonObject() //
							.add("pagination", PaginationOptions.serializer().serialize(obj.paginationOptions())) //
							.build());
		}

	}

	public record Response(//
			List<EdgeMetadata> edges //
	) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link GetEdges.Request}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Response> serializer() {
			return jsonObjectSerializer(Response.class, //
					json -> new Response(//
							json.getList("edge", EdgeMetadata.serializer())), //
					obj -> JsonUtils.buildJsonObject() //
							.add("edge", EdgeMetadata.serializer().toListSerializer().serialize(obj.edges())) //
							.build());
		}

	}

}
