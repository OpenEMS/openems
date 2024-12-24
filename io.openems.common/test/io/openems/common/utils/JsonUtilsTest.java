package io.openems.common.utils;

import static com.google.gson.JsonNull.INSTANCE;
import static io.openems.common.exceptions.OpenemsError.JSON_HAS_NO_MEMBER;
import static io.openems.common.exceptions.OpenemsError.JSON_NO_ARRAY;
import static io.openems.common.exceptions.OpenemsError.JSON_NO_ARRAY_MEMBER;
import static io.openems.common.exceptions.OpenemsError.JSON_NO_BOOLEAN;
import static io.openems.common.exceptions.OpenemsError.JSON_NO_BOOLEAN_MEMBER;
import static io.openems.common.exceptions.OpenemsError.JSON_NO_DATE_MEMBER;
import static io.openems.common.exceptions.OpenemsError.JSON_NO_DOUBLE;
import static io.openems.common.exceptions.OpenemsError.JSON_NO_DOUBLE_MEMBER;
import static io.openems.common.exceptions.OpenemsError.JSON_NO_ENUM;
import static io.openems.common.exceptions.OpenemsError.JSON_NO_ENUM_MEMBER;
import static io.openems.common.exceptions.OpenemsError.JSON_NO_FLOAT;
import static io.openems.common.exceptions.OpenemsError.JSON_NO_FLOAT_MEMBER;
import static io.openems.common.exceptions.OpenemsError.JSON_NO_INET4ADDRESS;
import static io.openems.common.exceptions.OpenemsError.JSON_NO_INET4ADDRESS_MEMBER;
import static io.openems.common.exceptions.OpenemsError.JSON_NO_INTEGER;
import static io.openems.common.exceptions.OpenemsError.JSON_NO_INTEGER_MEMBER;
import static io.openems.common.exceptions.OpenemsError.JSON_NO_LONG;
import static io.openems.common.exceptions.OpenemsError.JSON_NO_LONG_MEMBER;
import static io.openems.common.exceptions.OpenemsError.JSON_NO_OBJECT;
import static io.openems.common.exceptions.OpenemsError.JSON_NO_OBJECT_MEMBER;
import static io.openems.common.exceptions.OpenemsError.JSON_NO_PRIMITIVE;
import static io.openems.common.exceptions.OpenemsError.JSON_NO_PRIMITIVE_MEMBER;
import static io.openems.common.exceptions.OpenemsError.JSON_NO_SHORT;
import static io.openems.common.exceptions.OpenemsError.JSON_NO_SHORT_MEMBER;
import static io.openems.common.exceptions.OpenemsError.JSON_NO_STRING;
import static io.openems.common.exceptions.OpenemsError.JSON_NO_STRING_ARRAY;
import static io.openems.common.exceptions.OpenemsError.JSON_NO_STRING_MEMBER;
import static io.openems.common.exceptions.OpenemsError.JSON_NO_UUID;
import static io.openems.common.exceptions.OpenemsError.JSON_NO_UUID_MEMBER;
import static io.openems.common.exceptions.OpenemsError.JSON_PARSE_FAILED;
import static io.openems.common.utils.JsonUtils.buildJsonArray;
import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.common.utils.JsonUtils.generateJsonArray;
import static io.openems.common.utils.JsonUtils.getAsBestType;
import static io.openems.common.utils.JsonUtils.getAsBoolean;
import static io.openems.common.utils.JsonUtils.getAsDouble;
import static io.openems.common.utils.JsonUtils.getAsEnum;
import static io.openems.common.utils.JsonUtils.getAsFloat;
import static io.openems.common.utils.JsonUtils.getAsInet4Address;
import static io.openems.common.utils.JsonUtils.getAsInt;
import static io.openems.common.utils.JsonUtils.getAsJsonArray;
import static io.openems.common.utils.JsonUtils.getAsJsonElement;
import static io.openems.common.utils.JsonUtils.getAsJsonObject;
import static io.openems.common.utils.JsonUtils.getAsLong;
import static io.openems.common.utils.JsonUtils.getAsOptionalBoolean;
import static io.openems.common.utils.JsonUtils.getAsOptionalDouble;
import static io.openems.common.utils.JsonUtils.getAsOptionalEnum;
import static io.openems.common.utils.JsonUtils.getAsOptionalFloat;
import static io.openems.common.utils.JsonUtils.getAsOptionalInet4Address;
import static io.openems.common.utils.JsonUtils.getAsOptionalInt;
import static io.openems.common.utils.JsonUtils.getAsOptionalJsonArray;
import static io.openems.common.utils.JsonUtils.getAsOptionalJsonObject;
import static io.openems.common.utils.JsonUtils.getAsOptionalLong;
import static io.openems.common.utils.JsonUtils.getAsOptionalShort;
import static io.openems.common.utils.JsonUtils.getAsOptionalString;
import static io.openems.common.utils.JsonUtils.getAsOptionalUUID;
import static io.openems.common.utils.JsonUtils.getAsPrimitive;
import static io.openems.common.utils.JsonUtils.getAsShort;
import static io.openems.common.utils.JsonUtils.getAsString;
import static io.openems.common.utils.JsonUtils.getAsStringArray;
import static io.openems.common.utils.JsonUtils.getAsStringOrElse;
import static io.openems.common.utils.JsonUtils.getAsType;
import static io.openems.common.utils.JsonUtils.getAsUUID;
import static io.openems.common.utils.JsonUtils.getAsZonedDateWithZeroTime;
import static io.openems.common.utils.JsonUtils.getOptionalSubElement;
import static io.openems.common.utils.JsonUtils.getSubElement;
import static io.openems.common.utils.JsonUtils.isEmptyJsonArray;
import static io.openems.common.utils.JsonUtils.isEmptyJsonObject;
import static io.openems.common.utils.JsonUtils.parse;
import static io.openems.common.utils.JsonUtils.parseToJsonArray;
import static io.openems.common.utils.JsonUtils.parseToJsonObject;
import static io.openems.common.utils.JsonUtils.prettyToString;
import static io.openems.common.utils.JsonUtils.toJson;
import static io.openems.common.utils.JsonUtils.toJsonArray;
import static io.openems.common.utils.JsonUtils.toJsonObject;
import static java.util.Optional.empty;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.openems.common.channel.Unit;
import io.openems.common.exceptions.NotImplementedException;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;

