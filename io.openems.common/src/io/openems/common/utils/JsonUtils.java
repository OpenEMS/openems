package io.openems.common.utils;

import static io.openems.common.utils.EnumUtils.toEnum;

import java.net.Inet4Address;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.stream.Collector;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Supplier;
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
import io.openems.common.types.OpenemsType;

public final class JsonUtils {

	private JsonUtils() {
	}

	/**
	 * Provide a easy way to generate a JsonArray from a list using the given
	 * convert function to add each element.
	 *
	 * @param list    to convert
	 * @param convert function to convert elements
	 * @param <T>     type of an element from list
	 *
	 * @return list as JsonArray
	 */
	public static <T> JsonArray generateJsonArray(Collection<T> list, Function<T, JsonElement> convert) {
		if (list == null) {
			return null;
		}
		var jab = new JsonArrayBuilder(list.size());
		list.forEach(e -> jab.add(convert.apply(e)));
		return jab.build();
	}

	/**
	 * Provide a easy way to generate a JsonArray from a collection of JsonElements.
	 *
	 * @param <T>  type of element from list
	 * @param list to convert
	 * @return list as JsonArray
	 */
	public static <T extends JsonElement> JsonArray generateJsonArray(Collection<T> list) {
		return generateJsonArray(list, json -> json);
	}

	/**
	 * A temporary builder class for JsonArrays.
	 */
	public static class JsonArrayBuilder {

		private final JsonArray j;

		protected JsonArrayBuilder() {
			this(new JsonArray());
		}

