package io.openems.edge.io.phoenixcontact.utils;

import com.google.gson.JsonElement;

import io.openems.common.types.OpenemsType;

public final class PlcNextJsonElementHelper {

	private PlcNextJsonElementHelper() {}
	
	/**
	 * Conversion depending on channel type
	 * 
	 * @param jsonElement	represents the JSON element to read
	 * @param type	represents the channel type
	 * @return	proper value object
	 */
	public static Object getJsonValue(JsonElement jsonElement, OpenemsType type) {
		if (type == OpenemsType.BOOLEAN) {
			return jsonElement.getAsBoolean();
		} else if (type == OpenemsType.DOUBLE) {
			return jsonElement.getAsDouble();
		} else if (type == OpenemsType.FLOAT) {
			return jsonElement.getAsFloat();
		} else if (type == OpenemsType.INTEGER) {
			return jsonElement.getAsInt();
		} else if (type == OpenemsType.LONG) {
			return jsonElement.getAsLong();
		} else if (type == OpenemsType.SHORT) {
			return jsonElement.getAsShort();
		} else {
			return jsonElement.getAsString();
		}
	}	
}
