package io.openems.common.utils;

import java.net.Inet4Address;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import io.openems.common.exceptions.NotImplementedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelEnum;

public class JsonUtils {
	public static boolean getAsBoolean(JsonElement jElement) throws OpenemsException {
		JsonPrimitive jPrimitive = getAsPrimitive(jElement);
		if (!jPrimitive.isBoolean()) {
			throw new OpenemsException("This is not a Boolean: " + jPrimitive);
		}
		return jPrimitive.getAsBoolean();
	};

	public static boolean getAsBoolean(JsonElement jElement, String memberName) throws OpenemsException {
		JsonPrimitive jPrimitive = getAsPrimitive(jElement, memberName);
		if (!jPrimitive.isBoolean()) {
			throw new OpenemsException("Element [" + memberName + "] is not a Boolean: " + jPrimitive);
		}
		return jPrimitive.getAsBoolean();
	};

	public static int getAsInt(JsonElement jElement, String memberName) throws OpenemsException {
		JsonPrimitive jPrimitive = getAsPrimitive(jElement, memberName);
		if (jPrimitive.isNumber()) {
			return jPrimitive.getAsInt();
		} else if (jPrimitive.isString()) {
			String string = jPrimitive.getAsString();
			return Integer.parseInt(string);
		}
		throw new OpenemsException("Element [" + memberName + "] is not an Integer: " + jPrimitive);
	}

	public static JsonArray getAsJsonArray(JsonElement jElement) throws OpenemsException {
		if (!jElement.isJsonArray()) {
			throw new OpenemsException("This is not a JsonArray: " + jElement);
		}
		return jElement.getAsJsonArray();
	};

	public static JsonArray getAsJsonArray(JsonElement jElement, String memberName) throws OpenemsException {
		JsonElement jSubElement = getSubElement(jElement, memberName);
		if (!jSubElement.isJsonArray()) {
			throw new OpenemsException("Element [" + memberName + "] is not a JsonArray: " + jSubElement);
		}
		return jSubElement.getAsJsonArray();
	};

	public static JsonElement getAsJsonElement(Object value) throws NotImplementedException {
		// null
		if (value == null) {
			return null;
		}
		// optional
		if (value instanceof Optional<?>) {
			if (!((Optional<?>) value).isPresent()) {
				return null;
			} else {
				value = ((Optional<?>) value).get();
			}
		}
		if (value instanceof Number) {
			/*
			 * Number
			 */
			return new JsonPrimitive((Number) value);
		} else if (value instanceof ChannelEnum) {
			/*
			 * ChannelEnum
			 */
			return new JsonPrimitive(((ChannelEnum) value).getValue());
		} else if (value instanceof String) {
			/*
			 * String
			 */
			return new JsonPrimitive((String) value);
		} else if (value instanceof Boolean) {
			/*
			 * Boolean
			 */
			return new JsonPrimitive((Boolean) value);
		} else if (value instanceof Inet4Address) {
			/*
			 * Inet4Address
			 */
			return new JsonPrimitive(((Inet4Address) value).getHostAddress());
		} else if (value instanceof JsonElement) {
			/*
			 * JsonElement
			 */
			return (JsonElement) value;
		} else if (value instanceof Long[]) {
			/*
			 * Long-Array
			 */
			JsonArray js = new JsonArray();
			for (Long l : (Long[]) value) {
				js.add(new JsonPrimitive((Long) l));
			}
			return js;
		}
		throw new NotImplementedException("Converter for [" + value + "]" + " of type [" //
				+ value.getClass().getSimpleName() + "]" //
				+ " to JSON is not implemented.");
	};

	public static JsonObject getAsJsonObject(JsonElement jElement) throws OpenemsException {
		if (!jElement.isJsonObject()) {
			throw new OpenemsException("This is not a JsonObject: " + jElement);
		}
		return jElement.getAsJsonObject();
	}

	public static JsonObject getAsJsonObject(JsonElement jElement, String memberName) throws OpenemsException {
		JsonElement jsubElement = getSubElement(jElement, memberName);
		if (!jsubElement.isJsonObject()) {
			throw new OpenemsException("Element [" + memberName + "] is not a JsonObject: " + jsubElement);
		}
		return jsubElement.getAsJsonObject();
	}

