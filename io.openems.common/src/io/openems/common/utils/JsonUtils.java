package io.openems.common.utils;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import io.openems.common.exceptions.NotImplementedException;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;

public class JsonUtils {

	/**
	 * A temporary builder class for JsonArrays.
	 */
	public static class JsonArrayBuilder {

		private final JsonArray j;

		protected JsonArrayBuilder() {
			this(new JsonArray());
		}

		protected JsonArrayBuilder(JsonArray j) {
			this.j = j;
		}

		/**
		 * Add a boolean value to the {@link JsonArray}.
		 *
		 * @param value the value
		 * @return the {@link JsonArrayBuilder}
		 */
		public JsonArrayBuilder add(boolean value) {
			this.j.add(value);
			return this;
		}

		/**
		 * Add a int value to the {@link JsonArray}.
		 *
		 * @param value the value
		 * @return the {@link JsonArrayBuilder}
		 */
		public JsonArrayBuilder add(int value) {
			this.j.add(value);
			return this;
		}

		/**
		 * Add a {@link JsonElement} value to the {@link JsonArray}.
		 *
		 * @param value the value
		 * @return the {@link JsonArrayBuilder}
		 */
		public JsonArrayBuilder add(JsonElement value) {
			this.j.add(value);
			return this;
		}

		/**
		 * Add a long value to the {@link JsonArray}.
		 *
		 * @param value the value
		 * @return the {@link JsonArrayBuilder}
		 */
		public JsonArrayBuilder add(long value) {
			this.j.add(value);
			return this;
		}

		/**
		 * Add a String value to the {@link JsonArray}.
		 *
		 * @param value the value
		 * @return the {@link JsonArrayBuilder}
		 */
		public JsonArrayBuilder add(String value) {
			this.j.add(value);
			return this;
		}

		/**
		 * Return the built {@link JsonArray}.
		 *
		 * @return the {@link JsonArray}
		 */
		public JsonArray build() {
			return this.j;
		}

	}

	/**
	 * A temporary builder class for JsonObjects.
	 */
	public static class JsonObjectBuilder {

		private final JsonObject j;

		protected JsonObjectBuilder() {
			this(new JsonObject());
		}

		protected JsonObjectBuilder(JsonObject j) {
			this.j = j;
		}

		/**
		 * Add a {@link JsonElement} value to the {@link JsonObject}.
		 *
		 * @param property the key
		 * @param value    the value
		 * @return the {@link JsonObjectBuilder}
		 */
		public JsonObjectBuilder add(String property, JsonElement value) {
			this.j.add(property, value);
			return this;
		}

		/**
		 * Add a boolean value to the {@link JsonObject}.
		 *
		 * @param property the key
		 * @param value    the value
		 * @return the {@link JsonObjectBuilder}
		 */
		public JsonObjectBuilder addProperty(String property, boolean value) {
			this.j.addProperty(property, value);
			return this;
		}

		/**
		 * Add a double value to the {@link JsonObject}.
		 *
		 * @param property the key
		 * @param value    the value
		 * @return the {@link JsonObjectBuilder}
		 */
		public JsonObjectBuilder addProperty(String property, double value) {
			this.j.addProperty(property, value);
			return this;
		}

		/**
		 * Add a int value to the {@link JsonObject}.
		 *
		 * @param property the key
		 * @param value    the value
		 * @return the {@link JsonObjectBuilder}
		 */
		public JsonObjectBuilder addProperty(String property, int value) {
			this.j.addProperty(property, value);
			return this;
		}

		/**
		 * Add a long value to the {@link JsonObject}.
		 *
		 * @param property the key
		 * @param value    the value
		 * @return the {@link JsonObjectBuilder}
		 */
		public JsonObjectBuilder addProperty(String property, long value) {
			this.j.addProperty(property, value);
			return this;
		}

		/**
		 * Add a String value to the {@link JsonObject}.
		 *
		 * @param property the key
		 * @param value    the value
		 * @return the {@link JsonObjectBuilder}
		 */
		public JsonObjectBuilder addProperty(String property, String value) {
			this.j.addProperty(property, value);
			return this;
		}

		/**
		 * Add a {@link Boolean} value to the {@link JsonObject}.
		 *
		 * @param property the key
		 * @param value    the value
		 * @return the {@link JsonObjectBuilder}
		 */
		public JsonObjectBuilder addPropertyIfNotNull(String property, Boolean value) {
			if (value != null) {
				this.j.addProperty(property, value);
			}
			return this;
		}

		/**
		 * Add a {@link Double} value to the {@link JsonObject} if it is not null.
		 *
		 * @param property the key
		 * @param value    the value
		 * @return the {@link JsonObjectBuilder}
		 */
		public JsonObjectBuilder addPropertyIfNotNull(String property, Double value) {
			if (value != null) {
				this.j.addProperty(property, value);
			}
			return this;
		}

		/**
		 * Add an {@link Integer} value to the {@link JsonObject} if it is not null.
		 *
		 * @param property the key
		 * @param value    the value
		 * @return the {@link JsonObjectBuilder}
		 */
		public JsonObjectBuilder addPropertyIfNotNull(String property, Integer value) {
			if (value != null) {
				this.j.addProperty(property, value);
			}
			return this;
		}

		/**
		 * Add a {@link Long} value to the {@link JsonObject} if it is not null.
		 *
		 * @param property the key
		 * @param value    the value
		 * @return the {@link JsonObjectBuilder}
		 */
		public JsonObjectBuilder addPropertyIfNotNull(String property, Long value) {
			if (value != null) {
				this.j.addProperty(property, value);
			}
			return this;
		}