public class JsonUtilsTest {

	private static final void assertOpenemsError(OpenemsError error, ThrowingRunnable... runnable) {
		for (var run : runnable) {
			var exeption = assertThrows(OpenemsNamedException.class, run);
			assertEquals(error, exeption.getError());
		}
	}

	private static final <T extends Throwable> void assertAllThrow(Class<T> expectedThrowable,
			ThrowingRunnable... runnable) {
		for (var run : runnable) {
			assertThrows(expectedThrowable, run);
		}
	}

	private static JsonElement JSON_NUMBER = new JsonPrimitive(123);

	private static JsonElement JSON_STRING = new JsonPrimitive("value");

	private static JsonElement JSON_BOOLEAN = new JsonPrimitive(true);

	private static JsonElement JSON_ENUM = new JsonPrimitive(Unit.WATT.name());

	private static JsonElement JSON_INET4ADDRESS = new JsonPrimitive("192.168.1.2");

	private static JsonElement JSON_UUID = new JsonPrimitive("c48e2e28-09be-41d5-8e58-260d162991cc");

	private static JsonObject JSON_OBJECT = buildJsonObject() //
			.addProperty("Boolean", true) //
			.addProperty("Float", 123.456F) //
			.addProperty("Double", 123.456) //
			.addProperty("Int", 123) //
			.addProperty("Long", 123456789L) //
			.addProperty("String", "value will be overwritten") //
			.addProperty("Enum", Unit.WATT) //
			.addProperty("Enum3", (Unit) null) //
			.addProperty("Inet4Address", "192.168.1.2") //
			.addProperty("UUID", "c48e2e28-09be-41d5-8e58-260d162991cc") //
			.addPropertyIfNotNull("Boolean1", (Boolean) null) //
			.addPropertyIfNotNull("Boolean2", Boolean.FALSE) //
			.addPropertyIfNotNull("Double1", (Double) null) //
			.addPropertyIfNotNull("Double2", Double.valueOf(123.456)) //
			.addPropertyIfNotNull("Integer1", (Integer) null) //
			.addPropertyIfNotNull("Integer2", Integer.valueOf(123)) //
			.addPropertyIfNotNull("Long1", (Long) null) //
			.addPropertyIfNotNull("Long2", Long.valueOf(123456789L)) //
			.addPropertyIfNotNull("String1", (String) null) //
			.addPropertyIfNotNull("String2", "value") //
			.addPropertyIfNotNull("Enum1", (Unit) null) //
			.addPropertyIfNotNull("Enum2", Unit.WATT) //
			.onlyIf(true, (b) -> b.addProperty("String3", "bar")) //
			.onlyIf(false, (b) -> b.addProperty("String4", "bar")) //
			.add("Number", JSON_NUMBER) //
			.add("String", JSON_STRING) //
			.add("EmptyObject", new JsonObject()) //
			.add("EmptyArray", new JsonArray()) //
			.build();