	public static long getAsLong(JsonElement jElement, String memberName) throws OpenemsException {
		JsonPrimitive jPrimitive = getAsPrimitive(jElement, memberName);
		if (jPrimitive.isNumber()) {
			return jPrimitive.getAsLong();
		} else if (jPrimitive.isString()) {
			String string = jPrimitive.getAsString();
			return Long.parseLong(string);
		}
		throw new OpenemsException("[" + memberName + "] is not a Number: " + jPrimitive);
	}

	public static Optional<Integer> getAsOptionalInt(JsonElement jElement, String memberName) {
		try {
			return Optional.of(getAsInt(jElement, memberName));
		} catch (OpenemsException e) {
			return Optional.empty();
		}
	}

	public static Optional<JsonArray> getAsOptionalJsonArray(JsonElement jElement, String memberName) {
		try {
			return Optional.of(getAsJsonArray(jElement, memberName));
		} catch (OpenemsException e) {
			return Optional.empty();
		}
	}

	public static Optional<JsonObject> getAsOptionalJsonObject(JsonElement jElement) {
		try {
			return Optional.of(getAsJsonObject(jElement));
		} catch (OpenemsException e) {
			return Optional.empty();
		}
	}

	public static Optional<JsonObject> getAsOptionalJsonObject(JsonElement jElement, String memberName) {
		try {
			return Optional.of(getAsJsonObject(jElement, memberName));
		} catch (OpenemsException e) {
			return Optional.empty();
		}
	}

	public static Optional<Long> getAsOptionalLong(JsonElement jElement, String memberName) {
		try {
			return Optional.of(getAsLong(jElement, memberName));
		} catch (OpenemsException e) {
			return Optional.empty();
		}
	}

	public static Optional<String> getAsOptionalString(JsonElement jElement, String memberName) {
		try {
			return Optional.of(getAsString(jElement, memberName));
		} catch (OpenemsException e) {
			return Optional.empty();
		}
	}

	public static Object getAsBestType(JsonElement j) {
		try {
			if (!j.isJsonPrimitive()) {
				return j.toString();
			}
			JsonPrimitive jP = j.getAsJsonPrimitive();
			if (jP.isBoolean()) {
				return jP.getAsBoolean();
			}
			if (jP.isNumber()) {
				Number n = jP.getAsNumber();
				return n.intValue();
			}
			return j.getAsString();
		} catch (IllegalStateException e) {
			throw new IllegalStateException("Failed to parse JsonElement [" + j + "]", e);
		}
	}

	public static Object getAsType(Class<?> type, JsonElement j) throws NotImplementedException {
		try {
			if (Integer.class.isAssignableFrom(type)) {
				/*
				 * Asking for an Integer
				 */
				return j.getAsInt();

			} else if (Long.class.isAssignableFrom(type)) {
				/*
				 * Asking for an Long
				 */
				return j.getAsLong();
			} else if (Boolean.class.isAssignableFrom(type)) {
				/*
				 * Asking for an Boolean
				 */
				return j.getAsBoolean();
			} else if (Double.class.isAssignableFrom(type)) {
				/*
				 * Asking for an Double
				 */
				return j.getAsDouble();
			} else if (String.class.isAssignableFrom(type)) {
				/*
				 * Asking for a String
				 */
				return j.getAsString();
			} else if (JsonObject.class.isAssignableFrom(type)) {
				/*
				 * Asking for a JsonObject
				 */
				return j.getAsJsonObject();
			} else if (JsonArray.class.isAssignableFrom(type)) {
				/*
				 * Asking for a JsonArray
				 */
				return j.getAsJsonArray();
			} else if (type.isArray()) {
				/**
				 * Asking for Array
				 */
				if (Long.class.isAssignableFrom(type.getComponentType())) {
					/**
					 * Asking for ArrayOfLong
					 */
					if (j.isJsonArray()) {
						JsonArray js = j.getAsJsonArray();
						Long[] la = new Long[js.size()];
						for (int i = 0; i < js.size(); i++) {
							la[i] = js.get(i).getAsLong();
						}
						return la;
					}

				}
			}
		} catch (IllegalStateException e) {
			throw new IllegalStateException("Failed to parse JsonElement [" + j + "]", e);
		}
		throw new NotImplementedException(
				"Converter for value [" + j + "] to class type [" + type + "] is not implemented.");
	}

	public static Object getAsType(Optional<Class<?>> typeOptional, JsonElement j) throws NotImplementedException {
		if (!typeOptional.isPresent()) {
			throw new NotImplementedException("Type of Channel was not set: " + j.getAsString());
		}
		Class<?> type = typeOptional.get();
		return getAsType(type, j);
	}

