package io.openems.common.utils;

import java.net.Inet4Address;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
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

// TODO use getAsOptional***() as basis for getAs***() to avoid unnecessary exceptions
public class JsonUtils {
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

	public static Optional<JsonArray> getAsOptionalJsonArray(JsonElement jElement, String memberName) {
		try {
			return Optional.of(getAsJsonArray(jElement, memberName));
		} catch (OpenemsException e) {
			return Optional.empty();
		}
	}

	public static JsonObject getAsJsonObject(JsonElement jElement) throws OpenemsException {
		if (!jElement.isJsonObject()) {
			throw new OpenemsException("This is not a JsonObject: " + jElement);
		}
		return jElement.getAsJsonObject();
	};

	public static JsonObject getAsJsonObject(JsonElement jElement, String memberName) throws OpenemsException {
		JsonElement jsubElement = getSubElement(jElement, memberName);
		if (!jsubElement.isJsonObject()) {
			throw new OpenemsException("Element [" + memberName + "] is not a JsonObject: " + jsubElement);
		}
		return jsubElement.getAsJsonObject();
	};

	public static Optional<JsonObject> getAsOptionalJsonObject(JsonElement jElement, String memberName) {
		try {
			return Optional.of(getAsJsonObject(jElement, memberName));
		} catch (OpenemsException e) {
			return Optional.empty();
		}
	}

	public static JsonPrimitive getAsPrimitive(JsonElement jElement, String memberName) throws OpenemsException {
		JsonElement jSubElement = getSubElement(jElement, memberName);
		return getAsPrimitive(jSubElement);
	}

	public static JsonPrimitive getAsPrimitive(JsonElement jElement) throws OpenemsException {
		if (!jElement.isJsonPrimitive()) {
			throw new OpenemsException("This is not a JsonPrimitive: " + jElement);
		}
		return jElement.getAsJsonPrimitive();
	}

	public static String getAsString(JsonElement jElement) throws OpenemsException {
		JsonPrimitive jPrimitive = getAsPrimitive(jElement);
		if (!jPrimitive.isString()) {
			throw new OpenemsException("This is not a String: " + jPrimitive);
		}
		return jPrimitive.getAsString();
	}

	public static boolean getAsBoolean(JsonElement jElement) throws OpenemsException {
		JsonPrimitive jPrimitive = getAsPrimitive(jElement);
		if (!jPrimitive.isBoolean()) {
			throw new OpenemsException("This is not a Boolean: " + jPrimitive);
		}
		return jPrimitive.getAsBoolean();
	}

	public static Optional<String> getAsOptionalString(JsonElement jElement, String memberName) {
		try {
			return Optional.of(getAsString(jElement, memberName));
		} catch (OpenemsException e) {
			return Optional.empty();
		}
	}

	public static String getAsString(JsonElement jElement, String memberName) throws OpenemsException {
		JsonPrimitive jPrimitive = getAsPrimitive(jElement, memberName);
		if (!jPrimitive.isString()) {
			throw new OpenemsException("Element [" + memberName + "] is not a String: " + jPrimitive);
		}
		return jPrimitive.getAsString();
	}

	public static Optional<Integer> getAsOptionalInt(JsonElement jElement, String memberName) {
		try {
			return Optional.of(getAsInt(jElement, memberName));
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

	public static boolean getAsBoolean(JsonElement jElement, String memberName) throws OpenemsException {
		JsonPrimitive jPrimitive = getAsPrimitive(jElement, memberName);
		if (!jPrimitive.isBoolean()) {
			throw new OpenemsException("Element [" + memberName + "] is not a Boolean: " + jPrimitive);
		}
		return jPrimitive.getAsBoolean();
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

	public static JsonElement getSubElement(JsonElement jElement, String memberName) throws OpenemsException {
		JsonObject jObject = getAsJsonObject(jElement);
		if (!jObject.has(memberName)) {
			throw new OpenemsException("Element [" + memberName + "] is not a Subelement of: " + jElement);
		}
		return jObject.get(memberName);
	}

	/**
	 * Merges the second Object into the first object
	 * 
	 * @param j1
	 * @param j2
	 * @return
	 */
	public static JsonObject merge(JsonObject j1, JsonObject j2) {
		// TODO be smarter: merge down the tree
		for (Entry<String, JsonElement> entry : j2.entrySet()) {
			j1.add(entry.getKey(), entry.getValue());
		}
		return j1;
	}

	public static Optional<JsonObject> merge(Optional<JsonObject> j1Opt, Optional<JsonObject> j2Opt) {
		if (j1Opt.isPresent() && j2Opt.isPresent()) {
			return Optional.of(JsonUtils.merge(j1Opt.get(), j2Opt.get()));
		}
		if (j1Opt.isPresent()) {
			return j1Opt;
		}
		return j2Opt;
	}

	public static boolean hasElement(JsonElement j, String... paths) {
		return getMatchingElements(j, paths).size() > 0;
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
	
	/*
	 * Copied from edge
	 * TODO!
	 */
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
		} else if (value instanceof Long[]){
			/*
			 * Long-Array
			 */
			JsonArray js = new JsonArray();
			for (Long l : (Long[]) value){
				js.add(new JsonPrimitive((Long) l));
			}
			return js;
		}
		throw new NotImplementedException("Converter for [" + value + "]" + " of type [" //
				+ value.getClass().getSimpleName() + "]" //
				+ " to JSON is not implemented.");
	}
}