	private static JsonArray JSON_ARRAY = buildJsonArray() //
			.add(true) //
			.add(123) //
			.add(123456789L) //
			.add("String") //
			.add(JSON_OBJECT) //
			.build();

	@Test
	public void testJsonArrayCollector() throws OpenemsNamedException {
		var j = buildJsonArray() //
				.add(true) //
				.add("String") //
				.build();
		var l = Stream.of(//
				new JsonPrimitive(true), //
				new JsonPrimitive("String"))//
				.parallel() // make sure to trigger `combiner()`
				.collect(toJsonArray());
		assertEquals(j, l);
	}

	@Test
	public void testJsonObjectCollector() throws OpenemsNamedException {
		final var map = ImmutableMap.<String, JsonElement>builder() //
				.put("1", new JsonPrimitive("1")) //
				.put("2", new JsonPrimitive(2)) //
				.put("3", new JsonPrimitive(3.25)) //
				.put("4", new JsonPrimitive(false)) //
				.build();

		final var jsonObject = map.entrySet().parallelStream() //
				.collect(toJsonObject(Entry::getKey, Entry::getValue));

		assertEquals("1", jsonObject.get("1").getAsString());
		assertEquals(2, jsonObject.get("2").getAsInt());
		assertEquals(3.25, jsonObject.get("3").getAsDouble(), 0.0);
		assertEquals(false, jsonObject.get("4").getAsBoolean());
	}

	@Test
	public void testBuilder() throws OpenemsNamedException {
		buildJsonArray(JSON_ARRAY) //
				.build();
		buildJsonObject(JSON_OBJECT) //
				.build();
	}

	@Test
	public void testToJson() {
		assertEquals(INSTANCE, toJson((Boolean) null));
		assertEquals(new JsonPrimitive(true), toJson(true));

		assertEquals(INSTANCE, toJson((Number) null));
		assertEquals(new JsonPrimitive(123), toJson(123));

		assertEquals(INSTANCE, toJson((Character) null));
		assertEquals(new JsonPrimitive('a'), toJson('a'));

		assertEquals(INSTANCE, toJson((String) null));
		assertEquals(new JsonPrimitive("abc"), toJson("abc"));
	}

	@Test
	public void testGetAsPrimitive() throws OpenemsNamedException {
		// -> Element
		assertEquals(new JsonPrimitive("value"), getAsPrimitive(JSON_STRING));
		assertOpenemsError(JSON_NO_PRIMITIVE, //
				() -> getAsPrimitive(new JsonObject()) //
		);

		// -> Sub-Element
		assertEquals(new JsonPrimitive("value"), getAsPrimitive(JSON_OBJECT, "String"));
		assertOpenemsError(JSON_NO_PRIMITIVE_MEMBER, //
				() -> getAsPrimitive(JSON_OBJECT, "EmptyObject") //
		);
	}

	@Test
	public void testGetSubElement() throws OpenemsNamedException {
		// -> Sub-Element
		assertEquals(new JsonPrimitive("value"), getSubElement(JSON_OBJECT, "String"));
		assertEquals(new JsonObject(), getSubElement(JSON_OBJECT, "EmptyObject"));
		assertOpenemsError(JSON_HAS_NO_MEMBER, //
				() -> getSubElement(JSON_OBJECT, "foo") //
		);

		// -> Optional Sub-Element
		assertEquals(new JsonObject(), getOptionalSubElement(JSON_OBJECT, "EmptyObject").get());
		assertEquals(empty(), getOptionalSubElement(JSON_OBJECT, "foo"));
		assertEquals(empty(), getOptionalSubElement(null, "foo"));
	}

	@Test
	public void testGetAsJsonObject() throws OpenemsNamedException {
		// -> Element
		assertEquals(JSON_OBJECT, getAsJsonObject(JSON_OBJECT));
		assertOpenemsError(JSON_NO_OBJECT, //
				() -> getAsJsonObject(JSON_STRING) //
		);

		// -> Optional Element
		assertEquals(JSON_OBJECT, getAsOptionalJsonObject(JSON_OBJECT).get());
		assertEquals(empty(), getAsOptionalJsonObject(JSON_STRING));

		// -> Sub-Element
		assertEquals(new JsonObject(), getAsJsonObject(JSON_OBJECT, "EmptyObject"));
		assertOpenemsError(JSON_NO_OBJECT_MEMBER, //
				() -> getAsJsonObject(JSON_OBJECT, "foo"), //
				() -> getAsJsonObject(JSON_OBJECT, "String") //
		); //

		// -> Optional Sub-Element
		assertEquals(new JsonObject(), getAsOptionalJsonObject(JSON_OBJECT, "EmptyObject").get());
		assertEquals(empty(), getAsOptionalJsonObject(JSON_OBJECT, "foo"));
	}

