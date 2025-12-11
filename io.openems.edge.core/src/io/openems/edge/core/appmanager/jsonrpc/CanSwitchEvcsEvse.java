package io.openems.edge.core.appmanager.jsonrpc;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;

import io.openems.common.jsonrpc.serialization.EmptyObject;
import io.openems.common.jsonrpc.serialization.EndpointRequestType;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.core.appmanager.jsonrpc.CanSwitchEvcsEvse.Response;

public class CanSwitchEvcsEvse implements EndpointRequestType<EmptyObject, Response> {

	@Override
	public String getMethod() {
		return "canSwitchEvcsEvse";
	}

	@Override
	public JsonSerializer<EmptyObject> getRequestSerializer() {
		return EmptyObject.serializer();
	}

	@Override
	public JsonSerializer<Response> getResponseSerializer() {
		return Response.serializer();
	}

	public record Response(boolean canSwitch) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link SwitchEvcsEvse.Response}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Response> serializer() {
			return jsonObjectSerializer(Response.class, json -> {
				return new Response(json.getBoolean("canSwitch"));
			}, obj -> {
				return JsonUtils.buildJsonObject() //
						.addProperty("canSwitch", obj.canSwitch()) //
						.build();
			});
		}
	}

}
