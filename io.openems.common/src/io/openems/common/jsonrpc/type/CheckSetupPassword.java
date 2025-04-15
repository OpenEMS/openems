package io.openems.common.jsonrpc.type;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;

import io.openems.common.jsonrpc.serialization.EmptyObject;
import io.openems.common.jsonrpc.serialization.EndpointRequestType;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.utils.JsonUtils;

public class CheckSetupPassword implements EndpointRequestType<CheckSetupPassword.Request, EmptyObject> {

	@Override
	public String getMethod() {
		return "checkSetupPassword";
	}

	@Override
	public JsonSerializer<CheckSetupPassword.Request> getRequestSerializer() {
		return Request.serializer();
	}

	@Override
	public JsonSerializer<EmptyObject> getResponseSerializer() {
		return EmptyObject.serializer();
	}

	public record Request(String setupPassword) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link CheckSetupPassword.Request}.
		 *
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<CheckSetupPassword.Request> serializer() {
			return jsonObjectSerializer(CheckSetupPassword.Request.class, json -> {
				return new CheckSetupPassword.Request(json.getString("setupPassword"));
			}, obj -> {
				return JsonUtils.buildJsonObject() //
						.addProperty("setupPassword", obj.setupPassword()) //
						.build();
			});
		}

	}

}