		/**
		 * Add a {@link String} value to the {@link JsonObject} if it is not null.
		 *
		 * @param property the key
		 * @param value    the value
		 * @return the {@link JsonObjectBuilder}
		 */
		public JsonObjectBuilder addPropertyIfNotNull(String property, String value) {
			if (value != null) {
				this.j.addProperty(property, value);
			}
			return this;
		}

		/**
		 * Return the built {@link JsonObject}.
		 *
		 * @return the {@link JsonObject}
		 */
		public JsonObject build() {
			return this.j;
		}

	}

	private static final Logger LOG = LoggerFactory.getLogger(JsonUtils.class);

	/**
	 * Creates a JsonArray using a Builder.
	 *
	 * @return the Builder
	 */
	public static JsonArrayBuilder buildJsonArray() {
		return new JsonArrayBuilder();
	}

	/**
	 * Creates a JsonArray using a Builder. Initialized from an existing JsonArray.
	 *
	 * @param j the initial JsonArray
	 * @return the Builder
	 */
	public static JsonArrayBuilder buildJsonArray(JsonArray j) {
		return new JsonArrayBuilder(j);
	}

	/**
	 * Creates a JsonObject using a Builder.
	 *
	 * @return the Builder
	 */
	public static JsonObjectBuilder buildJsonObject() {
		return new JsonObjectBuilder();
	}

	/**
	 * Creates a JsonObject using a Builder. Initialized from an existing
	 * JsonObject.
	 *
	 * @param j the initial JsonObject
	 * @return the Builder
	 */
	public static JsonObjectBuilder buildJsonObject(JsonObject j) {
		return new JsonObjectBuilder(j);
	}

	/**
	 * Gets the {@link JsonElement} as {@link JsonPrimitive}.
	 *
	 * @param jElement the {@link JsonElement}
	 * @return the {@link JsonPrimitive} value
	 * @throws OpenemsNamedException on error
	 */
	public static JsonPrimitive getAsPrimitive(JsonElement jElement) throws OpenemsNamedException {
		if (!jElement.isJsonPrimitive()) {
			throw OpenemsError.JSON_NO_PRIMITIVE.exception(jElement.toString().replace("%", "%%"));
		}
		return jElement.getAsJsonPrimitive();
	}

	/**
	 * Gets the member of the {@link JsonElement} as {@link JsonPrimitive}.
	 *
	 * @param jElement   the {@link JsonElement}
	 * @param memberName the name of the member
	 * @return the {@link JsonPrimitive} value
	 * @throws OpenemsNamedException on error
	 */
	public static JsonPrimitive getAsPrimitive(JsonElement jElement, String memberName) throws OpenemsNamedException {
		var jSubElement = getSubElement(jElement, memberName);
		if (!jSubElement.isJsonPrimitive()) {
			throw OpenemsError.JSON_NO_PRIMITIVE_MEMBER.exception(memberName, jElement.toString().replace("%", "%%"));
		}
		return jSubElement.getAsJsonPrimitive();
	}

	/**
	 * Gets the member of the {@link JsonElement} as {@link Optional}
	 * {@link JsonElement}.
	 *
	 * @param jElement   the {@link JsonElement}
	 * @param memberName the name of the member
	 * @return the {@link Optional} {@link JsonElement} value
	 * @throws OpenemsNamedException on error
	 */
	public static Optional<JsonElement> getOptionalSubElement(JsonElement jElement, String memberName) {
		try {
			return Optional.of(getSubElement(jElement, memberName));
		} catch (OpenemsNamedException e) {
			return Optional.empty();
		}
	}

	/**
	 * Gets the member of the {@link JsonElement} as {@link JsonElement}.
	 *
	 * @param jElement   the {@link JsonElement}
	 * @param memberName the name of the member
	 * @return the {@link JsonElement} value
	 * @throws OpenemsNamedException on error
	 */
	public static JsonElement getSubElement(JsonElement jElement, String memberName) throws OpenemsNamedException {
		var jObject = getAsJsonObject(jElement);
		if (!jObject.has(memberName)) {
			throw OpenemsError.JSON_HAS_NO_MEMBER.exception(memberName,
					StringUtils.toShortString(jElement, 100).replace("%", "%%"));
		}
		return jObject.get(memberName);
	}

	/**
	 * Gets the {@link JsonElement} as {@link JsonObject}.
	 *
	 * @param jElement the {@link JsonElement}
	 * @return the {@link JsonObject} value
	 * @throws OpenemsNamedException on error
	 */
	public static JsonObject getAsJsonObject(JsonElement jElement) throws OpenemsNamedException {
		if (!jElement.isJsonObject()) {
			throw OpenemsError.JSON_NO_OBJECT.exception(jElement.toString().replace("%", "%%"));
		}
		return jElement.getAsJsonObject();
	}

	/**
	 * Gets the member of the {@link JsonElement} as {@link JsonObject}.
	 *
	 * @param jElement   the {@link JsonElement}
	 * @param memberName the name of the member
	 * @return the {@link JsonObject} value
	 * @throws OpenemsNamedException on error
	 */
	public static JsonObject getAsJsonObject(JsonElement jElement, String memberName) throws OpenemsNamedException {
		var subElement = getSubElement(jElement, memberName);
		if (!subElement.isJsonObject()) {
			throw OpenemsError.JSON_NO_OBJECT_MEMBER.exception(memberName,
					StringUtils.toShortString(subElement, 100).replace("%", "%%"));
		}
		return subElement.getAsJsonObject();
	}

