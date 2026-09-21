package io.openems.edge.braiinsos.api;

import static io.openems.common.utils.JsonUtils.buildJsonObject;

import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.serialization.JsonSerializerUtil;

public record AuthLogin(String token, int timeout /* [s] */) {

	/**
	 * Returns a {@link JsonSerializer} for a {@link AuthLogin}.
	 * 
	 * @return the created {@link JsonSerializer}
	 */
	public static JsonSerializer<AuthLogin> serializer() {
		return JsonSerializerUtil.jsonObjectSerializer(AuthLogin.class, json -> {
			return new AuthLogin(//
					json.getString("token"), //
					json.getInt("timeout_s"));
		}, obj -> {
			return buildJsonObject() //
					.addProperty("token", obj.token) //
					.addProperty("timeout_s", obj.timeout) //
					.build();
		});
	}
}