	public static JsonPrimitive getAsPrimitive(JsonElement jElement) throws OpenemsException {
		if (!jElement.isJsonPrimitive()) {
			throw new OpenemsException("This is not a JsonPrimitive: " + jElement);
		}
		return jElement.getAsJsonPrimitive();
	}

	public static JsonPrimitive getAsPrimitive(JsonElement jElement, String memberName) throws OpenemsException {
		JsonElement jSubElement = getSubElement(jElement, memberName);
		return getAsPrimitive(jSubElement);
	}

	public static String getAsString(JsonElement jElement) throws OpenemsException {
		JsonPrimitive jPrimitive = getAsPrimitive(jElement);
		if (!jPrimitive.isString()) {
			throw new OpenemsException("This is not a String: " + jPrimitive);
		}
		return jPrimitive.getAsString();
	}

	public static String getAsString(JsonElement jElement, String memberName) throws OpenemsException {
		JsonPrimitive jPrimitive = getAsPrimitive(jElement, memberName);
		if (!jPrimitive.isString()) {
			throw new OpenemsException("Element [" + memberName + "] is not a String: " + jPrimitive);
		}
		return jPrimitive.getAsString();
	}

	/**
	 * Takes a json in the form 'YYYY-MM-DD' and converts it to a ZonedDateTime with
	 * hour, minute and second set to zero.
	 * 
	 * @param jElement
	 * @param memberName
	 * @param timezone
	 * @return
	 * @throws OpenemsException
	 */
	public static ZonedDateTime getAsZonedDateTime(JsonElement jElement, String memberName, ZoneId timezone)
			throws OpenemsException {
		String[] date = JsonUtils.getAsString(jElement, memberName).split("-");
		try {
			int year = Integer.valueOf(date[0]);
			int month = Integer.valueOf(date[1]);
			int day = Integer.valueOf(date[2]);
			return ZonedDateTime.of(year, month, day, 0, 0, 0, 0, timezone);
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new OpenemsException("Element [" + memberName + "] is not a Date: " + jElement + ". Error: " + e);
		}
	}

	public static Set<JsonElement> getMatchingElements(JsonElement j, String... paths) {
		Set<JsonElement> result = new HashSet<JsonElement>();
		if (paths.length == 0) {
			// last path element
			result.add(j);
			return result;
		}
		String path = paths[0];
		if (j.isJsonObject()) {
			JsonObject jO = j.getAsJsonObject();
			if (jO.has(path)) {
				List<String> nextPathsList = new ArrayList<String>(Arrays.asList(paths));
				nextPathsList.remove(0);
				String[] nextPaths = nextPathsList.toArray(new String[0]);
				result.addAll(getMatchingElements(jO.get(path), nextPaths));
			}
		} else if (j.isJsonArray()) {
			for (JsonElement jE : j.getAsJsonArray()) {
				result.addAll(getMatchingElements(jE, paths));
			}
		} else if (j.isJsonPrimitive()) {
			JsonPrimitive jP = j.getAsJsonPrimitive();
			if (jP.isString()) {
				if (jP.getAsString().equals(path)) {
					result.add(jP);
				}
			}
		}
		return result;
	}

	public static JsonElement getSubElement(JsonElement jElement, String memberName) throws OpenemsException {
		JsonObject jObject = getAsJsonObject(jElement);
		if (!jObject.has(memberName)) {
			throw new OpenemsException("Element [" + memberName + "] is not a Subelement of: " + jElement);
		}
		return jObject.get(memberName);
	}

	public static boolean hasElement(JsonElement j, String... paths) {
		return getMatchingElements(j, paths).size() > 0;
	}

	/**
	 * Parses a string to a JsonElement
	 * 
	 * @param string
	 * @return
	 */
	public static JsonElement parse(String string) throws OpenemsException {
		try {
			JsonParser parser = new JsonParser();
			return parser.parse(string);
		} catch (JsonParseException e) {
			throw new OpenemsException("Unable to parse [" + string + "] + to JSON: " + e.getMessage(), e);
		}
	}

	/**
	 * Pretty print a JsonElement
	 *
	 * @param j
	 */
	public static void prettyPrint(JsonElement j) {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String json = gson.toJson(j);
		System.out.println(json);
	}
}
