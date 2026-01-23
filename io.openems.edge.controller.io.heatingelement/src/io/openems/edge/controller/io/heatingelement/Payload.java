package io.openems.edge.controller.io.heatingelement;

import io.openems.common.jsonrpc.serialization.JsonSerializer;

/**
 * Represents the payload for a scheduled task, containing the parameter
 * sessionEnergy.
 */
public record Payload(int sessionEnergy) {

	/**
	 * Returns a {@link JsonSerializer} for a {@link Payload}.
	 *
	 * @return the created {@link JsonSerializer}
	 */
	public static io.openems.common.jsonrpc.serialization.JsonSerializer<Payload> serializer() {
		return io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer(Payload.class, json -> {
			return new Payload(json.getInt("sessionEnergy"));
		}, obj -> {
			return io.openems.common.utils.JsonUtils.buildJsonObject() //
					.addProperty("sessionEnergy", obj.sessionEnergy()).build();
		});
	}

}
