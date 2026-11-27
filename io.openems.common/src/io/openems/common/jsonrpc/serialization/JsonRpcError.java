package io.openems.common.jsonrpc.serialization;

import static io.openems.common.utils.JsonUtils.buildJsonObject;

import io.openems.common.utils.JsonUtils;

public record JsonRpcError(//
		int code, //
		String message, //
		Object[] data //
) {
	/**
	 * Returns a {@link JsonSerializer} for a {@link JsonRpcError}.
	 *
	 * @return the created {@link JsonSerializer}
	 */
	public static JsonSerializer<JsonRpcError> serializer() {
		return JsonSerializerUtil.jsonObjectSerializer(JsonRpcError.class, json -> new JsonRpcError(//
				json.getInt("code"), //
				json.getString("message"), //
				json.getArray("data", String[]::new, JsonElementPath::getAsString)), //
				obj -> buildJsonObject() //
						.addProperty("code", obj.code()) //
						.addProperty("message", obj.message()) //
						.add("data", JsonUtils.getAsJsonElement(obj.data())) //
						.build() //
		);
	}
}
