package io.openems.edge.core.meta.geocoding;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;

import java.util.List;

import io.openems.common.jsonrpc.serialization.EndpointRequestType;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.core.meta.geocoding.GeocodeJsonRpcEndpoint.Request;
import io.openems.edge.core.meta.geocoding.GeocodeJsonRpcEndpoint.Response;

/**
 * JSON-RPC Request for the "geocode" method.
 *
 * <p>
 * Example request:
 * 
 * <pre>
 * {
 *   "method": "componentJsonApi",
 *   "params": {
 *     "componentId": "_meta",
 *     "payload": {
 *       "method": "geocode",
 *       "params": {
 *         "query": "Berlin Mitte, Invalidenstraße 117"
 *       }
 *     }
 *   }
 * }
 * </pre>
 */
public class GeocodeJsonRpcEndpoint implements EndpointRequestType<Request, Response> {

	@Override
	public String getMethod() {
		return "geocode";
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
			String query//
	) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link GeocodeRequest.Request}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<GeocodeJsonRpcEndpoint.Request> serializer() {
			return jsonObjectSerializer(GeocodeJsonRpcEndpoint.Request.class, //
					json -> new GeocodeJsonRpcEndpoint.Request(//
							json.getString("query")), //
					obj -> JsonUtils.buildJsonObject()//
							.addProperty("query", obj.query())//
							.build());
		}
	}

	public record Response(//
			List<GeoResult> geocodingResults//
	) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link GeocodeRequest.Response}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<GeocodeJsonRpcEndpoint.Response> serializer() {
			return jsonObjectSerializer(GeocodeJsonRpcEndpoint.Response.class, //
					json -> new GeocodeJsonRpcEndpoint.Response(//
							json.getList("geocodingResults", GeoResult.serializer())), //
					obj -> JsonUtils.buildJsonObject()//
							.add("geocodingResults", GeoResult.serializer().toListSerializer()//
									.serialize(obj.geocodingResults()))//
							.build());
		}
	}
}
