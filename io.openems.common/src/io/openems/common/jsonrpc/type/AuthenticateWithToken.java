package io.openems.common.jsonrpc.type;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;

import java.util.Objects;

import io.openems.common.jsonrpc.serialization.EndpointRequestType;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.type.AuthenticateWithToken.Request;
import io.openems.common.utils.JsonUtils;

public class AuthenticateWithToken implements EndpointRequestType<Request, AuthenticateWithPassword.Response> {

	@Override
	public String getMethod() {
		return "authenticateWithToken";
	}

	@Override
	public JsonSerializer<Request> getRequestSerializer() {
		return Request.serializer();
	}

	@Override
	public JsonSerializer<AuthenticateWithPassword.Response> getResponseSerializer() {
		return AuthenticateWithPassword.Response.serializer();
	}

	public record Request(//
			String token //
	) {

		public Request {
			Objects.requireNonNull(token);
		}

		/**
		 * Returns a {@link JsonSerializer} for a {@link AuthenticateWithToken}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Request> serializer() {
			return jsonObjectSerializer(Request.class, json -> {
				return new Request(//
						json.getString("token") //
				);
			}, obj -> {
				return JsonUtils.buildJsonObject() //
						.addProperty("token", obj.token()) //
						.build();
			});
		}

	}

}
