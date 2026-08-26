package io.openems.common.jsonrpc.serialization;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;

import io.openems.common.utils.JsonUtils;

public final class EmptyObject {

	public static final EmptyObject INSTANCE = new EmptyObject();

	/**
	 * Returns a {@link JsonSerializer} for a {@link EmptyObject}.
	 * 
	 * @return the created {@link JsonSerializer}
	 */
	public static JsonSerializer<EmptyObject> serializer() {
		return jsonObjectSerializer(EmptyObject.class, json -> {
			return EmptyObject.INSTANCE;
		}, obj -> {
			return JsonUtils.buildJsonObject() //
					.build();
		});
	}

	private EmptyObject() {
	}

}
