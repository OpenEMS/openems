package io.openems.edge.common.jsonapi;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;

import com.google.gson.JsonElement;

import io.openems.common.jsonrpc.serialization.EmptyObject;
import io.openems.common.jsonrpc.serialization.EndpointRequestType;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.jsonapi.ReplaceUser.Request;

public class ReplaceUser implements EndpointRequestType<Request, EmptyObject> {

	@Override
	public String getMethod() {
		return "replaceUser";
	}

	@Override
	public JsonSerializer<Request> getRequestSerializer() {
		return Request.serializer();
	}

	@Override
	public JsonSerializer<EmptyObject> getResponseSerializer() {
		return EmptyObject.serializer();
	}

	public static record Request(Role role, Language language, JsonElement request) {
		/**
		 * Returns a {@link JsonSerializer} for a {@link ReplaceUser.Request}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<ReplaceUser.Request> serializer() {
			return jsonObjectSerializer(ReplaceUser.Request.class, json -> {
				return new ReplaceUser.Request(//
						json.getEnum("role", Role.class), //
						json.getEnum("language", Language.class), //
						json.getJsonElement("request") //
				);
			}, obj -> {
				return JsonUtils.buildJsonObject() //
						.addProperty("role", obj.role()) //
						.addProperty("language", obj.language()) //
						.add("request", obj.request()) //
						.build();
			});
		}

	}

}
