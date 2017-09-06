package io.openems.backend.utilities;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class JsonUtils {
	public static JsonArray getAsJsonArray(JsonElement jElement) throws OpenemsException {
		if (!jElement.isJsonArray()) {
			throw new OpenemsException("Config is not a JsonArray: " + jElement);
		}
		return jElement.getAsJsonArray();
	};

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

	public static String getAsString(JsonElement jElement) throws OpenemsException {
		JsonPrimitive jPrimitive = getAsPrimitive(jElement);
		if (!jPrimitive.isString()) {
			throw new OpenemsException("Config is not a String: " + jPrimitive);
		}
		return jPrimitive.getAsString();
	}

	public static String getAsString(JsonElement jElement, String memberName) throws OpenemsException {
		JsonPrimitive jPrimitive = getAsPrimitive(jElement, memberName);
		if (!jPrimitive.isString()) {
			throw new OpenemsException("[" + memberName + "] is not a String: " + jPrimitive);
		}
		return jPrimitive.getAsString();
	}

	public static int getAsInt(JsonElement jElement, String memberName) throws OpenemsException {
		JsonPrimitive jPrimitive = getAsPrimitive(jElement, memberName);
		if (jPrimitive.isNumber()) {
			return jPrimitive.getAsInt();
		} else if (jPrimitive.isString()) {
			String string = jPrimitive.getAsString();
			return Integer.parseInt(string);
		}
		throw new OpenemsException("[" + memberName + "] is not a Number: " + jPrimitive);
	}

	public static ZonedDateTime getAsZonedDateTime(JsonElement jElement, String memberName, ZoneId timezone)
			throws OpenemsException {
		String[] date = JsonUtils.getAsString(jElement, memberName).split("-");
		try {
			int year = Integer.valueOf(date[0]);
			int month = Integer.valueOf(date[1]);
			int day = Integer.valueOf(date[2]);
			return ZonedDateTime.of(year, month, day, 0, 0, 0, 0, timezone);
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new OpenemsException("Unable to parse date [" + memberName + "] from [" + jElement + "]: " + e);
		}
	}

	public static JsonElement getSubElement(JsonElement jElement, String memberName) throws OpenemsException {
		JsonObject jObject = getAsJsonObject(jElement);
		if (!jObject.has(memberName)) {
			throw new OpenemsException("[" + memberName + "] is missing in Config: " + jElement);
		}
		return jObject.get(memberName);
	}

}
