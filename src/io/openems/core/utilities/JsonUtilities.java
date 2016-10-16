package io.openems.core.utilities;

import java.net.Inet4Address;
import java.net.UnknownHostException;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.openems.api.exception.ConfigException;

public class JsonUtilities {
	public static JsonArray getAsJsonArray(JsonElement jElement, String memberName) throws ConfigException {
		JsonElement jSubElement = getSubElement(jElement, memberName);
		if (!jSubElement.isJsonArray()) {
			throw new ConfigException("Config [" + memberName + "] is not a JsonArray: " + jSubElement);
		}
		return jSubElement.getAsJsonArray();
	};

	public static JsonObject getAsJsonObject(JsonElement jElement) throws ConfigException {
		if (!jElement.isJsonObject()) {
			throw new ConfigException("Config is not a JsonObject: " + jElement);
		}
		return jElement.getAsJsonObject();
	};

	public static JsonPrimitive getAsPrimitive(JsonElement jElement, String memberName) throws ConfigException {
		JsonElement jSubElement = getSubElement(jElement, memberName);
		if (!jSubElement.isJsonPrimitive()) {
			throw new ConfigException("Config is not a JsonPrimitive: " + jSubElement);
		}
		return jSubElement.getAsJsonPrimitive();
	}

	public static String getAsString(JsonElement jElement, String memberName) throws ConfigException {
		JsonPrimitive jPrimitive = getAsPrimitive(jElement, memberName);
		if (!jPrimitive.isString()) {
			throw new ConfigException("[" + memberName + "] is not a String: " + jPrimitive);
		}
		return jPrimitive.getAsString();
	}

	public static Object getJsonPrimitiveAsClass(JsonPrimitive j, Class<?> clazz) throws ConfigException {
		Object parameter = null;
		if (j.isNumber()) {
			if (clazz.isAssignableFrom(Integer.class)) {
				parameter = j.getAsInt();
			}
		} else if (j.isString()) {
			if (clazz.isAssignableFrom(String.class)) {
				parameter = j.getAsString();
			} else if (clazz.isAssignableFrom(Inet4Address.class)) {
				try {
					parameter = Inet4Address.getByName(j.getAsString());
				} catch (UnknownHostException e) {
					throw new ConfigException("Unable to convert [" + j + "] to IPv4 address");
				}
			}
		}
		if (parameter == null) {
			throw new ConfigException("Unable to match config [" + j + "] to class type [" + clazz + "]");
		}
		return parameter;
	}

	public static JsonElement getSubElement(JsonElement jElement, String memberName) throws ConfigException {
		JsonObject jObject = getAsJsonObject(jElement);
		if (!jObject.has(memberName)) {
			throw new ConfigException("[" + memberName + "] is missing in Config: " + jElement);
		}
		return jObject.get(memberName);
	}

}
