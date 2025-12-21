package io.openems.edge.core.appmanager.jsonrpc;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;

import io.openems.common.jsonrpc.serialization.EmptyObject;
import io.openems.common.jsonrpc.serialization.EndpointRequestType;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.core.appmanager.jsonrpc.CanSwitchEvcsEvse.Response;

public class CanSwitchEvcsEvse implements EndpointRequestType<EmptyObject, Response> {

	public static final String METHOD = "canSwitchEvcsEvse";

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

	public record Response(boolean canSwitch, Version current, String header, String info, String link) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link SwitchEvcsEvse.Response}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Response> serializer() {
			return jsonObjectSerializer(Response.class, json -> {
				return new Response(json.getBoolean("canSwitch"), //
						json.getEnumOrNull("current", Version.class), //
						json.getString("header"), //
						json.getString("info"), //
						json.getStringOrNull("link") //
				);
			}, obj -> {
				return JsonUtils.buildJsonObject() //
						.addProperty("canSwitch", obj.canSwitch()) //
						.addPropertyIfNotNull("current", obj.current()) //
						.addProperty("header", obj.header()) //
						.addProperty("info", obj.info()) //
						.addPropertyIfNotNull("link", obj.link()).build();
			});
		}
	}

	public enum Version {
		NEW, OLD;
	}

}