		protected JsonArrayBuilder(int capacity) {
			this(new JsonArray(capacity));
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
		 * Add a float value to the {@link JsonArray}.
		 *
		 * @param value the value
		 * @return the {@link JsonArrayBuilder}
		 */
		public JsonArrayBuilder add(float value) {
			this.j.add(value);
			return this;
		}

		/**
		 * Add a double value to the {@link JsonArray}.
		 *
		 * @param value the value
		 * @return the {@link JsonArrayBuilder}
		 */
		public JsonArrayBuilder add(double value) {
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
		 * Add a {@link Enum} value to the {@link JsonObject}.
		 *
		 * @param property the key
		 * @param value    the value
		 * @return the {@link JsonObjectBuilder}
		 */
		public JsonObjectBuilder addProperty(String property, Enum<?> value) {
			this.j.addProperty(property, value == null ? null : value.name());
			return this;
		}

		/**
		 * Add a {@link ZonedDateTime} value to the {@link JsonObject}.
		 *
		 * <p>
		 * The value gets added in the format of {@link DateTimeFormatter#ISO_INSTANT}.
		 *
		 * @param property the key
		 * @param value    the value
		 * @return the {@link JsonObjectBuilder}
		 */
		public JsonObjectBuilder addProperty(String property, ZonedDateTime value) {
			if (value != null) {
				this.j.addProperty(property, value.format(DateTimeFormatter.ISO_INSTANT));
			}
			return this;
		}

		/**
		 * Add a {@link LocalDateTime} value to the {@link JsonObject}.
		 *
		 * <p>
		 * The value gets added in the format of
		 * {@link DateTimeFormatter#ISO_LOCAL_DATE_TIME}.
		 *
		 * @param property the key
		 * @param value    the value
		 * @return the {@link JsonObjectBuilder}
		 */
		public JsonObjectBuilder addProperty(String property, LocalDateTime value) {
			if (value != null) {
				this.j.addProperty(property, value.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
			}
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
		 * Add a {@link Enum} value to the {@link JsonObject} if it is not null.
		 *
		 * @param property the key
		 * @param value    the value
		 * @return the {@link JsonObjectBuilder}
		 */
		public JsonObjectBuilder addPropertyIfNotNull(String property, Enum<?> value) {
			if (value != null) {
				this.j.addProperty(property, value.name());
			}
			return this;
		}

		/**
		 * Add a {@link ZonedDateTime} value to the {@link JsonObject} if it is not
		 * null.
		 *
		 * <p>
		 * The value gets added in the format of {@link DateTimeFormatter#ISO_INSTANT}.
		 *
		 * @param property the key
		 * @param value    the value
		 * @return the {@link JsonObjectBuilder}
		 */
		public JsonObjectBuilder addPropertyIfNotNull(String property, ZonedDateTime value) {
			if (value != null) {
				this.addProperty(property, value);
			}
			return this;
		}

		/**
		 * Call a method on a JsonObjectBuilder if an expression is true.
		 *
		 * @param expression     the expression
		 * @param ifTrueCallback allows a lambda function on {@link JsonObjectBuilder}
		 * @return the {@link JsonObjectBuilder}
		 */
		public JsonObjectBuilder onlyIf(boolean expression, Consumer<JsonObjectBuilder> ifTrueCallback) {
			if (expression) {
				ifTrueCallback.accept(this);
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

	public static class JsonArrayCollector implements Collector<JsonElement, JsonArrayBuilder, JsonArray> {

		@Override
		public Set<Characteristics> characteristics() {
			return Set.of();
		}

		@Override
		public Supplier<JsonArrayBuilder> supplier() {
			return JsonUtils::buildJsonArray;
		}

		@Override
		public BiConsumer<JsonArrayBuilder, JsonElement> accumulator() {
			return JsonArrayBuilder::add;
		}

		@Override
		public BinaryOperator<JsonArrayBuilder> combiner() {
			return (t, u) -> {
				u.build().forEach(t::add);
				return t;
			};
		}

		@Override
		public Function<JsonArrayBuilder, JsonArray> finisher() {
			return JsonArrayBuilder::build;
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
	 * Creates a JsonElement from the {@link Number} value.
	 *
	 * @param number the {@link Number}
	 * @return a {@link JsonPrimitive} or {@link JsonNull}
	 */
	public static JsonElement toJson(Number number) {
		if (number == null) {
			return JsonNull.INSTANCE;
		}
		return new JsonPrimitive(number);
	}

	/**
	 * Creates a JsonElement from the {@link Boolean} value.
	 *
	 * @param bool the {@link Boolean}
	 * @return a {@link JsonPrimitive} or {@link JsonNull}
	 */
	public static JsonElement toJson(Boolean bool) {
		if (bool == null) {
			return JsonNull.INSTANCE;
		}
		return new JsonPrimitive(bool);
	}

	/**
	 * Creates a JsonElement from the {@link Boolean} value.
	 *
	 * @param c the {@link Character}
	 * @return a {@link JsonPrimitive} or {@link JsonNull}
	 */
	public static JsonElement toJson(Character c) {
		if (c == null) {
			return JsonNull.INSTANCE;
		}
		return new JsonPrimitive(c);
	}

	/**
	 * Creates a JsonElement from the {@link Boolean} value.
	 *
	 * @param string the {@link String}
	 * @return a {@link JsonPrimitive} or {@link JsonNull}
	 */
	public static JsonElement toJson(String string) {
		if (string == null) {
			return JsonNull.INSTANCE;
		}
		return new JsonPrimitive(string);
	}

	/**
	 * Gets the {@link JsonElement} as {@link JsonPrimitive}.
	 *
	 * @param jElement the {@link JsonElement}
	 * @return the {@link JsonPrimitive} value
	 * @throws OpenemsNamedException on error
	 */
	public static JsonPrimitive getAsPrimitive(JsonElement jElement) throws OpenemsNamedException {
		var value = toPrimitive(jElement);
		if (value != null) {
			return value;
		}
		throw OpenemsError.JSON_NO_PRIMITIVE.exception(jElement.toString().replace("%", "%%"));
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
		var value = toPrimitive(toSubElement(jElement, memberName));
		if (value != null) {
			return value;
		}
		throw OpenemsError.JSON_NO_PRIMITIVE_MEMBER.exception(memberName, jElement.toString().replace("%", "%%"));
	}

	/**
	 * Gets the member of the {@link JsonElement} as {@link Optional}
	 * {@link JsonPrimitive}.
	 *
	 * @param jElement   the {@link JsonElement}
	 * @param memberName the name of the member
	 * @return the {@link Optional} {@link JsonPrimitive} value
	 */
	public static Optional<JsonPrimitive> getAsOptionalPrimitive(JsonElement jElement, String memberName) {
		return Optional.ofNullable(toPrimitive(toSubElement(jElement, memberName)));
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
		var value = toSubElement(jElement, memberName);
		if (value != null) {
			return value;
		}
		throw OpenemsError.JSON_HAS_NO_MEMBER.exception(memberName,
				StringUtils.toShortString(jElement, 100).replace("%", "%%"));
	}

	/**
	 * Gets the member of the {@link JsonElement} as {@link Optional}
	 * {@link JsonElement}.
	 *
	 * @param jElement   the {@link JsonElement}
	 * @param memberName the name of the member
	 * @return the {@link Optional} {@link JsonElement} value
	 */
	public static Optional<JsonElement> getOptionalSubElement(JsonElement jElement, String memberName) {
		return Optional.ofNullable(toSubElement(jElement, memberName));
	}

	/**
	 * Gets the {@link JsonElement} as {@link JsonObject}.
	 *
	 * @param jElement the {@link JsonElement}
	 * @return the {@link JsonObject} value
	 * @throws OpenemsNamedException on error
	 */
	public static JsonObject getAsJsonObject(JsonElement jElement) throws OpenemsNamedException {
		var value = toJsonObject(jElement);
		if (value != null) {
			return value;
		}
		throw OpenemsError.JSON_NO_OBJECT.exception(StringUtils.toShortString(jElement, 100).replace("%", "%%"));
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
		var value = toJsonObject(toSubElement(jElement, memberName));
		if (value != null) {
			return value;
		}
		throw OpenemsError.JSON_NO_OBJECT_MEMBER.exception(memberName,
				StringUtils.toShortString(jElement, 100).replace("%", "%%"));
	}

	/**
	 * Gets the {@link JsonElement} as {@link Optional} {@link JsonObject}.
	 *
	 * @param jElement the {@link JsonElement}
	 * @return the {@link Optional} {@link JsonObject} value
	 */
	public static Optional<JsonObject> getAsOptionalJsonObject(JsonElement jElement) {
		return Optional.ofNullable(toJsonObject(jElement));
	}

	/**
	 * Gets the member of the {@link JsonElement} as {@link Optional}
	 * {@link JsonObject}.
	 *
	 * @param jElement   the {@link JsonElement}
	 * @param memberName the name of the member
	 * @return the {@link Optional} {@link JsonObject} value
	 */
	public static Optional<JsonObject> getAsOptionalJsonObject(JsonElement jElement, String memberName) {
		return Optional.ofNullable(toJsonObject(toSubElement(jElement, memberName)));
	}

	/**
	 * Gets the {@link JsonElement} as {@link JsonArray}.
	 *
	 * @param jElement the {@link JsonElement}
	 * @return the {@link JsonArray} value
	 * @throws OpenemsNamedException on error
	 */
	public static JsonArray getAsJsonArray(JsonElement jElement) throws OpenemsNamedException {
		var value = toJsonArray(jElement);
		if (value != null) {
			return value;
		}
		throw OpenemsError.JSON_NO_ARRAY.exception(StringUtils.toShortString(jElement, 100).replace("%", "%%"));
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
		var value = toJsonArray(toSubElement(jElement, memberName));
		if (value != null) {
			return value;
		}
		throw OpenemsError.JSON_NO_ARRAY_MEMBER.exception(memberName, jElement.toString().replace("%", "%%"));
	}

	/**
	 * Gets the member of the {@link JsonElement} as {@link Optional}
	 * {@link JsonArray}.
	 *
	 * @param jElement the {@link JsonElement}
	 * @return the {@link Optional} {@link JsonArray} value
	 */
	public static Optional<JsonArray> getAsOptionalJsonArray(JsonElement jElement) {
		return Optional.ofNullable(toJsonArray(jElement));
	}

	/**
	 * Gets the member of the {@link JsonElement} as {@link Optional}
	 * {@link JsonArray}.
	 *
	 * @param jElement   the {@link JsonElement}
	 * @param memberName the name of the member
	 * @return the {@link Optional} {@link JsonArray} value
	 */
	public static Optional<JsonArray> getAsOptionalJsonArray(JsonElement jElement, String memberName) {
		return Optional.ofNullable(toJsonArray(toSubElement(jElement, memberName)));
	}

	/**
	 * Gets the {@link JsonElement} as {@link String}.
	 *
	 * @param jElement the {@link JsonElement}
	 * @return the {@link String} value
	 * @throws OpenemsNamedException on error
	 */

	public static String getAsString(JsonElement jElement) throws OpenemsNamedException {
		var value = toString(toPrimitive(jElement));
		if (value != null) {
			return value;
		}
		throw OpenemsError.JSON_NO_STRING.exception(jElement.toString().replace("%", "%%"));
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
		var value = toString(toPrimitive(toSubElement(jElement, memberName)));
		if (value != null) {
			return value;
		}
		throw OpenemsError.JSON_NO_STRING_MEMBER.exception(memberName, jElement.toString().replace("%", "%%"));
	}

	/**
	 * Gets the {@link JsonElement} as {@link Optional} {@link String}.
	 *
	 * @param jElement the {@link JsonElement}
	 * @return the {@link Optional} {@link String} value
	 */
	public static Optional<String> getAsOptionalString(JsonElement jElement) {
		return Optional.ofNullable(toString(toPrimitive(jElement)));
	}

	/**
	 * Gets the member of the {@link JsonElement} as {@link Optional}
	 * {@link String}.
	 *
	 * @param jElement   the {@link JsonElement}
	 * @param memberName the name of the member
	 * @return the {@link Optional} {@link String} value
	 */
	public static Optional<String> getAsOptionalString(JsonElement jElement, String memberName) {
		return Optional.ofNullable(toString(toPrimitive(toSubElement(jElement, memberName))));
	}

	/**
	 * Gets the member of the {@link JsonElement} as {@link String} if it exists,
	 * and the alternative value otherwise.
	 *
	 * @param jElement    the {@link JsonElement}
	 * @param memberName  the name of the member
	 * @param alternative the alternative value
	 * @return the {@link String} value or the alternative value
	 */
	public static String getAsStringOrElse(JsonElement jElement, String memberName, String alternative) {
		return getAsOptionalString(jElement, memberName).orElse(alternative);
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
		for (JsonElement jElement : json) {
			var value = toString(toPrimitive(jElement));
			if (value == null) {
				throw OpenemsError.JSON_NO_STRING_ARRAY.exception(json.toString().replace("%", "%%"));
			}
			result[i++] = value;
		}
		return result;
	}

	/**
	 * Gets the {@link JsonElement} as {@link Boolean}.
	 *
	 * @param jElement the {@link JsonElement}
	 * @return the {@link Boolean} value
	 * @throws OpenemsNamedException on error
	 */
	public static boolean getAsBoolean(JsonElement jElement) throws OpenemsNamedException {
		var value = toBoolean(toPrimitive(jElement));
		if (value != null) {
			return value;
		}
		throw OpenemsError.JSON_NO_BOOLEAN.exception(jElement.toString().replace("%", "%%"));
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
		var value = toBoolean(toPrimitive(toSubElement(jElement, memberName)));
		if (value != null) {
			return value;
		}
		throw OpenemsError.JSON_NO_BOOLEAN_MEMBER.exception(memberName, jElement.toString().replace("%", "%%"));
	}

	/**
	 * Gets the {@link JsonElement} as an {@link Optional} {@link Boolean}.
	 *
	 * @param jElement the {@link JsonElement}
	 * @return the {@link Optional} {@link Boolean} value
	 */
	public static Optional<Boolean> getAsOptionalBoolean(JsonElement jElement) {
		return Optional.ofNullable(toBoolean(toPrimitive(jElement)));
	}

	/**
	 * Gets the member of the {@link JsonElement} as an {@link Optional}
	 * {@link Boolean}.
	 *
	 * @param jElement   the {@link JsonElement}
	 * @param memberName the name of the member
	 * @return the {@link Optional} {@link Boolean} value
	 */
	public static Optional<Boolean> getAsOptionalBoolean(JsonElement jElement, String memberName) {
		return Optional.ofNullable(toBoolean(toPrimitive(toSubElement(jElement, memberName))));
	}

	/**
	 * Gets the {@link JsonElement} as short.
	 *
	 * @param jElement the {@link JsonElement}
	 * @return the short value
	 * @throws OpenemsNamedException on error
	 */
	public static short getAsShort(JsonElement jElement) throws OpenemsNamedException {
		var value = toShort(toPrimitive(jElement));
		if (value != null) {
			return value;
		}
		throw OpenemsError.JSON_NO_SHORT.exception(jElement.toString().replace("%", "%%"));
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
		var value = toShort(toPrimitive(toSubElement(jElement, memberName)));
		if (value != null) {
			return value;
		}
		throw OpenemsError.JSON_NO_SHORT_MEMBER.exception(memberName, jElement.toString().replace("%", "%%"));
	}

	/**
	 * Gets the {@link JsonElement} as an {@link Optional} {@link Short}.
	 *
	 * @param jElement the {@link JsonElement}
	 * @return the {@link Optional} {@link Short} value
	 */
	public static Optional<Short> getAsOptionalShort(JsonElement jElement) {
		return Optional.ofNullable(toShort(toPrimitive(jElement)));
	}

	/**
	 * Gets the member of the {@link JsonElement} as an {@link Optional}
	 * {@link Short}.
	 *
	 * @param jElement   the {@link JsonElement}
	 * @param memberName the name of the member
	 * @return the {@link Optional} {@link Boolean} value
	 */
	public static Optional<Short> getAsOptionalShort(JsonElement jElement, String memberName) {
		return Optional.ofNullable(toShort(toPrimitive(toSubElement(jElement, memberName))));
	}

	/**
	 * Gets the {@link JsonElement} as int.
	 *
	 * @param jElement the {@link JsonElement}
	 * @return the int value
	 * @throws OpenemsNamedException on error
	 */
	public static int getAsInt(JsonElement jElement) throws OpenemsNamedException {
		var value = toInt(toPrimitive(jElement));
		if (value != null) {
			return value;
		}
		throw OpenemsError.JSON_NO_INTEGER.exception(jElement.toString().replace("%", "%%"));
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
		var value = toInt(toPrimitive(toSubElement(jElement, memberName)));
		if (value != null) {
			return value;
		}
		throw OpenemsError.JSON_NO_INTEGER_MEMBER.exception(memberName, jElement.toString().replace("%", "%%"));
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
	 */
	public static Optional<Integer> getAsOptionalInt(JsonElement jElement) {
		return Optional.ofNullable(toInt(toPrimitive(jElement)));
	}

	/**
	 * Gets the member of the {@link JsonElement} as {@link Optional}
	 * {@link Integer}.
	 *
	 * @param jElement   the {@link JsonElement}
	 * @param memberName the name of the member
	 * @return the {@link Optional} {@link Integer} value
	 */
	public static Optional<Integer> getAsOptionalInt(JsonElement jElement, String memberName) {
		return Optional.ofNullable(toInt(toPrimitive(toSubElement(jElement, memberName))));
	}

	/**
	 * Gets the {@link JsonElement} as long.
	 *
	 * @param jElement the {@link JsonElement}
	 * @return the long value
	 * @throws OpenemsNamedException on error
	 */
	public static long getAsLong(JsonElement jElement) throws OpenemsNamedException {
		var value = toLong(toPrimitive(jElement));
		if (value != null) {
			return value;
		}
		throw OpenemsError.JSON_NO_LONG.exception(jElement.toString().replace("%", "%%"));
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
		var value = toLong(toPrimitive(toSubElement(jElement, memberName)));
		if (value != null) {
			return value;
		}
		throw OpenemsError.JSON_NO_LONG_MEMBER.exception(memberName, jElement.toString().replace("%", "%%"));
	}

	/**
	 * Gets the {@link JsonElement} as {@link Optional} {@link Long}.
	 *
	 * @param jElement the {@link JsonElement}
	 * @return the {@link Optional} {@link Long} value
	 */
	public static Optional<Long> getAsOptionalLong(JsonElement jElement) {
		return Optional.ofNullable(toLong(toPrimitive(jElement)));
	}

	/**
	 * Gets the member of the {@link JsonElement} as {@link Optional} {@link Long}.
	 *
	 * @param jElement   the {@link JsonElement}
	 * @param memberName the name of the member
	 * @return the {@link Optional} {@link Long} value
	 */
	public static Optional<Long> getAsOptionalLong(JsonElement jElement, String memberName) {
		return Optional.ofNullable(toLong(toPrimitive(toSubElement(jElement, memberName))));
	}

	/**
	 * Gets the {@link JsonElement} as {@link Float}.
	 *
	 * @param jElement the {@link JsonElement}
	 * @return the {@link Float} value
	 * @throws OpenemsNamedException on error
	 */
	public static float getAsFloat(JsonElement jElement) throws OpenemsNamedException {
		var value = toFloat(toPrimitive(jElement));
		if (value != null) {
			return value;
		}
		throw OpenemsError.JSON_NO_FLOAT.exception(jElement.toString().replace("%", "%%"));
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
		var value = toFloat(toPrimitive(toSubElement(jElement, memberName)));
		if (value != null) {
			return value;
		}
		throw OpenemsError.JSON_NO_FLOAT_MEMBER.exception(memberName, jElement.toString().replace("%", "%%"));
	}

	/**
	 * Gets the {@link JsonElement} as {@link Optional} {@link Float}.
	 *
	 * @param jElement the {@link JsonElement}
	 * @return the {@link Optional} {@link Float} value
	 */
	public static Optional<Float> getAsOptionalFloat(JsonElement jElement) {
		return Optional.ofNullable(toFloat(toPrimitive(jElement)));
	}

	/**
	 * Gets the member of the {@link JsonElement} as {@link Optional} {@link Float}.
	 *
	 * @param jElement   the {@link JsonElement}
	 * @param memberName the name of the member
	 * @return the {@link Optional} {@link Float} value
	 */
	public static Optional<Float> getAsOptionalFloat(JsonElement jElement, String memberName) {
		return Optional.ofNullable(toFloat(toPrimitive(toSubElement(jElement, memberName))));
	}

	/**
	 * Gets the {@link JsonElement} as {@link Double}.
	 *
	 * @param jElement the {@link JsonElement}
	 * @return the {@link Double} value
	 * @throws OpenemsNamedException on error
	 */
	public static double getAsDouble(JsonElement jElement) throws OpenemsNamedException {
		var value = toDouble(toPrimitive(jElement));
		if (value != null) {
			return value;
		}
		throw OpenemsError.JSON_NO_DOUBLE.exception(jElement.toString().replace("%", "%%"));
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
		var value = toDouble(toPrimitive(toSubElement(jElement, memberName)));
		if (value != null) {
			return value;
		}
		throw OpenemsError.JSON_NO_DOUBLE_MEMBER.exception(memberName, jElement.toString().replace("%", "%%"));
	}

	/**
	 * Gets the {@link JsonElement} as {@link Optional} {@link Double}.
	 *
	 * @param jElement the {@link JsonElement}
	 * @return the {@link Optional} {@link Double} value
	 */
	public static Optional<Double> getAsOptionalDouble(JsonElement jElement) {
		return Optional.ofNullable(toDouble(toPrimitive(jElement)));
	}

	/**
	 * Gets the member of the {@link JsonElement} as {@link Optional}
	 * {@link Double}.
	 *
	 * @param jElement   the {@link JsonElement}
	 * @param memberName the name of the member
	 * @return the {@link Optional} {@link Double} value
	 */
	public static Optional<Double> getAsOptionalDouble(JsonElement jElement, String memberName) {
		return Optional.ofNullable(toDouble(toPrimitive(toSubElement(jElement, memberName))));
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
		var value = toEnum(enumType, toString(toPrimitive(jElement)));
		if (value != null) {
			return value;
		}
		throw OpenemsError.JSON_NO_ENUM.exception(jElement.toString().replace("%", "%%"));
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
		var value = toEnum(enumType, toString(toPrimitive(toSubElement(jElement, memberName))));
		if (value != null) {
			return value;
		}
		throw OpenemsError.JSON_NO_ENUM_MEMBER.exception(memberName, jElement.toString().replace("%", "%%"));
	}

	/**
	 * Gets the {@link JsonElement} as {@link Optional} {@link Enum}.
	 *
	 * @param <E>      the {@link Enum} type
	 * @param enumType the class of the {@link Enum}
	 * @param jElement the {@link JsonElement}
	 * @return the {@link Optional} {@link Enum} value
	 */
	public static <E extends Enum<E>> Optional<E> getAsOptionalEnum(Class<E> enumType, JsonElement jElement) {
		return Optional.ofNullable(toEnum(enumType, toString(toPrimitive(jElement))));
	}

	/**
	 * Gets the member of the {@link JsonElement} as {@link Optional} {@link Enum}.
	 *
	 * @param <E>        the {@link Enum} type
	 * @param enumType   the class of the {@link Enum}
	 * @param jElement   the {@link JsonElement}
	 * @param memberName the name of the member
	 * @return the {@link Optional} {@link Enum} value
	 */
	public static <E extends Enum<E>> Optional<E> getAsOptionalEnum(Class<E> enumType, JsonElement jElement,
			String memberName) {
		return Optional.ofNullable(toEnum(enumType, toString(toPrimitive(toSubElement(jElement, memberName)))));
	}

	/**
	 * Gets the {@link JsonElement} as {@link Inet4Address}.
	 *
	 * @param jElement the {@link JsonElement}
	 * @return the {@link Inet4Address} value
	 * @throws OpenemsNamedException on error
	 */
	public static Inet4Address getAsInet4Address(JsonElement jElement) throws OpenemsNamedException {
		var value = InetAddressUtils.parseOrNull(toString(toPrimitive(jElement)));
		if (value != null) {
			return value;
		}
		throw OpenemsError.JSON_NO_INET4ADDRESS.exception(jElement.toString().replace("%", "%%"));
	}

	/**
	 * Gets the member of the {@link JsonElement} as {@link Inet4Address}.
	 *
	 * @param jElement   the {@link JsonElement}
	 * @param memberName the name of the member
	 * @return the {@link Inet4Address} value
	 * @throws OpenemsNamedException on error
	 */
	public static Inet4Address getAsInet4Address(JsonElement jElement, String memberName) throws OpenemsNamedException {
		var value = InetAddressUtils.parseOrNull(toString(toPrimitive(toSubElement(jElement, memberName))));
		if (value != null) {
			return value;
		}
		throw OpenemsError.JSON_NO_INET4ADDRESS_MEMBER.exception(memberName, jElement.toString().replace("%", "%%"));
	}

	/**
	 * Gets the {@link JsonElement} as {@link Optional} {@link Inet4Address}.
	 *
	 * @param jElement the {@link JsonElement}
	 * @return the {@link Optional} {@link Inet4Address} value
	 */
	public static Optional<Inet4Address> getAsOptionalInet4Address(JsonElement jElement) {
		return Optional.ofNullable(InetAddressUtils.parseOrNull(toString(toPrimitive(jElement))));
	}

	/**
	 * Gets the member of the {@link JsonElement} as {@link Optional}
	 * {@link Inet4Address}.
	 *
	 * @param jElement   the {@link JsonElement}
	 * @param memberName the name of the member
	 * @return the {@link Optional} {@link Inet4Address} value
	 */
	public static Optional<Inet4Address> getAsOptionalInet4Address(JsonElement jElement, String memberName) {
		return Optional.ofNullable(//
				InetAddressUtils.parseOrNull(toString(toPrimitive(toSubElement(jElement, memberName)))));
	}

	/**
	 * Gets the {@link JsonElement} as {@link UUID}.
	 *
	 * @param jElement the {@link JsonElement}
	 * @return the {@link UUID} value
	 * @throws OpenemsNamedException on error
	 */
	// CHECKSTYLE:OFF
	public static UUID getAsUUID(JsonElement jElement) throws OpenemsNamedException {
		// CHECKSTYLE:ON
		var value = toUUID(toString(toPrimitive(jElement)));
		if (value != null) {
			return value;
		}
		throw OpenemsError.JSON_NO_UUID.exception(jElement.toString().replace("%", "%%"));
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
		var value = toUUID(toString(toPrimitive(toSubElement(jElement, memberName))));
		if (value != null) {
			return value;
		}
		throw OpenemsError.JSON_NO_UUID_MEMBER.exception(memberName, jElement.toString().replace("%", "%%"));
	}

	/**
	 * Gets the {@link JsonElement} as {@link Optional} {@link UUID}.
	 *
	 * @param jElement the {@link JsonElement}
	 * @return the {@link Optional} {@link UUID} value
	 */
	// CHECKSTYLE:OFF
	public static Optional<UUID> getAsOptionalUUID(JsonElement jElement) {
		// CHECKSTYLE:ON
		return Optional.ofNullable(toUUID(toString(toPrimitive(jElement))));
	}

	/**
	 * Gets the member of the {@link JsonElement} as {@link Optional} {@link UUID}.
	 *
	 * @param jElement   the {@link JsonElement}
	 * @param memberName the name of the member
	 * @return the {@link Optional} {@link UUID} value
	 */
	// CHECKSTYLE:OFF
	public static Optional<UUID> getAsOptionalUUID(JsonElement jElement, String memberName) {
		// CHECKSTYLE:ON
		return Optional.ofNullable(toUUID(toString(toPrimitive(toSubElement(jElement, memberName)))));
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
				if (jA.isEmpty()) {
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
				}
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
			throw OpenemsError.JSON_PARSE_ELEMENT_FAILED.exception(//
					StringUtils.toShortString(j.toString().replace("%", "%%"), 100), //
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
		// optional
		if (value instanceof Optional<?> opt) {
			if (opt.isEmpty()) {
				return JsonNull.INSTANCE;
			}
			value = opt.get();
		}

		return switch (value) {
		case null -> JsonNull.INSTANCE;
		case Number n -> new JsonPrimitive(n);
		case String s -> new JsonPrimitive(s);
		case Boolean b -> new JsonPrimitive(b);
		case Inet4Address inet -> new JsonPrimitive(inet.getHostAddress());
		case JsonElement json -> json;
		case boolean[] bool -> {
			var js = new JsonArray();
			for (boolean b : bool) {
				js.add(new JsonPrimitive(b));
			}
			yield js;
		}
		case short[] shorts -> {
			var js = new JsonArray();
			for (short s : shorts) {
				js.add(new JsonPrimitive(s));
			}
			yield js;
		}
		case int[] ints -> {
			var js = new JsonArray();
			for (int i : ints) {
				js.add(new JsonPrimitive(i));
			}
			yield js;
		}
		case long[] longs -> {
			var js = new JsonArray();
			for (long l : longs) {
				js.add(new JsonPrimitive(l));
			}
			yield js;
		}
		case float[] floats -> {
			var js = new JsonArray();
			for (float f : floats) {
				js.add(new JsonPrimitive(f));
			}
			yield js;
		}
		case double[] doubles -> {
			var js = new JsonArray();
			for (double f : doubles) {
				js.add(new JsonPrimitive(f));
			}
			yield js;
		}
		case String[] strings -> {
			var js = new JsonArray();
			if (strings.length == 1 && strings[0].isEmpty()) {
				// special case: String-Array with one entry which is an empty String. Return an
				// empty JsonArray.
				yield js;
			}
			for (String s : strings) {
				js.add(new JsonPrimitive(s));
			}
			yield js;
		}
		case Object[] objects -> {
			var js = new JsonArray();
			for (Object o : objects) {
				js.add(JsonUtils.getAsJsonElement(o));
			}
			yield js;
		}
		default -> {
			JsonUtils.LOG.warn("Converter for [{}] of type [{}] to JSON is not implemented.", //
					value, value.getClass().getSimpleName());
			yield new JsonPrimitive(value.toString());
		}
		};
	}

	/**
	 * Gets a {@link JsonElement} as the given type.
	 *
	 * @param type the class of the type
	 * @param j    the {@link JsonElement}
	 * @return an Object of the given type
	 */
	public static Object getAsType(Class<?> type, JsonElement j) throws NotImplementedException, IllegalStateException {
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
			}
			if (Boolean.class.isAssignableFrom(type)) {
				/*
				 * Asking for an Boolean
				 */
				return j.getAsBoolean();
			}
			if (Double.class.isAssignableFrom(type)) {
				/*
				 * Asking for an Double
				 */
				return j.getAsDouble();
			}
			if (String.class.isAssignableFrom(type)) {
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
				/*
				 * Asking for Array
				 */
				if (Long.class.isAssignableFrom(type.getComponentType())) {
					/*
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
		} catch (IllegalStateException | NumberFormatException e) {
			throw new IllegalStateException("Failed to parse JsonElement [" + j + "]", e);
		}
		throw new NotImplementedException(
				"Converter for value [" + j + "] to class type [" + type + "] is not implemented.");
	}

	/**
	 * Gets a {@link JsonElement} as the given {@link OpenemsType}.
	 *
	 * @param <T>  the Type for implicit casting of the result
	 * @param type the {@link OpenemsType}
	 * @param j    the {@link JsonElement}
	 * @return an Object of the given type
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getAsType(OpenemsType type, JsonElement j) throws OpenemsNamedException {
		if ((j == null) || j.isJsonNull()) {
			return null;
		}

		if (j.isJsonPrimitive()) {
			return switch (type) {
			case BOOLEAN -> (T) Boolean.valueOf(JsonUtils.getAsBoolean(j));
			case DOUBLE -> (T) Double.valueOf(JsonUtils.getAsDouble(j));
			case FLOAT -> (T) Float.valueOf(JsonUtils.getAsFloat(j));
			case INTEGER -> (T) Integer.valueOf(JsonUtils.getAsInt(j));
			case LONG -> (T) Long.valueOf(JsonUtils.getAsLong(j));
			case SHORT -> (T) Short.valueOf(JsonUtils.getAsShort(j));
			case STRING -> (T) JsonUtils.getAsString(j);
			};
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
				return (T) j.toString();
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
		if (typeOptional.isEmpty()) {
			throw new NotImplementedException(
					"Type of Channel was not set: " + (j == null ? "UNDEFINED" : j.getAsString()));
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
	public static ZonedDateTime getAsZonedDateWithZeroTime(JsonElement element, String memberName, ZoneId timezone)
			throws OpenemsNamedException {
		var date = JsonUtils.getAsString(element, memberName).split("-");
		try {
			var year = Integer.parseInt(date[0]);
			var month = Integer.parseInt(date[1]);
			var day = Integer.parseInt(date[2]);
			return ZonedDateTime.of(year, month, day, 0, 0, 0, 0, timezone);
		} catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
			throw OpenemsError.JSON_NO_DATE_MEMBER.exception(memberName, element.toString(), e.getMessage());
		}
	}

	/**
	 * Takes a JSON in the form '2020-01-01T00:00:00' and converts it to a
	 * {@link LocalDateTime}.
	 *
	 * @param jElement   the {@link JsonElement}
	 * @param memberName the name of the member of the JsonObject
	 * @return the {@link ZonedDateTime}
	 */
	public static LocalDateTime getAsLocalDateTime(JsonElement jElement, String memberName)
			throws OpenemsNamedException {
		return DateUtils.parseLocalDateTimeOrError(toString(toPrimitive(toSubElement(jElement, memberName))));
	}

	/**
	 * Takes a JSON in the form '2020-01-01T00:00:00' and converts it to a
	 * {@link LocalDateTime}.
	 *
	 * @param jElement   the {@link JsonElement}
	 * @param memberName the name of the member of the JsonObject
	 * @return the {@link ZonedDateTime}
	 */
	public static Optional<LocalDateTime> getAsOptionalLocalDateTime(JsonElement jElement, String memberName) {
		return JsonUtils.getAsOptionalString(jElement, memberName)//
				.map(DateUtils::parseLocalDateTimeOrNull);
	}

	/**
	 * Takes a JSON in the form '2020-01-01T00:00:00Z' and converts it to a
	 * {@link ZonedDateTime}.
	 *
	 * @param jElement   the {@link JsonElement}
	 * @param memberName the name of the member of the JsonObject
	 * @return the {@link ZonedDateTime}
	 */
	public static ZonedDateTime getAsZonedDateTime(JsonElement jElement, String memberName)
			throws OpenemsNamedException {
		return DateUtils.parseZonedDateTimeOrError(toString(toPrimitive(toSubElement(jElement, memberName))));
	}

	/**
	 * Takes a JSON in the form 'YYYY-MM-DD' and converts it to a
	 * {@link ZonedDateTime}.
	 *
	 * @param element    the {@link JsonElement}
	 * @param memberName the name of the member of the JsonObject
	 * @return the {@link ZonedDateTime}
	 */
	public static Optional<ZonedDateTime> getAsOptionalZonedDateTime(JsonElement element, String memberName)
			throws OpenemsNamedException {
		return JsonUtils.getAsOptionalString(element, memberName)//
				.map(DateUtils::parseZonedDateTimeOrNull);
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
			throw OpenemsError.JSON_PARSE_FAILED.exception(e.getMessage(), StringUtils.toShortString(string, 100));
		}
	}

	/**
	 * Parses a string to a {@link Optional} {@link JsonElement}.
	 * 
	 * @param string to be parsed
	 * @return the {@link Optional} of the result; {@link Optional#empty()} if the
	 *         value can not be parsed
	 */
	public static Optional<JsonElement> parseOptional(String string) {
		try {
			return Optional.of(JsonParser.parseString(string));
		} catch (JsonParseException e) {
			return Optional.empty();
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
	 * @return a pretty string representing the {@link JsonElement} using
	 *         {@link GsonBuilder#setPrettyPrinting()}
	 */
	public static String prettyToString(JsonElement j) {
		return new GsonBuilder().setPrettyPrinting().serializeNulls().create().toJson(j);
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
			return object.isEmpty();
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
			return array.isEmpty();
		}

		return false;
	}

	/**
	 * Check if the given {@link JsonElement} is a {@link Number}.
	 *
	 * @param j the {@link JsonElement} to check
	 * @return true if the element is a {@link Number}, otherwise false
	 */
	public static boolean isNumber(JsonElement j) {
		return j.isJsonPrimitive() && j.getAsJsonPrimitive().isNumber();
	}

	/**
	 * Returns a sequential stream of the {@link JsonElement JsonElements} in the
	 * {@link JsonArray}.
	 *
	 * @param jsonArray The {@link JsonArray}, assumed to be unmodified during use
	 * @return a Stream of the elements
	 */
	public static Stream<JsonElement> stream(JsonArray jsonArray) {
		return IntStream.range(0, jsonArray.size()) //
				.mapToObj(jsonArray::get);
	}

	private static JsonObject toJsonObject(JsonElement jElement) {
		if (jElement == null) {
			return null;
		}
		if (jElement.isJsonObject()) {
			return jElement.getAsJsonObject();
		}
		return null;
	}

	/**
	 * Returns a {@link Collector} that accumulates the input elements into a new
	 * {@link JsonObject}.
	 *
	 * @param <T>         the type of the input
	 * @param keyMapper   the key mapper
	 * @param valueMapper the value mapper
	 * @return the {@link Collector}
	 */
	public static <T> Collector<T, ?, JsonObject> toJsonObject(//
			final Function<T, ? extends String> keyMapper, //
			final Function<T, ? extends JsonElement> valueMapper //
	) {
		return Collector.of(JsonObject::new, //
				(t, u) -> {
					t.add(keyMapper.apply(u), valueMapper.apply(u));
				}, (t, u) -> {
					u.entrySet().forEach(entry -> t.add(entry.getKey(), entry.getValue()));
					return t;
				});
	}

	/**
	 * Returns a Collector that accumulates the input elements into a new JsonArray.
	 *
	 * @return a Collector which collects all the input elements into a JsonArray
	 */
	public static Collector<JsonElement, JsonArrayBuilder, JsonArray> toJsonArray() {
		return new JsonArrayCollector();
	}

	private static JsonArray toJsonArray(JsonElement jElement) {
		if (jElement == null) {
			return null;
		}
		if (jElement.isJsonArray()) {
			return jElement.getAsJsonArray();
		}
		return null;
	}

	private static JsonElement toSubElement(JsonElement jElement, String memberName) {
		if (jElement == null) {
			return null;
		}
		var j = toJsonObject(jElement);
		if (j == null) {
			return null;
		}
		if (j.has(memberName)) {
			return j.get(memberName);
		}
		return null;
	}

	private static JsonPrimitive toPrimitive(JsonElement jElement) {
		if (jElement == null) {
			return null;
		}
		if (jElement.isJsonPrimitive()) {
			return jElement.getAsJsonPrimitive();
		}
		return null;
	}

	private static Boolean toBoolean(JsonPrimitive jPrimitive) {
		if (jPrimitive == null) {
			return null;
		}
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
		return null;
	}

	private static Short toShort(JsonPrimitive jPrimitive) {
		if (jPrimitive == null) {
			return null;
		}
		if (jPrimitive.isNumber()) {
			try {
				return jPrimitive.getAsShort();
			} catch (NumberFormatException e) {
				// handled below
			}
		}
		if (jPrimitive.isString()) {
			var string = jPrimitive.getAsString();
			try {
				return Short.parseShort(string);
			} catch (NumberFormatException e) {
				// handled below
			}
		}
		return null;
	}

	private static Integer toInt(JsonPrimitive jPrimitive) {
		if (jPrimitive == null) {
			return null;
		}
		if (jPrimitive.isNumber()) {
			try {
				return jPrimitive.getAsInt();
			} catch (NumberFormatException e) {
				// handled below
			}
		}
		if (jPrimitive.isString()) {
			var string = jPrimitive.getAsString();
			try {
				return Integer.parseInt(string);
			} catch (NumberFormatException e) {
				// handled below
			}
		}
		return null;
	}

	private static Long toLong(JsonPrimitive jPrimitive) {
		if (jPrimitive == null) {
			return null;
		}
		if (jPrimitive.isNumber()) {
			try {
				return jPrimitive.getAsLong();
			} catch (NumberFormatException e) {
				// handled below
			}
		}
		if (jPrimitive.isString()) {
			var string = jPrimitive.getAsString();
			try {
				return Long.parseLong(string);
			} catch (NumberFormatException e) {
				// handled below
			}
		}
		return null;
	}

	private static Float toFloat(JsonPrimitive jPrimitive) {
		if (jPrimitive == null) {
			return null;
		}
		if (jPrimitive.isNumber()) {
			try {
				return jPrimitive.getAsFloat();
			} catch (NumberFormatException e) {
				// handled below
			}
		}
		if (jPrimitive.isString()) {
			var string = jPrimitive.getAsString();
			try {
				return Float.parseFloat(string);
			} catch (NumberFormatException e) {
				// handled below
			}
		}
		return null;
	}

	private static Double toDouble(JsonPrimitive jPrimitive) {
		if (jPrimitive == null) {
			return null;
		}
		if (jPrimitive.isNumber()) {
			try {
				return jPrimitive.getAsDouble();
			} catch (NumberFormatException e) {
				// handled below
			}
		}
		if (jPrimitive.isString()) {
			var string = jPrimitive.getAsString();
			try {
				return Double.parseDouble(string);
			} catch (NumberFormatException e) {
				// handled below
			}
		}
		return null;
	}

	private static String toString(JsonPrimitive jPrimitive) {
		if (jPrimitive == null) {
			return null;
		}
		if (jPrimitive.isString()) {
			return jPrimitive.getAsString();
		}
		return null;
	}

	// CHECKSTYLE:OFF
	private static UUID toUUID(String value) {
		// CHECKSTYLE:ON
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return UUID.fromString(value);
		} catch (IllegalArgumentException e) {
			// handled below
		}
		return null;
	}

}