	/**
	 * Gets the {@link JsonElement} as {@link Optional} {@link JsonObject}.
	 *
	 * @param jElement the {@link JsonElement}
	 * @return the {@link Optional} {@link JsonObject} value
	 * @throws OpenemsNamedException on error
	 */
	public static Optional<JsonObject> getAsOptionalJsonObject(JsonElement jElement) {
		try {
			return Optional.of(getAsJsonObject(jElement));
		} catch (OpenemsNamedException e) {
			return Optional.empty();
		}
	}

	/**
	 * Gets the member of the {@link JsonElement} as {@link Optional}
	 * {@link JsonObject}.
	 *
	 * @param jElement   the {@link JsonElement}
	 * @param memberName the name of the member
	 * @return the {@link Optional} {@link JsonObject} value
	 * @throws OpenemsNamedException on error
	 */
	public static Optional<JsonObject> getAsOptionalJsonObject(JsonElement jElement, String memberName) {
		try {
			return Optional.of(getAsJsonObject(jElement, memberName));
		} catch (OpenemsNamedException e) {
			return Optional.empty();
		}
	}

	/**
	 * Gets the {@link JsonElement} as {@link JsonArray}.
	 *
	 * @param jElement the {@link JsonElement}
	 * @return the {@link JsonArray} value
	 * @throws OpenemsNamedException on error
	 */
	public static JsonArray getAsJsonArray(JsonElement jElement) throws OpenemsNamedException {
		if (!jElement.isJsonArray()) {
			throw OpenemsError.JSON_NO_ARRAY.exception(jElement.toString().replace("%", "%%"));
		}
		return jElement.getAsJsonArray();
	}

	/**
	 * Gets the member of the {@link JsonElement} as {@link JsonArray}.
	 *
	 * @param jElement   the {@link JsonElement}
	 * @param memberName the name of the member
	 * @return the {@link JsonArray} value
	 * @throws OpenemsNamedException on error
	 */
	public static JsonArray getAsJsonArray(JsonElement jElement, String memberName) throws OpenemsNamedException {
		var jSubElement = getSubElement(jElement, memberName);
		if (!jSubElement.isJsonArray()) {
			throw OpenemsError.JSON_NO_ARRAY_MEMBER.exception(memberName, jSubElement.toString().replace("%", "%%"));
		}
		return jSubElement.getAsJsonArray();
	}

	/**
	 * Gets the member of the {@link JsonElement} as {@link Optional}
	 * {@link JsonArray}.
	 *
	 * @param jElement   the {@link JsonElement}
	 * @param memberName the name of the member
	 * @return the {@link Optional} {@link JsonArray} value
	 * @throws OpenemsNamedException on error
	 */
	public static Optional<JsonArray> getAsOptionalJsonArray(JsonElement jElement, String memberName) {
		try {
			return Optional.of(getAsJsonArray(jElement, memberName));
		} catch (OpenemsNamedException e) {
			return Optional.empty();
		}
	}

	/**
	 * Gets the {@link JsonElement} as {@link String}.
	 *
	 * @param jElement the {@link JsonElement}
	 * @return the {@link String} value
	 * @throws OpenemsNamedException on error
	 */

	public static String getAsString(JsonElement jElement) throws OpenemsNamedException {
		return getAsString(getAsPrimitive(jElement));
	}

	/**
	 * Gets the {@link JsonPrimitive} as {@link String}.
	 *
	 * @param jPrimitive the {@link JsonPrimitive}
	 * @return the {@link String} value
	 * @throws OpenemsNamedException on error
	 */

	public static String getAsString(JsonPrimitive jPrimitive) throws OpenemsNamedException {
		if (!jPrimitive.isString()) {
			throw OpenemsError.JSON_NO_STRING.exception(jPrimitive.toString().replace("%", "%%"));
		}
		return jPrimitive.getAsString();
	}

	/**
	 * Gets the member of the {@link JsonElement} as {@link String}.
	 *
	 * @param jElement   the {@link JsonElement}
	 * @param memberName the name of the member
	 * @return the {@link String} value
	 * @throws OpenemsNamedException on error
	 */
	public static String getAsString(JsonElement jElement, String memberName) throws OpenemsNamedException {
		var jPrimitive = getAsPrimitive(jElement, memberName);
		if (!jPrimitive.isString()) {
			throw OpenemsError.JSON_NO_STRING_MEMBER.exception(memberName, jPrimitive.toString().replace("%", "%%"));
		}
		return jPrimitive.getAsString();
	}

	/**
	 * Gets the {@link JsonElement} as {@link Optional} {@link String}.
	 *
	 * @param jElement the {@link JsonElement}
	 * @return the {@link Optional} {@link String} value
	 * @throws OpenemsNamedException on error
	 */
	public static Optional<String> getAsOptionalString(JsonElement jElement) {
		try {
			return Optional.of(getAsString(jElement));
		} catch (OpenemsNamedException e) {
			return Optional.empty();
		}
	}

	/**
	 * Gets the member of the {@link JsonElement} as {@link Optional}
	 * {@link String}.
	 *
	 * @param jElement   the {@link JsonElement}
	 * @param memberName the name of the member
	 * @return the {@link Optional} {@link String} value
	 * @throws OpenemsNamedException on error
	 */
	public static Optional<String> getAsOptionalString(JsonElement jElement, String memberName) {
		try {
			return Optional.of(getAsString(jElement, memberName));
		} catch (OpenemsNamedException e) {
			return Optional.empty();
		}
	}

