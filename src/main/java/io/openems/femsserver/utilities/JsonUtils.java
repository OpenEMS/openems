package io.openems.femsserver.utilities;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class JsonUtils {
	public static JsonArray getAsJsonArray(JsonElement jElement, String memberName) throws OpenemsException {
		JsonElement jSubElement = getSubElement(jElement, memberName);
		if (!jSubElement.isJsonArray()) {
			throw new OpenemsException("Config [" + memberName + "] is not a JsonArray: " + jSubElement);
		}
		return jSubElement.getAsJsonArray();
	};

	public static JsonObject getAsJsonObject(JsonElement jElement) throws OpenemsException {
		if (!jElement.isJsonObject()) {
			throw new OpenemsException("Config is not a JsonObject: " + jElement);
		}
		return jElement.getAsJsonObject();
	};

	public static JsonObject getAsJsonObject(JsonElement jElement, String memberName) throws OpenemsException {
		JsonElement jsubElement = getSubElement(jElement, memberName);
		if (!jsubElement.isJsonObject()) {
			throw new OpenemsException("Config is not a JsonObject: " + jsubElement);
		}
		return jsubElement.getAsJsonObject();
	};

	public static JsonPrimitive getAsPrimitive(JsonElement jElement, String memberName) throws OpenemsException {
		JsonElement jSubElement = getSubElement(jElement, memberName);
		return getAsPrimitive(jSubElement);
	}
	
	public static JsonPrimitive getAsPrimitive(JsonElement jElement) throws OpenemsException {
		if (!jElement.isJsonPrimitive()) {
			throw new OpenemsException("Element is not a JsonPrimitive: " + jElement);
		}
		return jElement.getAsJsonPrimitive();
	}

	public static String getAsString(JsonElement jElement, String memberName) throws OpenemsException {
		JsonPrimitive jPrimitive = getAsPrimitive(jElement, memberName);
		if (!jPrimitive.isString()) {
			throw new OpenemsException("[" + memberName + "] is not a String: " + jPrimitive);
		}
		return jPrimitive.getAsString();
	}

	public static JsonElement getSubElement(JsonElement jElement, String memberName) throws OpenemsException {
		JsonObject jObject = getAsJsonObject(jElement);
		if (!jObject.has(memberName)) {
			throw new OpenemsException("[" + memberName + "] is missing in Config: " + jElement);
		}
		return jObject.get(memberName);
	}

}
