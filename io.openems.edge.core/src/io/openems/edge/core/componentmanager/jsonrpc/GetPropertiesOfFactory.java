package io.openems.edge.core.componentmanager.jsonrpc;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.jsonapi.EndpointRequestType;
import io.openems.edge.core.appmanager.jsonrpc.AddAppInstance;
import io.openems.edge.core.componentmanager.jsonrpc.GetPropertiesOfFactory.Request;
import io.openems.edge.core.componentmanager.jsonrpc.GetPropertiesOfFactory.Response;

public class GetPropertiesOfFactory implements EndpointRequestType<Request, Response> {

	@Override
	public String getMethod() {
		return "getPropertiesOfFactory";
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
			String factoryId //
	) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link AddAppInstance.Request}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Request> serializer() {
			return jsonObjectSerializer(Request.class, //
					json -> new Request(//
							json.getString("factoryId")), //
					obj -> JsonUtils.buildJsonObject() //
							.addProperty("factoryId", obj.factoryId()) //
							.build());
		}

	}

	// TODO change to proper types
	public record Response(JsonObject factory, JsonArray properties) {

		/**
		 * Returns a {@link JsonSerializer} for a
		 * {@link GetPropertiesOfFactory.Response}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<GetPropertiesOfFactory.Response> serializer() {
			return jsonObjectSerializer(GetPropertiesOfFactory.Response.class, json -> {
				return new Response(//
						json.getJsonObject("factory"), //
						json.getJsonArray("properties"));
			}, obj -> {
				return JsonUtils.buildJsonObject() //
						.add("factory", obj.factory()) //
						.add("properties", obj.properties()) //
						.build();
			});
		}

	}

}