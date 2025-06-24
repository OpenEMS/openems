package io.openems.edge.common.jsonapi;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;

import io.openems.common.jsonrpc.serialization.EmptyObject;
import io.openems.common.jsonrpc.serialization.EndpointRequestType;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.jsonapi.CreateAccountFromSetupKey.Request;

public class CreateAccountFromSetupKey implements EndpointRequestType<Request, EmptyObject> {

	@Override
	public String getMethod() {
		return "createAccountFromSetupKey";
	}

	@Override
	public JsonSerializer<Request> getRequestSerializer() {
		return Request.serializer();
	}

	@Override
	public JsonSerializer<EmptyObject> getResponseSerializer() {
		return EmptyObject.serializer();
	}

	public static record Request(String setupKey, String username, String password) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link ReplaceUser.Request}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<CreateAccountFromSetupKey.Request> serializer() {
			return jsonObjectSerializer(CreateAccountFromSetupKey.Request.class, json -> {
				return new CreateAccountFromSetupKey.Request(//
						json.getString("setupKey"), //
						json.getString("username"), //
						json.getString("password") //
				);
			}, obj -> {
				return JsonUtils.buildJsonObject() //
						.addProperty("setupKey", obj.setupKey()) //
						.addProperty("username", obj.username()) //
						.addProperty("password", obj.password()) //
						.build();
			});
		}

	}

}