	@Test
	public void testGetAsJsonArray() throws OpenemsNamedException {
		// -> Element
		assertEquals(JSON_ARRAY, getAsJsonArray(JSON_ARRAY));
		assertOpenemsError(JSON_NO_ARRAY, //
				() -> getAsJsonArray(JSON_STRING) //
		);

		// -> Optional Element
		assertEquals(JSON_ARRAY, getAsOptionalJsonArray(JSON_ARRAY).get());
		assertEquals(empty(), getAsOptionalJsonArray(JSON_STRING));

		// -> Sub-Element
		assertEquals(new JsonArray(), getAsJsonArray(JSON_OBJECT, "EmptyArray"));
		assertOpenemsError(JSON_NO_ARRAY_MEMBER, //
				() -> getAsJsonArray(JSON_OBJECT, "foo"), //
				() -> getAsJsonArray(JSON_OBJECT, "String") //
		);

		// -> Optional Sub-Element
		assertEquals(new JsonArray(), getAsOptionalJsonArray(JSON_OBJECT, "EmptyArray").get());
		assertEquals(empty(), getAsOptionalJsonArray(JSON_OBJECT, "foo"));
	}

	@Test
	public void testGetAsString() throws OpenemsNamedException {
		// -> Element
		assertEquals("value", getAsString(JSON_STRING));
		assertOpenemsError(JSON_NO_STRING, //
				() -> getAsString(JSON_NUMBER) //
		);

		// -> Optional Element
		assertEquals("value", getAsOptionalString(JSON_STRING).get());
		assertEquals(empty(), getAsOptionalString(JSON_OBJECT));

		// -> Sub-Element
		assertEquals("value", getAsString(JSON_OBJECT, "String"));
		assertOpenemsError(JSON_NO_STRING_MEMBER, //
				() -> getAsString(JSON_OBJECT, "foo"), //
				() -> getAsString(JSON_OBJECT, "Number") //
		);

		// -> Optional Sub-Element
		assertEquals("value", getAsOptionalString(JSON_OBJECT, "String").get());
		assertEquals(empty(), getAsOptionalString(JSON_OBJECT, "foo"));

		// -> As String or Else
		assertEquals("value", getAsStringOrElse(JSON_OBJECT, "String", "alternative"));
		assertEquals("alternative", getAsStringOrElse(JSON_OBJECT, "foo", "alternative"));
	}

	@Test
	public void testGetAsStringArray() throws OpenemsNamedException {
		var j1 = buildJsonArray() //
				.add("foo") //
				.add("bar") //
				.build();
		assertArrayEquals(new String[] { "foo", "bar" }, getAsStringArray(j1));

		var j2 = buildJsonArray() //
				.add(123) //
				.build();
		assertOpenemsError(JSON_NO_STRING_ARRAY, //
				() -> getAsStringArray(j2) //
		);
	}

	@Test
	public void testGetAsBoolean() throws OpenemsNamedException {
		// -> Element
		assertEquals(true, getAsBoolean(JSON_BOOLEAN));
		assertOpenemsError(JSON_NO_BOOLEAN, //
				() -> getAsBoolean(JSON_NUMBER) //
		);

		// -> Optional Element
		assertEquals(true, getAsOptionalBoolean(JSON_BOOLEAN).get());
		assertEquals(empty(), getAsOptionalBoolean(JSON_NUMBER));

		// -> Sub-Element
		assertEquals(true, getAsBoolean(JSON_OBJECT, "Boolean"));
		assertOpenemsError(JSON_NO_BOOLEAN_MEMBER, //
				() -> getAsBoolean(JSON_OBJECT, "foo"), //
				() -> getAsBoolean(JSON_OBJECT, "Number") //
		);

		// -> Optional Sub-Element
		assertEquals(true, getAsOptionalBoolean(JSON_OBJECT, "Boolean").get());
		assertEquals(empty(), getAsOptionalBoolean(JSON_OBJECT, "foo"));
	}

