package io.openems.edge.core.appmanager.jsonrpc;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.utils.JsonUtils.toJsonArray;

import java.util.List;

import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.jsonapi.EndpointRequestType;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppInstance;
import io.openems.edge.core.appmanager.jsonrpc.GetAppInstances.Request;
import io.openems.edge.core.appmanager.jsonrpc.GetAppInstances.Response;

/**
 * Gets the active instances of an {@link OpenemsApp}.
 *
 * <p>
 * Request:
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "getAppInstances",
 *   "params": {
 *   	"appId": string
 *   }
 * }
 * </pre>
 *
 * <p>
 * Response:
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {
 *     "instances": {@link OpenemsAppInstance#toJsonObject()}[]
 *   }
 * }
 * </pre>
 */
public class GetAppInstances implements EndpointRequestType<Request, Response> {

	@Override
	public String getMethod() {
		return "getAppInstances";
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
			String appId //
	) {
		/**
		 * Returns a {@link JsonSerializer} for a {@link GetAppInstances.Request}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<GetAppInstances.Request> serializer() {
			return jsonObjectSerializer(GetAppInstances.Request.class, //
					json -> new GetAppInstances.Request(//
							json.getString("appId")), //
					obj -> JsonUtils.buildJsonObject() //
							.addProperty("appId", obj.appId()) //
							.build());
		}

	}

	public record Response(//
			List<OpenemsAppInstance> instances //
	) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link GetAppInstances.Response}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<GetAppInstances.Response> serializer() {
			return jsonObjectSerializer(GetAppInstances.Response.class, //
					json -> new GetAppInstances.Response(json.getList("instances", OpenemsAppInstance.serializer())), //
					obj -> JsonUtils.buildJsonObject() //
							.add("instances", obj.instances().stream() //
									.map(OpenemsAppInstance.serializer()::serialize) //
									.collect(toJsonArray())) //
							.build());
		}

	}

}