	/**
	 * Converts a {@link JsonArray} to a String Array.
	 *
	 * @param json the {@link JsonArray}
	 * @return a String Array
	 * @throws OpenemsNamedException on error
	 */
	public static String[] getAsStringArray(JsonArray json) throws OpenemsNamedException {
		var result = new String[json.size()];
		var i = 0;
		for (JsonElement element : json) {
			result[i++] = JsonUtils.getAsString(element);
		}
		return result;
	}

	/**
	 * Gets the {@link JsonPrimitive} as {@link Boolean}.
	 *
	 * @param jPrimitive the {@link JsonPrimitive}
	 * @return the {@link Boolean} value
	 * @throws OpenemsNamedException on error
	 */
	public static boolean getAsBoolean(JsonPrimitive jPrimitive) throws OpenemsNamedException {
		if (jPrimitive.isBoolean()) {
			return jPrimitive.getAsBoolean();
		}
		if (jPrimitive.isString()) {
			var element = jPrimitive.getAsString();
			if (element.equalsIgnoreCase("false")) {
				return false;
			}
			if (element.equalsIgnoreCase("true")) {
				return true;
			}
		}
		throw OpenemsError.JSON_NO_BOOLEAN.exception(jPrimitive.toString().replace("%", "%%"));
	}

	/**
	 * Gets the {@link JsonElement} as {@link Boolean}.
	 *
	 * @param jElement the {@link JsonElement}
	 * @return the {@link Boolean} value
	 * @throws OpenemsNamedException on error
	 */
	public static boolean getAsBoolean(JsonElement jElement) throws OpenemsNamedException {
		return getAsBoolean(getAsPrimitive(jElement));
	}

	/**
	 * Gets the member of the {@link JsonElement} as {@link Boolean}.
	 *
	 * @param jElement   the {@link JsonElement}
	 * @param memberName the name of the member
	 * @return the {@link Boolean} value
	 * @throws OpenemsNamedException on error
	 */
	public static boolean getAsBoolean(JsonElement jElement, String memberName) throws OpenemsNamedException {
		var jPrimitive = getAsPrimitive(jElement, memberName);
		return getAsBoolean(jPrimitive);
	}

	/**
	 * Gets the member of the {@link JsonElement} as an {@link Optional}
	 * {@link Boolean}.
	 *
	 * @param jElement   the {@link JsonElement}
	 * @param memberName the name of the member
	 * @return the {@link Optional} {@link Boolean} value
	 * @throws OpenemsNamedException on error
	 */
	public static Optional<Boolean> getAsOptionalBoolean(JsonElement jElement, String memberName) {
		try {
			return Optional.of(getAsBoolean(jElement, memberName));
		} catch (OpenemsNamedException e) {
			return Optional.empty();
		}
	}

	/**
	 * Gets the {@link JsonPrimitive} as short.
	 *
	 * @param jPrimitive the {@link JsonPrimitive}
	 * @return the short value
	 * @throws OpenemsNamedException on error
	 */
	public static short getAsShort(JsonPrimitive jPrimitive) throws OpenemsNamedException {
		if (jPrimitive.isNumber()) {
			return jPrimitive.getAsShort();
		}
		if (jPrimitive.isString()) {
			var string = jPrimitive.getAsString();
			return Short.parseShort(string);
		}
		throw OpenemsError.JSON_NO_INTEGER.exception(jPrimitive.toString().replace("%", "%%"));
	}

	/**
	 * Gets the {@link JsonElement} as short.
	 *
	 * @param jElement the {@link JsonElement}
	 * @return the short value
	 * @throws OpenemsNamedException on error
	 */
	public static short getAsShort(JsonElement jElement) throws OpenemsNamedException {
		return getAsShort(getAsPrimitive(jElement));
	}

	/**
	 * Gets the member of the {@link JsonElement} as short.
	 *
	 * @param jElement   the {@link JsonElement}
	 * @param memberName the name of the member
	 * @return the short value
	 * @throws OpenemsNamedException on error
	 */
	public static short getAsShort(JsonElement jElement, String memberName) throws OpenemsNamedException {
		return getAsShort(getAsPrimitive(jElement, memberName));
	}

	/**
	 * Gets the {@link JsonPrimitive} as int.
	 *
	 * @param jPrimitive the {@link JsonPrimitive}
	 * @return the int value
	 * @throws OpenemsNamedException on error
	 */
	public static int getAsInt(JsonPrimitive jPrimitive) throws OpenemsNamedException {
		if (jPrimitive.isNumber()) {
			return jPrimitive.getAsInt();
		}
		if (jPrimitive.isString()) {
			var string = jPrimitive.getAsString();
			return Integer.parseInt(string);
		}
		throw OpenemsError.JSON_NO_INTEGER.exception(jPrimitive.toString().replace("%", "%%"));
	}

	/**
	 * Gets the {@link JsonElement} as int.
	 *
	 * @param jElement the {@link JsonElement}
	 * @return the int value
	 * @throws OpenemsNamedException on error
	 */
	public static int getAsInt(JsonElement jElement) throws OpenemsNamedException {
		return getAsInt(getAsPrimitive(jElement));
	}

	/**
	 * Gets the member of the {@link JsonElement} as int.
	 *
	 * @param jElement   the {@link JsonElement}
	 * @param memberName the name of the member
	 * @return the int value
	 * @throws OpenemsNamedException on error
	 */
	public static int getAsInt(JsonElement jElement, String memberName) throws OpenemsNamedException {
		return getAsInt(getAsPrimitive(jElement, memberName));
	}