	@Test
	public void testGetAsShort() throws OpenemsNamedException {
		// -> Element
		assertEquals((short) 123, getAsShort(JSON_NUMBER));
		assertOpenemsError(JSON_NO_SHORT, //
				() -> getAsShort(JSON_STRING) //
		);

		// -> Optional Element
		assertEquals(Short.valueOf((short) 123), getAsOptionalShort(JSON_NUMBER).get());
		assertEquals(empty(), getAsOptionalShort(JSON_STRING));

		// -> Sub-Element
		assertEquals((short) 123, getAsShort(JSON_OBJECT, "Int"));
		assertOpenemsError(JSON_NO_SHORT_MEMBER, //
				() -> getAsShort(JSON_OBJECT, "String") //
		);

		// -> Optional Sub-Element
		assertEquals(Short.valueOf((short) 123), getAsOptionalShort(JSON_OBJECT, "Int").get());
		assertEquals(empty(), getAsOptionalShort(JSON_OBJECT, "foo"));
	}

	@Test
	public void testGetAsInt() throws OpenemsNamedException {
		// -> Element
		assertEquals(123, getAsInt(JSON_NUMBER));
		assertOpenemsError(JSON_NO_INTEGER, //
				() -> getAsInt(JSON_STRING) //
		);

		// -> Optional Element
		assertEquals(Integer.valueOf(123), getAsOptionalInt(JSON_NUMBER).get());
		assertEquals(empty(), getAsOptionalInt(JSON_STRING));

		// -> Sub-Element
		assertEquals(123, getAsInt(JSON_OBJECT, "Int"));
		assertOpenemsError(JSON_NO_INTEGER_MEMBER, //
				() -> getAsInt(JSON_OBJECT, "String") //
		);

		// -> Optional Sub-Element
		assertEquals(Integer.valueOf(123), getAsOptionalInt(JSON_OBJECT, "Int").get());
		assertEquals(empty(), getAsOptionalInt(JSON_OBJECT, "foo"));
	}

	@Test
	public void testGetAsLong() throws OpenemsNamedException {
		// -> Element
		assertEquals(123L, getAsLong(JSON_NUMBER));
		assertOpenemsError(JSON_NO_LONG, //
				() -> getAsLong(JSON_STRING) //
		);

		// -> Optional Element
		assertEquals(Long.valueOf(123), getAsOptionalLong(JSON_NUMBER).get());
		assertEquals(empty(), getAsOptionalLong(JSON_STRING));

		// -> Sub-Element
		assertEquals(123456789L, getAsLong(JSON_OBJECT, "Long"));
		assertOpenemsError(JSON_NO_LONG_MEMBER, //
				() -> getAsLong(JSON_OBJECT, "String") //
		);

		// -> Optional Sub-Element
		assertEquals(Long.valueOf(123456789L), getAsOptionalLong(JSON_OBJECT, "Long").get());
		assertEquals(empty(), getAsOptionalLong(JSON_OBJECT, "foo"));
	}

	@Test
	public void testGetAsDouble() throws OpenemsNamedException {
		// -> Element
		assertEquals(123.0, getAsDouble(JSON_NUMBER), 0.1);
		assertOpenemsError(JSON_NO_DOUBLE, //
				() -> getAsDouble(JSON_STRING) //
		);

		// -> Optional Element
		assertEquals(Double.valueOf(123.0), getAsOptionalDouble(JSON_NUMBER).get());
		assertEquals(empty(), getAsOptionalDouble(JSON_STRING));

		// -> Sub-Element
		assertEquals(Double.valueOf(123.456), getAsDouble(JSON_OBJECT, "Double"), 0.1);
		assertOpenemsError(JSON_NO_DOUBLE_MEMBER, //
				() -> getAsDouble(JSON_OBJECT, "String") //
		);

		// -> Optional Sub-Element
		assertEquals(Double.valueOf(123.456), getAsOptionalDouble(JSON_OBJECT, "Double").get());
		assertEquals(empty(), getAsOptionalDouble(JSON_OBJECT, "foo"));
	}

	@Test
	public void testGetAsFloat() throws OpenemsNamedException {
		// -> Element
		assertEquals(123.0, getAsFloat(JSON_NUMBER), 0.1);
		assertOpenemsError(JSON_NO_FLOAT, //
				() -> getAsFloat(JSON_STRING) //
		);

		// -> Optional Element
		assertEquals(Float.valueOf(123.0F), getAsOptionalFloat(JSON_NUMBER).get());
		assertEquals(empty(), getAsOptionalFloat(JSON_STRING));

		// -> Sub-Element
		assertEquals(Float.valueOf(123.456F), getAsFloat(JSON_OBJECT, "Double"), 0.1);
		assertOpenemsError(JSON_NO_FLOAT_MEMBER, //
				() -> getAsFloat(JSON_OBJECT, "String") //
		);

		// -> Optional Sub-Element
		assertEquals(Float.valueOf(123.456F), getAsOptionalFloat(JSON_OBJECT, "Double").get());
		assertEquals(empty(), getAsOptionalFloat(JSON_OBJECT, "foo"));
	}

