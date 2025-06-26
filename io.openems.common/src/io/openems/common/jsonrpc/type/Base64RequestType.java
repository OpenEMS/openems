package io.openems.common.jsonrpc.type;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;

import java.util.Base64;

import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.utils.JsonUtils;

public record Base64RequestType(byte[] payload) {

	// TODO should not be encoded in first place only when send
	public Base64RequestType(String payload) {
		this(Base64.getDecoder().decode(payload));
	}

	/**
	 * Returns a {@link JsonSerializer} for a {@link Base64RequestType}.
	 * 
	 * @return the created {@link JsonSerializer}
	 */
	public static JsonSerializer<Base64RequestType> serializer() {
		return jsonObjectSerializer(Base64RequestType.class, json -> {
			return new Base64RequestType(Base64.getDecoder().decode(json.getString("payload")));
		}, obj -> {
			return JsonUtils.buildJsonObject() //
					.addProperty("payload", Base64.getEncoder().encodeToString(obj.payload())) //
					.build();
		});
	}

}