	/**
	 * Gets the member with given index of the {@link JsonArray} as int.
	 *
	 * @param jArray the {@link JsonArray}
	 * @param index  the index of the member
	 * @return the int value
	 * @throws OpenemsNamedException on error
	 */
	public static int getAsInt(JsonArray jArray, int index) throws OpenemsNamedException {
		if (index < 0 || jArray.size() <= index) {
			throw OpenemsError.JSON_NO_INTEGER_MEMBER.exception(index, jArray.toString().replace("%", "%%"));
		}
		return JsonUtils.getAsInt(jArray.get(index));
	}

	/**
	 * Gets the {@link JsonElement} as {@link Optional} {@link Integer}.
	 *
	 * @param jElement the {@link JsonElement}
	 * @return the {@link Optional} {@link Integer} value
	 * @throws OpenemsNamedException on error
	 */
	public static Optional<Integer> getAsOptionalInt(JsonElement jElement) {
		try {
			return Optional.of(getAsInt(jElement));
		} catch (OpenemsNamedException e) {
			return Optional.empty();
		}
	}

	/**
	 * Gets the member of the {@link JsonElement} as {@link Optional}
	 * {@link Integer}.
	 *
	 * @param jElement   the {@link JsonElement}
	 * @param memberName the name of the member
	 * @return the {@link Optional} {@link Integer} value
	 * @throws OpenemsNamedException on error
	 */
	public static Optional<Integer> getAsOptionalInt(JsonElement jElement, String memberName) {
		try {
			return Optional.of(getAsInt(jElement, memberName));
		} catch (OpenemsNamedException e) {
			return Optional.empty();
		}
	}

	/**
	 * Gets the {@link JsonPrimitive} as long.
	 *
	 * @param jPrimitive the {@link JsonPrimitive}
	 * @return the long value
	 * @throws OpenemsNamedException on error
	 */
	public static long getAsLong(JsonPrimitive jPrimitive) throws OpenemsNamedException {
		if (jPrimitive.isNumber()) {
			return jPrimitive.getAsLong();
		}
		if (jPrimitive.isString()) {
			var string = jPrimitive.getAsString();
			return Integer.parseInt(string);
		}
		throw OpenemsError.JSON_NO_NUMBER.exception(jPrimitive.toString().replace("%", "%%"));
	}

	/**
	 * Gets the {@link JsonElement} as long.
	 *
	 * @param jElement the {@link JsonElement}
	 * @return the long value
	 * @throws OpenemsNamedException on error
	 */
	public static long getAsLong(JsonElement jElement) throws OpenemsNamedException {
		return getAsLong(getAsPrimitive(jElement));
	}

	/**
	 * Gets the member of the {@link JsonElement} as long.
	 *
	 * @param jElement   the {@link JsonElement}
	 * @param memberName the name of the member
	 * @return the long value
	 * @throws OpenemsNamedException on error
	 */
	public static long getAsLong(JsonElement jElement, String memberName) throws OpenemsNamedException {
		return getAsLong(getAsPrimitive(jElement, memberName));
	}

	/**
	 * Gets the member of the {@link JsonElement} as {@link Optional} {@link Long}.
	 *
	 * @param jElement   the {@link JsonElement}
	 * @param memberName the name of the member
	 * @return the {@link Optional} {@link Long} value
	 * @throws OpenemsNamedException on error
	 */
	public static Optional<Long> getAsOptionalLong(JsonElement jElement, String memberName) {
		try {
			return Optional.of(getAsLong(jElement, memberName));
		} catch (OpenemsNamedException e) {
			return Optional.empty();
		}
	}

	/**
	 * Gets the {@link JsonPrimitive} as {@link Float}.
	 *
	 * @param jPrimitive the {@link JsonPrimitive}
	 * @return the {@link Float} value
	 * @throws OpenemsNamedException on error
	 */
	public static float getAsFloat(JsonPrimitive jPrimitive) throws OpenemsNamedException {
		if (jPrimitive.isNumber()) {
			return jPrimitive.getAsFloat();
		}
		if (jPrimitive.isString()) {
			var string = jPrimitive.getAsString();
			return Float.parseFloat(string);
		}
		throw OpenemsError.JSON_NO_FLOAT.exception(jPrimitive.toString().replace("%", "%%"));
	}

	/**
	 * Gets the {@link JsonElement} as {@link Float}.
	 *
	 * @param jElement the {@link JsonElement}
	 * @return the {@link Float} value
	 * @throws OpenemsNamedException on error
	 */
	public static float getAsFloat(JsonElement jElement) throws OpenemsNamedException {
		return getAsFloat(getAsPrimitive(jElement));
	}

	/**
	 * Gets the member of the {@link JsonElement} as {@link Float}.
	 *
	 * @param jElement   the {@link JsonElement}
	 * @param memberName the name of the member
	 * @return the {@link Float} value
	 * @throws OpenemsNamedException on error
	 */
	public static float getAsFloat(JsonElement jElement, String memberName) throws OpenemsNamedException {
		return getAsFloat(getAsPrimitive(jElement, memberName));
	}

	/**
	 * Gets the {@link JsonPrimitive} as {@link Double}.
	 *
	 * @param jPrimitive the {@link JsonPrimitive}
	 * @return the {@link Double} value
	 * @throws OpenemsNamedException on error
	 */
	public static double getAsDouble(JsonPrimitive jPrimitive) throws OpenemsNamedException {
		if (jPrimitive.isNumber()) {
			return jPrimitive.getAsDouble();
		}
		if (jPrimitive.isString()) {
			var string = jPrimitive.getAsString();
			return Double.parseDouble(string);
		}
		throw OpenemsError.JSON_NO_INTEGER.exception(jPrimitive.toString().replace("%", "%%"));
	}

