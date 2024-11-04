package io.openems.edge.core.componentmanager.jsonrpc;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.emptyObjectSerializer;
import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;

import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.jsonapi.EndpointRequestType;
import io.openems.edge.core.appmanager.jsonrpc.AddAppInstance;
import io.openems.edge.core.componentmanager.jsonrpc.GetAllComponentFactories.Request;
import io.openems.edge.core.componentmanager.jsonrpc.GetAllComponentFactories.Response;

public class GetAllComponentFactories implements EndpointRequestType<Request, Response> {

	@Override
	public String getMethod() {
		return "getAllComponentFactories";
	}

	@Override
	public JsonSerializer<Request> getRequestSerializer() {
		return Request.serializer();
	}

	@Override
	public JsonSerializer<Response> getResponseSerializer() {
		return Response.serializer();
	}

	public record Request() {

		/**
		 * Returns a {@link JsonSerializer} for a {@link AddAppInstance.Request}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Request> serializer() {
			return emptyObjectSerializer(Request::new);
		}

	}

	// TODO change to proper type
	public record Response(JsonObject factories) {

		/**
		 * Returns a {@link JsonSerializer} for a
		 * {@link GetAllComponentFactories.Response}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<GetAllComponentFactories.Response> serializer() {
			return jsonObjectSerializer(GetAllComponentFactories.Response.class, json -> {
				return new Response(json.getJsonElementPath("factories").getAsJsonObjectPath().get());
			}, obj -> {
				return JsonUtils.buildJsonObject() //
						.add("factories", obj.factories()) //
						.build();
			});
		}

	}

}