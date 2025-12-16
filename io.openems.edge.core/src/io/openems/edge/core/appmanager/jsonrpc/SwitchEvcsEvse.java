package io.openems.edge.core.appmanager.jsonrpc;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.utils.JsonUtils.toJsonArray;

import java.util.List;

import io.openems.common.jsonrpc.serialization.EmptyObject;
import io.openems.common.jsonrpc.serialization.EndpointRequestType;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.core.appmanager.OpenemsAppInstance;
import io.openems.edge.core.appmanager.jsonrpc.SwitchEvcsEvse.Response;

public class SwitchEvcsEvse implements EndpointRequestType<EmptyObject, Response> {

	public static final String METHOD = "switchEvcsEvse";

	@Override
	public String getMethod() {
		return METHOD;
	}

	@Override
	public JsonSerializer<EmptyObject> getRequestSerializer() {
		return EmptyObject.serializer();
	}

	@Override
	public JsonSerializer<Response> getResponseSerializer() {
		return Response.serializer();
	}

	public record Response(List<OpenemsAppInstance> apps) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link SwitchEvcsEvse.Response}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Response> serializer() {
			return jsonObjectSerializer(Response.class, json -> {
				return new Response(json.getList("apps", OpenemsAppInstance.serializer()));
			}, obj -> {
				return JsonUtils.buildJsonObject() //
						.add("apps", obj.apps.stream()//
								.map(OpenemsAppInstance.serializer()::serialize)//
								.collect(toJsonArray()))
						.build();
			});
		}
	}

}