	/**
	 * Gets the {@link JsonElement} as {@link Double}.
	 *
	 * @param jElement the {@link JsonElement}
	 * @return the {@link Double} value
	 * @throws OpenemsNamedException on error
	 */
	public static double getAsDouble(JsonElement jElement) throws OpenemsNamedException {
		return getAsDouble(getAsPrimitive(jElement));
	}

	/**
	 * Gets the member of the {@link JsonElement} as {@link Double}.
	 *
	 * @param jElement   the {@link JsonElement}
	 * @param memberName the name of the member
	 * @return the {@link Double} value
	 * @throws OpenemsNamedException on error
	 */
	public static double getAsDouble(JsonElement jElement, String memberName) throws OpenemsNamedException {
		return getAsDouble(getAsPrimitive(jElement, memberName));
	}

	/**
	 * Gets the {@link JsonElement} as {@link Enum}.
	 *
	 * @param <E>      the {@link Enum} type
	 * @param enumType the class of the {@link Enum}
	 * @param jElement the {@link JsonElement}
	 * @return the {@link Enum} value
	 * @throws OpenemsNamedException on error
	 */
	public static <E extends Enum<E>> E getAsEnum(Class<E> enumType, JsonElement jElement)
			throws OpenemsNamedException {
		var element = getAsString(jElement);
		try {
			return Enum.valueOf(enumType, element);
		} catch (IllegalArgumentException e) {
			throw OpenemsError.JSON_NO_ENUM.exception(element);
		}
	}

	/**
	 * Gets the member of the {@link JsonElement} as {@link Enum}.
	 *
	 * @param <E>        the {@link Enum} type
	 * @param enumType   the class of the {@link Enum}
	 * @param jElement   the {@link JsonElement}
	 * @param memberName the name of the member
	 * @return the {@link Enum} value
	 * @throws OpenemsNamedException on error
	 */
	public static <E extends Enum<E>> E getAsEnum(Class<E> enumType, JsonElement jElement, String memberName)
			throws OpenemsNamedException {
		var element = getAsString(jElement, memberName);
		try {
			return Enum.valueOf(enumType, element);
		} catch (IllegalArgumentException e) {
			throw OpenemsError.JSON_NO_ENUM_MEMBER.exception(memberName, element);
		}
	}

	/**
	 * Gets the member of the {@link JsonElement} as {@link Optional} {@link Enum}.
	 *
	 * @param <E>        the {@link Enum} type
	 * @param enumType   the class of the {@link Enum}
	 * @param jElement   the {@link JsonElement}
	 * @param memberName the name of the member
	 * @return the {@link Optional} {@link Enum} value
	 * @throws OpenemsNamedException on error
	 */
	public static <E extends Enum<E>> Optional<E> getAsOptionalEnum(Class<E> enumType, JsonElement jElement,
			String memberName) {
		var elementOpt = getAsOptionalString(jElement, memberName);
		if (!elementOpt.isPresent()) {
			return Optional.empty();
		}
		try {
			return Optional.ofNullable(Enum.valueOf(enumType, elementOpt.get()));
		} catch (IllegalArgumentException e) {
			return Optional.empty();
		}
	}

	/**
	 * Gets the {@link JsonElement} as {@link Inet4Address}.
	 *
	 * @param jElement the {@link JsonElement}
	 * @return the {@link Inet4Address} value
	 * @throws OpenemsNamedException on error
	 */
	public static Inet4Address getAsInet4Address(JsonElement jElement) throws OpenemsNamedException {
		try {
			return (Inet4Address) InetAddress.getByName(getAsString(jElement));
		} catch (UnknownHostException e) {
			throw OpenemsError.JSON_NO_INET4ADDRESS.exception(jElement.toString().replace("%", "%%"));
		}
	}

	/**
	 * Gets the member of the {@link JsonElement} as {@link Optional}
	 * {@link Inet4Address}.
	 *
	 * @param jElement   the {@link JsonElement}
	 * @param memberName the name of the member
	 * @return the {@link Optional} {@link Inet4Address} value
	 * @throws OpenemsNamedException on error
	 */
	public static Optional<Inet4Address> getAsOptionalInet4Address(JsonElement jElement, String memberName) {
		try {
			return Optional.ofNullable((Inet4Address) InetAddress.getByName(getAsString(jElement, memberName)));
		} catch (OpenemsNamedException | UnknownHostException e) {
			return Optional.empty();
		}
	}

	/**
	 * Gets the member of the {@link JsonElement} as {@link UUID}.
	 *
	 * @param jElement   the {@link JsonElement}
	 * @param memberName the name of the member
	 * @return the {@link UUID} value
	 * @throws OpenemsNamedException on error
	 */
	// CHECKSTYLE:OFF
	public static UUID getAsUUID(JsonElement jElement, String memberName) throws OpenemsNamedException {
		// CHECKSTYLE:ON
		try {
			return UUID.fromString(getAsString(jElement, memberName));
		} catch (IllegalArgumentException e) {
			throw new OpenemsException("Unable to parse UUID: " + e.getMessage());
		}
	}

	/**
	 * Gets the member of the {@link JsonElement} as {@link Optional} {@link UUID}.
	 *
	 * @param jElement   the {@link JsonElement}
	 * @param memberName the name of the member
	 * @return the {@link Optional} {@link UUID} value
	 * @throws OpenemsNamedException on error
	 */
	// CHECKSTYLE:OFF
	public static Optional<UUID> getAsOptionalUUID(JsonElement jElement, String memberName) {
		// CHECKSTYLE:ON
		var uuid = getAsOptionalString(jElement, memberName);
		if (uuid.isPresent()) {
			return Optional.ofNullable(UUID.fromString(uuid.get()));
		}
		return Optional.empty();
	}