	@Test
	public void testGetAsEnum() throws OpenemsNamedException {
		// -> Element
		assertEquals(Unit.WATT, getAsEnum(Unit.class, JSON_ENUM));
		assertOpenemsError(JSON_NO_ENUM, //
				() -> getAsEnum(Unit.class, JSON_STRING) //
		);

		// -> Optional Element
		assertEquals(Unit.WATT, getAsOptionalEnum(Unit.class, JSON_ENUM).get());
		assertEquals(empty(), getAsOptionalEnum(Unit.class, JSON_STRING));

		// -> Sub-Element
		assertEquals(Unit.WATT, getAsEnum(Unit.class, JSON_OBJECT, "Enum"));
		assertOpenemsError(JSON_NO_ENUM_MEMBER, //
				() -> getAsEnum(Unit.class, JSON_OBJECT, "String") //
		);

		// -> Optional Sub-Element
		assertEquals(Unit.WATT, getAsOptionalEnum(Unit.class, JSON_OBJECT, "Enum").get());
		assertEquals(empty(), getAsOptionalEnum(Unit.class, JSON_OBJECT, "foo"));
	}

	@Test
	public void testGetAsInet4Address() throws OpenemsNamedException, UnknownHostException {
		final var ip = Inet4Address.getByName("192.168.1.2");
		final var InvalidHost = new JsonPrimitive("value.");
		final var InvalidHostObject = buildJsonObject() //
				.add("String", InvalidHost) //
				.build();

		// -> Element
		assertEquals(ip, getAsInet4Address(JSON_INET4ADDRESS));
		assertOpenemsError(JSON_NO_INET4ADDRESS, //
				() -> getAsInet4Address(InvalidHost) //
		);

		// -> Optional Element
		assertEquals(ip, getAsOptionalInet4Address(JSON_INET4ADDRESS).get());
		assertEquals(empty(), getAsOptionalInet4Address(InvalidHost));

		// -> Sub-Element
		assertEquals(ip, getAsInet4Address(JSON_OBJECT, "Inet4Address"));
		assertOpenemsError(JSON_NO_INET4ADDRESS_MEMBER, //
				() -> getAsInet4Address(InvalidHostObject, "String") //
		);

		// -> Optional Sub-Element
		assertEquals(ip, getAsOptionalInet4Address(JSON_OBJECT, "Inet4Address").get());
		assertEquals(empty(), getAsOptionalInet4Address(JSON_OBJECT, "foo"));
	}

	@Test
	public void testGetAsUuid() throws OpenemsNamedException, UnknownHostException {
		final var uuid = UUID.fromString("c48e2e28-09be-41d5-8e58-260d162991cc");

		// -> Element
		assertEquals(uuid, getAsUUID(JSON_UUID));
		assertOpenemsError(JSON_NO_UUID, //
				() -> getAsUUID(JSON_STRING) //
		);

		// -> Optional Element
		assertEquals(uuid, getAsOptionalUUID(JSON_UUID).get());
		assertEquals(empty(), getAsOptionalUUID(JSON_STRING));

		// -> Sub-Element
		assertEquals(uuid, getAsUUID(JSON_OBJECT, "UUID"));
		assertOpenemsError(JSON_NO_UUID_MEMBER, //
				() -> getAsUUID(JSON_OBJECT, "String") //
		);

		// -> Optional Sub-Element
		assertEquals(uuid, getAsOptionalUUID(JSON_OBJECT, "UUID").get());
		assertEquals(empty(), getAsOptionalUUID(JSON_OBJECT, "foo"));
	}

