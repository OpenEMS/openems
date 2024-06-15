package io.openems.edge.common.jsonapi;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;

import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.utils.JsonUtils;

public record Tag(//
		String name //
) {

	/**
	 * Returns a {@link JsonSerializer} for a {@link Tag}.
	 * 
	 * @return the created {@link JsonSerializer}
	 */
	public static JsonSerializer<Tag> serializer() {
		return jsonObjectSerializer(Tag.class, json -> {
			return new Tag(json.getString("name"));
		}, obj -> {
			return JsonUtils.buildJsonObject() //
					.addProperty("name", obj.name()) //
					.build();
		});
	}

}