	/**
	 * Tries to find the best matching Object representation of the given
	 * {@link JsonElement}.
	 *
	 * @param j the {@link JsonElement}
	 * @return the Object
	 * @throws OpenemsNamedException on error
	 */
	public static Object getAsBestType(JsonElement j) throws OpenemsNamedException {
		try {
			if (j.isJsonArray()) {
				var jA = (JsonArray) j;
				if (jA.size() == 0) {
					return new Object[0];
				}
				// identify the array type (boolean, int or String)
				var isBoolean = true;
				var isInt = true;
				for (JsonElement jE : jA) {
					if (!jE.isJsonPrimitive()) {
						isBoolean = false;
						isInt = false;
						break;
					}
					var jP = jE.getAsJsonPrimitive();
					if (isBoolean && !jP.isBoolean()) {
						isBoolean = false;
					}
					if (isInt && !jP.isNumber()) {
						isInt = false;
					}
				}
				if (isBoolean) {
					// convert to boolean array
					var result = new boolean[jA.size()];
					for (var i = 0; i < jA.size(); i++) {
						result[i] = jA.get(i).getAsBoolean();
					}
					return result;
				}
				if (isInt) {
					// convert to int array
					var result = new int[jA.size()];
					for (var i = 0; i < jA.size(); i++) {
						result[i] = jA.get(i).getAsInt();
					}
					return result;
				} else {
					// convert to string array
					var result = new String[jA.size()];
					for (var i = 0; i < jA.size(); i++) {
						var jE = jA.get(i);
						if (!jE.isJsonPrimitive()) {
							result[i] = jE.toString();
						} else {
							result[i] = jE.getAsString();
						}
					}
					return result;
				}
			}
			if (!j.isJsonPrimitive()) {
				return j.toString();
			}
			var jP = j.getAsJsonPrimitive();
			if (jP.isBoolean()) {
				return jP.getAsBoolean();
			}
			if (jP.isNumber()) {
				var n = jP.getAsNumber();
				return n.intValue();
			}
			return j.getAsString();
		} catch (Exception e) {
			throw OpenemsError.JSON_PARSE_ELEMENT_FAILED.exception(j.toString().replace("%", "%%"),
					e.getClass().getSimpleName(), e.getMessage());
		}
	}

	/**
	 * Gets a {@link JsonElement} representing the Object value.
	 *
	 * @param value the {@link Object} value
	 * @return the {@link JsonElement}
	 */
	public static JsonElement getAsJsonElement(Object value) {
		// null
		if (value == null) {
			return JsonNull.INSTANCE;
		}
		// optional
		if (value instanceof Optional<?>) {
			if (!((Optional<?>) value).isPresent()) {
				return JsonNull.INSTANCE;
			}
			value = ((Optional<?>) value).get();
		}
		if (value instanceof Number) {
			/*
			 * Number
			 */
			return new JsonPrimitive((Number) value);
		}
		if (value instanceof String) {
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
		} else if (value instanceof boolean[]) {
			/*
			 * boolean-Array
			 */
			var js = new JsonArray();
			for (boolean b : (boolean[]) value) {
				js.add(new JsonPrimitive(b));
			}
			return js;
		} else if (value instanceof short[]) {
			/*
			 * short-Array
			 */
			var js = new JsonArray();
			for (short s : (short[]) value) {
				js.add(new JsonPrimitive(s));
			}
			return js;
		} else if (value instanceof int[]) {
			/*
			 * int-Array
			 */
			var js = new JsonArray();
			for (int i : (int[]) value) {
				js.add(new JsonPrimitive(i));
			}
			return js;
		} else if (value instanceof long[]) {
			/*
			 * long-Array
			 */
			var js = new JsonArray();
			for (long l : (long[]) value) {
				js.add(new JsonPrimitive(l));
			}
			return js;
		} else if (value instanceof float[]) {
			/*
			 * float-Array
			 */
			var js = new JsonArray();
			for (float f : (float[]) value) {
				js.add(new JsonPrimitive(f));
			}
			return js;
		} else if (value instanceof double[]) {
			/*
			 * double-Array
			 */
			var js = new JsonArray();
			for (double d : (double[]) value) {
				js.add(new JsonPrimitive(d));
			}
			return js;
		} else if (value instanceof String[]) {
			/*
			 * String-Array
			 */
			var js = new JsonArray();
			var v = (String[]) value;
			if (v.length == 1 && v[0].isEmpty()) {
				// special case: String-Array with one entry which is an empty String. Return an
				// empty JsonArray.
				return js;
			}
			for (String s : v) {
				js.add(new JsonPrimitive(s));
			}
			return js;
		} else if (value instanceof Object[]) {
			/*
			 * Object-Array
			 */
			var js = new JsonArray();
			for (Object o : (Object[]) value) {
				js.add(JsonUtils.getAsJsonElement(o));
			}
			return js;
		} else {
			/*
			 * Use toString()-method
			 */
			JsonUtils.LOG.warn("Converter for [" + value + "]" + " of type [" + value.getClass().getSimpleName()
					+ "] to JSON is not implemented.");
			return new JsonPrimitive(value.toString());
		}
	}