	@Test
	public void testGetAsJsonElement() throws UnknownHostException {
		assertEquals(INSTANCE, getAsJsonElement(null));
		assertEquals(INSTANCE, getAsJsonElement(empty()));
		assertEquals(new JsonPrimitive(123), getAsJsonElement(Optional.of(123)));
		assertEquals(new JsonPrimitive(123), getAsJsonElement(123));
		assertEquals(new JsonPrimitive("foo"), getAsJsonElement("foo"));
		assertEquals(new JsonPrimitive(true), getAsJsonElement(true));
		assertEquals(new JsonPrimitive(JSON_INET4ADDRESS.getAsString()),
				getAsJsonElement(Inet4Address.getByName("192.168.1.2")));
		assertEquals(new JsonPrimitive(true), getAsJsonElement(new JsonPrimitive(true)));
		assertEquals(buildJsonArray().add(true).add(false).build(), getAsJsonElement(new boolean[] { true, false }));
		assertEquals(buildJsonArray().add((short) 123).add((short) 456).build(),
				getAsJsonElement(new short[] { (short) 123, (short) 456 }));
		assertEquals(buildJsonArray().add(123).add(456).build(), getAsJsonElement(new int[] { 123, 456 }));
		assertEquals(buildJsonArray().add(123L).add(456L).build(), getAsJsonElement(new long[] { 123, 456 }));
		assertEquals(buildJsonArray().add(123F).add(456F).build(), getAsJsonElement(new float[] { 123, 456 }));
		assertEquals(buildJsonArray().add(123D).add(456D).build(), getAsJsonElement(new double[] { 123, 456 }));
		assertEquals(buildJsonArray().add("foo").add("bar").build(), getAsJsonElement(new String[] { "foo", "bar" }));
		assertEquals(buildJsonArray().add("foo").build(), getAsJsonElement(new String[] { "foo" }));
		assertEquals(new JsonArray(), getAsJsonElement(new String[] { "" }));
		assertEquals(buildJsonArray().add("foo").build(), getAsJsonElement(new Object[] { "foo" }));
		var uuid = UUID.randomUUID();
		assertEquals(new JsonPrimitive(uuid.toString()), getAsJsonElement(uuid));
	}

	@Test
	public void testGetArrayAsInt() throws OpenemsNamedException {
		var arr = buildJsonArray() //
				.add(10) //
				.add(20) //
				.add(30) //
				.build();
		assertEquals(10, getAsInt(arr, 0));
		assertEquals(20, getAsInt(arr, 1));
		assertEquals(30, getAsInt(arr, 2));
		assertOpenemsError(JSON_NO_INTEGER_MEMBER, //
				() -> getAsInt(arr, -1), () -> getAsInt(arr, 3));
	}

	@Test
	public void testGetAsBestType() throws OpenemsNamedException {
		assertEquals(Object[].class, getAsBestType(new JsonArray()).getClass());
		assertEquals(true, ((boolean[]) getAsBestType(buildJsonArray().add(true).add(false).build()))[0]);
		assertEquals(123, ((int[]) getAsBestType(buildJsonArray().add(123).add(456).build()))[0]);
		assertEquals("foo", ((Object[]) getAsBestType(buildJsonArray().add("foo").build()))[0]);
		assertEquals("{}", ((Object[]) getAsBestType(buildJsonArray().add(new JsonObject()).build()))[0]);
		assertEquals("{}", getAsBestType(new JsonObject()));
		assertEquals(true, getAsBestType(JSON_BOOLEAN));
		assertEquals(123, getAsBestType(JSON_NUMBER));
		assertEquals("192.168.1.2", getAsBestType(JSON_INET4ADDRESS));
	}

	@Test
	public void testGetAsType1() throws OpenemsNamedException {
		assertEquals(Integer.class, getAsType(Integer.class, new JsonPrimitive(123)).getClass());
		assertEquals(Long.class, getAsType(Long.class, new JsonPrimitive(123)).getClass());
		assertEquals(Boolean.class, getAsType(Boolean.class, new JsonPrimitive(true)).getClass());
		assertEquals(Double.class, getAsType(Double.class, new JsonPrimitive(123)).getClass());
		assertEquals(String.class, getAsType(String.class, new JsonPrimitive("foo")).getClass());
		assertEquals(JsonObject.class, getAsType(JsonObject.class, new JsonObject()).getClass());
		assertEquals(JsonArray.class, getAsType(JsonArray.class, new JsonArray()).getClass());
		assertEquals(Long[].class, getAsType(Long[].class, buildJsonArray().add(123).add(456).build()).getClass());
		assertAllThrow(NotImplementedException.class, //
				() -> getAsType(Integer[].class, buildJsonArray().add(123).add(456).build()).getClass(), //
				() -> getAsType(Long[].class, new JsonObject()).getClass(), //
				() -> getAsType(Inet4Address.class, new JsonObject()).getClass() //
		);
		assertThrows(IllegalStateException.class, //
				() -> getAsType(Long[].class, buildJsonArray().add(123).add("foo").build()).getClass() //
		);
	}

