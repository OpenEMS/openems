package io.openems.edge.core.componentmanager.jsonrpc;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;

import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.serialization.EmptyObject;
import io.openems.common.jsonrpc.serialization.EndpointRequestType;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.core.componentmanager.jsonrpc.GetAllComponentFactories.Response;

public class GetAllComponentFactories implements EndpointRequestType<EmptyObject, Response> {

	@Override
	public String getMethod() {
		return "getAllComponentFactories";
	}

	@Override
	public JsonSerializer<EmptyObject> getRequestSerializer() {
		return EmptyObject.serializer();
	}

	@Override
	public JsonSerializer<Response> getResponseSerializer() {
		return Response.serializer();
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