	/**
	 * Gets a {@link JsonElement} as the given type.
	 *
	 * @param type the class of the type
	 * @param j    the {@link JsonElement}
	 * @return an Object of the given type
	 */
	public static Object getAsType(Class<?> type, JsonElement j) throws NotImplementedException {
		try {
			if (Integer.class.isAssignableFrom(type)) {
				/*
				 * Asking for an Integer
				 */
				return j.getAsInt();

			}
			if (Long.class.isAssignableFrom(type)) {
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
						var js = j.getAsJsonArray();
						var la = new Long[js.size()];
						for (var i = 0; i < js.size(); i++) {
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

	/**
	 * Gets a {@link JsonElement} as the given {@link OpenemsType}.
	 *
	 * @param type the {@link OpenemsType}
	 * @param j    the {@link JsonElement}
	 * @return an Object of the given type
	 */
	public static Object getAsType(OpenemsType type, JsonElement j) throws OpenemsNamedException {
		if (j.isJsonNull()) {
			return null;
		}

		if (j.isJsonPrimitive()) {
			switch (type) {
			case BOOLEAN:
				return JsonUtils.getAsBoolean(j);
			case DOUBLE:
				return JsonUtils.getAsDouble(j);
			case FLOAT:
				return JsonUtils.getAsFloat(j);
			case INTEGER:
				return JsonUtils.getAsInt(j);
			case LONG:
				return JsonUtils.getAsLong(j);
			case SHORT:
				return JsonUtils.getAsShort(j);
			case STRING:
				return JsonUtils.getAsString(j);
			}
		}

		if (j.isJsonObject() || j.isJsonArray()) {
			switch (type) {
			case BOOLEAN:
			case DOUBLE:
			case FLOAT:
			case INTEGER:
			case LONG:
			case SHORT:
				break;
			case STRING:
				return j.toString();
			}
		}

		throw new NotImplementedException(
				"Converter for value [" + j + "] to class type [" + type + "] is not implemented.");
	}

	/**
	 * Gets a {@link JsonElement} as the given type.
	 *
	 * @param typeOptional the class of the type
	 * @param j            the {@link JsonElement}
	 * @return an Object of the given type
	 */
	public static Object getAsType(Optional<Class<?>> typeOptional, JsonElement j) throws NotImplementedException {
		if (!typeOptional.isPresent()) {
			throw new NotImplementedException("Type of Channel was not set: " + j.getAsString());
		}
		Class<?> type = typeOptional.get();
		return getAsType(type, j);
	}

	/**
	 * Takes a JSON in the form 'YYYY-MM-DD' and converts it to a
	 * {@link ZonedDateTime} with hour, minute and second set to zero.
	 *
	 * @param element    the {@link JsonElement}
	 * @param memberName the name of the member of the JsonObject
	 * @param timezone   the timezone as {@link ZoneId}
	 * @return the {@link ZonedDateTime}
	 * @throws OpenemsNamedException on parse error
	 */
	public static ZonedDateTime getAsZonedDateTime(JsonElement element, String memberName, ZoneId timezone)
			throws OpenemsNamedException {
		var date = JsonUtils.getAsString(element, memberName).split("-");
		try {
			var year = Integer.parseInt(date[0]);
			var month = Integer.parseInt(date[1]);
			var day = Integer.parseInt(date[2]);
			return ZonedDateTime.of(year, month, day, 0, 0, 0, 0, timezone);
		} catch (ArrayIndexOutOfBoundsException e) {
			throw OpenemsError.JSON_NO_DATE_MEMBER.exception(memberName, element.toString(), e.getMessage());
		}
	}

	/**
	 * Parses a string to a {@link JsonElement}.
	 *
	 * @param string to be parsed
	 * @return the {@link JsonElement}
	 * @throws OpenemsNamedException on error
	 */
	public static JsonElement parse(String string) throws OpenemsNamedException {
		try {
			return JsonParser.parseString(string);
		} catch (JsonParseException e) {
			throw OpenemsError.JSON_PARSE_FAILED.exception(e.getMessage(), string);
		}
	}

	/**
	 * Parses a string to a {@link JsonObject}.
	 *
	 * @param string the String
	 * @return the {@link JsonObject}
	 * @throws OpenemsNamedException on error
	 */
	public static JsonObject parseToJsonObject(String string) throws OpenemsNamedException {
		return JsonUtils.getAsJsonObject(JsonUtils.parse(string));
	}

	/**
	 * Parses a string to a {@link JsonArray}.
	 *
	 * @param string the String
	 * @return the {@link JsonArray}
	 * @throws OpenemsNamedException on error
	 */
	public static JsonArray parseToJsonArray(String string) throws OpenemsNamedException {
		return JsonUtils.getAsJsonArray(JsonUtils.parse(string));
	}

	/**
	 * Pretty print a {@link JsonElement}.
	 *
	 * @param j the {@link JsonElement}
	 */
	public static void prettyPrint(JsonElement j) {
		System.out.println(prettyToString(j));
	}

	/**
	 * Pretty toString()-method for a {@link JsonElement}.
	 *
	 * @param j the {@link JsonElement}
	 */
	public static String prettyToString(JsonElement j) {
		return new GsonBuilder().setPrettyPrinting().create().toJson(j);
	}

	/**
	 * Check if the given {@link JsonElement} is an empty JsonObject {}.
	 *
	 * @param j the {@link JsonElement} to check
	 * @return true if is empty, otherwise false
	 */
	public static boolean isEmptyJsonObject(JsonElement j) {
		if (j != null && j.isJsonObject()) {
			var object = j.getAsJsonObject();
			return object.size() == 0;
		}

		return false;
	}

	/**
	 * Check if the given {@link JsonElement} is an empty JsonArray [].
	 *
	 * @param j the {@link JsonElement} to check
	 * @return true if is empty, otherwise false
	 */
	public static boolean isEmptyJsonArray(JsonElement j) {
		if (j != null && j.isJsonArray()) {
			var array = j.getAsJsonArray();
			return array.size() == 0;
		}

		return false;
	}

}