	@Test
	public void testGetAsType2() throws OpenemsNamedException {
		assertEquals((Boolean) null, getAsType(OpenemsType.BOOLEAN, null));
		assertEquals((Boolean) null, getAsType(OpenemsType.BOOLEAN, INSTANCE));
		assertEquals(true, getAsType(OpenemsType.BOOLEAN, new JsonPrimitive(true)));
		assertEquals(Double.valueOf(123D), getAsType(OpenemsType.DOUBLE, new JsonPrimitive(123)));
		assertEquals(Float.valueOf(123F), getAsType(OpenemsType.FLOAT, new JsonPrimitive(123)));
		assertEquals(Integer.valueOf(123), getAsType(OpenemsType.INTEGER, new JsonPrimitive(123)));
		assertEquals(Long.valueOf(123L), getAsType(OpenemsType.LONG, new JsonPrimitive(123L)));
		assertEquals(Short.valueOf((short) 123), getAsType(OpenemsType.SHORT, new JsonPrimitive(123L)));
		assertEquals("foo", getAsType(OpenemsType.STRING, new JsonPrimitive("foo")));

		assertThrows(NotImplementedException.class, //
				() -> getAsType(OpenemsType.BOOLEAN, new JsonObject()));
		assertEquals("{}", getAsType(OpenemsType.STRING, new JsonObject()));
		assertEquals("[]", getAsType(OpenemsType.STRING, new JsonArray()));
	}

	@Test
	public void testGetAsTypeOptional() throws OpenemsNamedException {
		assertAllThrow(NotImplementedException.class, //
				() -> getAsType(empty(), null), //
				() -> getAsType(empty(), new JsonPrimitive("foo")));
		assertEquals("foo", getAsType(Optional.of(String.class), new JsonPrimitive("foo")));
	}

	@Test
	public void testGetAsZonedDateTime() throws OpenemsNamedException {
		var j = buildJsonObject() //
				.addProperty("date", "2000-12-30") //
				.addProperty("foo", "bar") //
				.build();
		assertEquals(ZonedDateTime.of(2000, 12, 30, 0, 0, 0, 0, ZoneId.of("UTC")),
				getAsZonedDateWithZeroTime(j, "date", ZoneId.of("UTC")));

		assertOpenemsError(JSON_NO_DATE_MEMBER, //
				() -> getAsZonedDateWithZeroTime(j, "foo", ZoneId.of("UTC")) //
		);
	}

	@Test
	public void testParse() throws OpenemsNamedException {
		assertEquals(buildJsonObject().addProperty("foo", "bar").build(), parse("{\"foo\": \"bar\"}"));

		assertOpenemsError(JSON_PARSE_FAILED, //
				() -> parse("{]") //
		);
	}

	@Test
	public void testParseToJsonObject() throws OpenemsNamedException {
		assertEquals(buildJsonObject().addProperty("foo", "bar").build(), parseToJsonObject("{\"foo\": \"bar\"}"));
	}

	@Test
	public void testParseToJsonArray() throws OpenemsNamedException {
		assertEquals(buildJsonArray().add("foo").build(), parseToJsonArray("[\"foo\"]"));
	}

	@Test
	public void testPrettyToString() throws OpenemsNamedException {
		assertEquals("""
				{
				  "Hello": "World",
				  "Foo": [
				    "A",
				    123,
				    true
				  ],
				  "Bar": null,
				  "BarBar": null
				}""", prettyToString(//
				buildJsonObject() //
						.addProperty("Hello", "World") //
						.add("Foo", buildJsonArray() //
								.add("A") //
								.add(123) //
								.add(true) //
								.build()) //
						.add("Bar", null) //
						.add("BarBar", JsonNull.INSTANCE) //
						.build()));
	}

	@Test
	public void testIsEmptyJsonObject() throws OpenemsNamedException {
		assertTrue(isEmptyJsonObject(new JsonObject()));
		assertFalse(isEmptyJsonObject(JSON_OBJECT));
		assertFalse(isEmptyJsonObject(new JsonArray()));
		assertFalse(isEmptyJsonObject(null));
	}

	@Test
	public void testIsEmptyJsonArray() throws OpenemsNamedException {
		assertTrue(isEmptyJsonArray(new JsonArray()));
		assertFalse(isEmptyJsonArray(JSON_ARRAY));
		assertFalse(isEmptyJsonArray(new JsonObject()));
		assertFalse(isEmptyJsonArray(null));
	}

	@Test
	public void testGenerateJsonArray() {
		var list = List.of("foo", "bar");
		var r = generateJsonArray(list, v -> new JsonPrimitive(v));
		assertEquals("foo", r.get(0).getAsString());
		assertEquals("bar", r.get(1).getAsString());
	}
}
