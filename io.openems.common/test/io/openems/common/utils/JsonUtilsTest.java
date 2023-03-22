package io.openems.common.utils;

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

	private static JsonObject JSON_OBJECT = JsonUtils.buildJsonObject() //
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

	private static JsonArray JSON_ARRAY = JsonUtils.buildJsonArray() //
			.add(true) //
			.add(123) //
			.add(123456789L) //
			.add("String") //
			.add(JSON_OBJECT) //
			.build();

	@Test
	public void testJsonArrayCollector() throws OpenemsNamedException {
		var j = JsonUtils.buildJsonArray() //
				.add(true) //
				.add("String") //
				.build();
		var l = Stream.of(//
				new JsonPrimitive(true), //
				new JsonPrimitive("String"))//
				.parallel() // make sure to trigger `combiner()`
				.collect(JsonUtils.toJsonArray());
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
				.collect(JsonUtils.toJsonObject(Entry::getKey, Entry::getValue));

		assertEquals("1", jsonObject.get("1").getAsString());
		assertEquals(2, jsonObject.get("2").getAsInt());
		assertEquals(3.25, jsonObject.get("3").getAsDouble(), 0.0);
		assertEquals(false, jsonObject.get("4").getAsBoolean());
	}

	@Test
	public void testBuilder() throws OpenemsNamedException {
		JsonUtils.buildJsonArray(JSON_ARRAY) //
				.build();
		JsonUtils.buildJsonObject(JSON_OBJECT) //
				.build();
	}

	@Test
	public void testGetAsPrimitive() throws OpenemsNamedException {
		// -> Element
		assertEquals(new JsonPrimitive("value"), JsonUtils.getAsPrimitive(JSON_STRING));
		assertOpenemsError(OpenemsError.JSON_NO_PRIMITIVE, //
				() -> JsonUtils.getAsPrimitive(new JsonObject()) //
		);

		// -> Sub-Element
		assertEquals(new JsonPrimitive("value"), JsonUtils.getAsPrimitive(JSON_OBJECT, "String"));
		assertOpenemsError(OpenemsError.JSON_NO_PRIMITIVE_MEMBER, //
				() -> JsonUtils.getAsPrimitive(JSON_OBJECT, "EmptyObject") //
		);
	}

	@Test
	public void testGetSubElement() throws OpenemsNamedException {
		// -> Sub-Element
		assertEquals(new JsonPrimitive("value"), JsonUtils.getSubElement(JSON_OBJECT, "String"));
		assertEquals(new JsonObject(), JsonUtils.getSubElement(JSON_OBJECT, "EmptyObject"));
		assertOpenemsError(OpenemsError.JSON_HAS_NO_MEMBER, //
				() -> JsonUtils.getSubElement(JSON_OBJECT, "foo") //
		);

		// -> Optional Sub-Element
		assertEquals(new JsonObject(), JsonUtils.getOptionalSubElement(JSON_OBJECT, "EmptyObject").get());
		assertEquals(Optional.empty(), JsonUtils.getOptionalSubElement(JSON_OBJECT, "foo"));
		assertEquals(Optional.empty(), JsonUtils.getOptionalSubElement(null, "foo"));
	}

	@Test
	public void testGetAsJsonObject() throws OpenemsNamedException {
		// -> Element
		assertEquals(JSON_OBJECT, JsonUtils.getAsJsonObject(JSON_OBJECT));
		assertOpenemsError(OpenemsError.JSON_NO_OBJECT, //
				() -> JsonUtils.getAsJsonObject(JSON_STRING) //
		);

		// -> Optional Element
		assertEquals(JSON_OBJECT, JsonUtils.getAsOptionalJsonObject(JSON_OBJECT).get());
		assertEquals(Optional.empty(), JsonUtils.getAsOptionalJsonObject(JSON_STRING));

		// -> Sub-Element
		assertEquals(new JsonObject(), JsonUtils.getAsJsonObject(JSON_OBJECT, "EmptyObject"));
		assertOpenemsError(OpenemsError.JSON_NO_OBJECT_MEMBER, //
				() -> JsonUtils.getAsJsonObject(JSON_OBJECT, "foo"), //
				() -> JsonUtils.getAsJsonObject(JSON_OBJECT, "String") //
		); //

		// -> Optional Sub-Element
		assertEquals(new JsonObject(), JsonUtils.getAsOptionalJsonObject(JSON_OBJECT, "EmptyObject").get());
		assertEquals(Optional.empty(), JsonUtils.getAsOptionalJsonObject(JSON_OBJECT, "foo"));
	}

	@Test
	public void testGetAsJsonArray() throws OpenemsNamedException {
		// -> Element
		assertEquals(JSON_ARRAY, JsonUtils.getAsJsonArray(JSON_ARRAY));
		assertOpenemsError(OpenemsError.JSON_NO_ARRAY, //
				() -> JsonUtils.getAsJsonArray(JSON_STRING) //
		);

		// -> Optional Element
		assertEquals(JSON_ARRAY, JsonUtils.getAsOptionalJsonArray(JSON_ARRAY).get());
		assertEquals(Optional.empty(), JsonUtils.getAsOptionalJsonArray(JSON_STRING));

		// -> Sub-Element
		assertEquals(new JsonArray(), JsonUtils.getAsJsonArray(JSON_OBJECT, "EmptyArray"));
		assertOpenemsError(OpenemsError.JSON_NO_ARRAY_MEMBER, //
				() -> JsonUtils.getAsJsonArray(JSON_OBJECT, "foo"), //
				() -> JsonUtils.getAsJsonArray(JSON_OBJECT, "String") //
		);

		// -> Optional Sub-Element
		assertEquals(new JsonArray(), JsonUtils.getAsOptionalJsonArray(JSON_OBJECT, "EmptyArray").get());
		assertEquals(Optional.empty(), JsonUtils.getAsOptionalJsonArray(JSON_OBJECT, "foo"));
	}

	@Test
	public void testGetAsString() throws OpenemsNamedException {
		// -> Element
		assertEquals("value", JsonUtils.getAsString(JSON_STRING));
		assertOpenemsError(OpenemsError.JSON_NO_STRING, //
				() -> JsonUtils.getAsString(JSON_NUMBER) //
		);

		// -> Optional Element
		assertEquals("value", JsonUtils.getAsOptionalString(JSON_STRING).get());
		assertEquals(Optional.empty(), JsonUtils.getAsOptionalString(JSON_OBJECT));

		// -> Sub-Element
		assertEquals("value", JsonUtils.getAsString(JSON_OBJECT, "String"));
		assertOpenemsError(OpenemsError.JSON_NO_STRING_MEMBER, //
				() -> JsonUtils.getAsString(JSON_OBJECT, "foo"), //
				() -> JsonUtils.getAsString(JSON_OBJECT, "Number") //
		);

		// -> Optional Sub-Element
		assertEquals("value", JsonUtils.getAsOptionalString(JSON_OBJECT, "String").get());
		assertEquals(Optional.empty(), JsonUtils.getAsOptionalString(JSON_OBJECT, "foo"));
	}

	@Test
	public void testGetAsStringArray() throws OpenemsNamedException {
		var j1 = JsonUtils.buildJsonArray() //
				.add("foo") //
				.add("bar") //
				.build();
		assertArrayEquals(new String[] { "foo", "bar" }, JsonUtils.getAsStringArray(j1));

		var j2 = JsonUtils.buildJsonArray() //
				.add(123) //
				.build();
		assertOpenemsError(OpenemsError.JSON_NO_STRING_ARRAY, //
				() -> JsonUtils.getAsStringArray(j2) //
		);
	}

	@Test
	public void testGetAsBoolean() throws OpenemsNamedException {
		// -> Element
		assertEquals(true, JsonUtils.getAsBoolean(JSON_BOOLEAN));
		assertOpenemsError(OpenemsError.JSON_NO_BOOLEAN, //
				() -> JsonUtils.getAsBoolean(JSON_NUMBER) //
		);

		// -> Optional Element
		assertEquals(true, JsonUtils.getAsOptionalBoolean(JSON_BOOLEAN).get());
		assertEquals(Optional.empty(), JsonUtils.getAsOptionalBoolean(JSON_NUMBER));

		// -> Sub-Element
		assertEquals(true, JsonUtils.getAsBoolean(JSON_OBJECT, "Boolean"));
		assertOpenemsError(OpenemsError.JSON_NO_BOOLEAN_MEMBER, //
				() -> JsonUtils.getAsBoolean(JSON_OBJECT, "foo"), //
				() -> JsonUtils.getAsBoolean(JSON_OBJECT, "Number") //
		);

		// -> Optional Sub-Element
		assertEquals(true, JsonUtils.getAsOptionalBoolean(JSON_OBJECT, "Boolean").get());
		assertEquals(Optional.empty(), JsonUtils.getAsOptionalBoolean(JSON_OBJECT, "foo"));
	}

	@Test
	public void testGetAsShort() throws OpenemsNamedException {
		// -> Element
		assertEquals((short) 123, JsonUtils.getAsShort(JSON_NUMBER));
		assertOpenemsError(OpenemsError.JSON_NO_SHORT, //
				() -> JsonUtils.getAsShort(JSON_STRING) //
		);

		// -> Optional Element
		assertEquals(Short.valueOf((short) 123), JsonUtils.getAsOptionalShort(JSON_NUMBER).get());
		assertEquals(Optional.empty(), JsonUtils.getAsOptionalShort(JSON_STRING));

		// -> Sub-Element
		assertEquals((short) 123, JsonUtils.getAsShort(JSON_OBJECT, "Int"));
		assertOpenemsError(OpenemsError.JSON_NO_SHORT_MEMBER, //
				() -> JsonUtils.getAsShort(JSON_OBJECT, "String") //
		);

		// -> Optional Sub-Element
		assertEquals(Short.valueOf((short) 123), JsonUtils.getAsOptionalShort(JSON_OBJECT, "Int").get());
		assertEquals(Optional.empty(), JsonUtils.getAsOptionalShort(JSON_OBJECT, "foo"));
	}

	@Test
	public void testGetAsInt() throws OpenemsNamedException {
		// -> Element
		assertEquals(123, JsonUtils.getAsInt(JSON_NUMBER));
		assertOpenemsError(OpenemsError.JSON_NO_INTEGER, //
				() -> JsonUtils.getAsInt(JSON_STRING) //
		);

		// -> Optional Element
		assertEquals(Integer.valueOf(123), JsonUtils.getAsOptionalInt(JSON_NUMBER).get());
		assertEquals(Optional.empty(), JsonUtils.getAsOptionalInt(JSON_STRING));

		// -> Sub-Element
		assertEquals(123, JsonUtils.getAsInt(JSON_OBJECT, "Int"));
		assertOpenemsError(OpenemsError.JSON_NO_INTEGER_MEMBER, //
				() -> JsonUtils.getAsInt(JSON_OBJECT, "String") //
		);

		// -> Optional Sub-Element
		assertEquals(Integer.valueOf(123), JsonUtils.getAsOptionalInt(JSON_OBJECT, "Int").get());
		assertEquals(Optional.empty(), JsonUtils.getAsOptionalInt(JSON_OBJECT, "foo"));
	}

	@Test
	public void testGetAsLong() throws OpenemsNamedException {
		// -> Element
		assertEquals(123L, JsonUtils.getAsLong(JSON_NUMBER));
		assertOpenemsError(OpenemsError.JSON_NO_LONG, //
				() -> JsonUtils.getAsLong(JSON_STRING) //
		);

		// -> Optional Element
		assertEquals(Long.valueOf(123), JsonUtils.getAsOptionalLong(JSON_NUMBER).get());
		assertEquals(Optional.empty(), JsonUtils.getAsOptionalLong(JSON_STRING));

		// -> Sub-Element
		assertEquals(123456789L, JsonUtils.getAsLong(JSON_OBJECT, "Long"));
		assertOpenemsError(OpenemsError.JSON_NO_LONG_MEMBER, //
				() -> JsonUtils.getAsLong(JSON_OBJECT, "String") //
		);

		// -> Optional Sub-Element
		assertEquals(Long.valueOf(123456789L), JsonUtils.getAsOptionalLong(JSON_OBJECT, "Long").get());
		assertEquals(Optional.empty(), JsonUtils.getAsOptionalLong(JSON_OBJECT, "foo"));
	}

	@Test
	public void testGetAsDouble() throws OpenemsNamedException {
		// -> Element
		assertEquals(123.0, JsonUtils.getAsDouble(JSON_NUMBER), 0.1);
		assertOpenemsError(OpenemsError.JSON_NO_DOUBLE, //
				() -> JsonUtils.getAsDouble(JSON_STRING) //
		);

		// -> Optional Element
		assertEquals(Double.valueOf(123.0), JsonUtils.getAsOptionalDouble(JSON_NUMBER).get());
		assertEquals(Optional.empty(), JsonUtils.getAsOptionalDouble(JSON_STRING));

		// -> Sub-Element
		assertEquals(Double.valueOf(123.456), JsonUtils.getAsDouble(JSON_OBJECT, "Double"), 0.1);
		assertOpenemsError(OpenemsError.JSON_NO_DOUBLE_MEMBER, //
				() -> JsonUtils.getAsDouble(JSON_OBJECT, "String") //
		);

		// -> Optional Sub-Element
		assertEquals(Double.valueOf(123.456), JsonUtils.getAsOptionalDouble(JSON_OBJECT, "Double").get());
		assertEquals(Optional.empty(), JsonUtils.getAsOptionalDouble(JSON_OBJECT, "foo"));
	}

	@Test
	public void testGetAsFloat() throws OpenemsNamedException {
		// -> Element
		assertEquals(123.0, JsonUtils.getAsFloat(JSON_NUMBER), 0.1);
		assertOpenemsError(OpenemsError.JSON_NO_FLOAT, //
				() -> JsonUtils.getAsFloat(JSON_STRING) //
		);

		// -> Optional Element
		assertEquals(Float.valueOf(123.0F), JsonUtils.getAsOptionalFloat(JSON_NUMBER).get());
		assertEquals(Optional.empty(), JsonUtils.getAsOptionalFloat(JSON_STRING));

		// -> Sub-Element
		assertEquals(Float.valueOf(123.456F), JsonUtils.getAsFloat(JSON_OBJECT, "Double"), 0.1);
		assertOpenemsError(OpenemsError.JSON_NO_FLOAT_MEMBER, //
				() -> JsonUtils.getAsFloat(JSON_OBJECT, "String") //
		);

		// -> Optional Sub-Element
		assertEquals(Float.valueOf(123.456F), JsonUtils.getAsOptionalFloat(JSON_OBJECT, "Double").get());
		assertEquals(Optional.empty(), JsonUtils.getAsOptionalFloat(JSON_OBJECT, "foo"));
	}

	@Test
	public void testGetAsEnum() throws OpenemsNamedException {
		// -> Element
		assertEquals(Unit.WATT, JsonUtils.getAsEnum(Unit.class, JSON_ENUM));
		assertOpenemsError(OpenemsError.JSON_NO_ENUM, //
				() -> JsonUtils.getAsEnum(Unit.class, JSON_STRING) //
		);

		// -> Optional Element
		assertEquals(Unit.WATT, JsonUtils.getAsOptionalEnum(Unit.class, JSON_ENUM).get());
		assertEquals(Optional.empty(), JsonUtils.getAsOptionalEnum(Unit.class, JSON_STRING));

		// -> Sub-Element
		assertEquals(Unit.WATT, JsonUtils.getAsEnum(Unit.class, JSON_OBJECT, "Enum"));
		assertOpenemsError(OpenemsError.JSON_NO_ENUM_MEMBER, //
				() -> JsonUtils.getAsEnum(Unit.class, JSON_OBJECT, "String") //
		);

		// -> Optional Sub-Element
		assertEquals(Unit.WATT, JsonUtils.getAsOptionalEnum(Unit.class, JSON_OBJECT, "Enum").get());
		assertEquals(Optional.empty(), JsonUtils.getAsOptionalEnum(Unit.class, JSON_OBJECT, "foo"));
	}

	@Test
	public void testGetAsInet4Address() throws OpenemsNamedException, UnknownHostException {
		final var ip = Inet4Address.getByName("192.168.1.2");
		final var InvalidHost = new JsonPrimitive("value.");
		final var InvalidHostObject = JsonUtils.buildJsonObject() //
				.add("String", InvalidHost) //
				.build();

		// -> Element
		assertEquals(ip, JsonUtils.getAsInet4Address(JSON_INET4ADDRESS));
		assertOpenemsError(OpenemsError.JSON_NO_INET4ADDRESS, //
				() -> JsonUtils.getAsInet4Address(InvalidHost) //
		);

		// -> Optional Element
		assertEquals(ip, JsonUtils.getAsOptionalInet4Address(JSON_INET4ADDRESS).get());
		assertEquals(Optional.empty(), JsonUtils.getAsOptionalInet4Address(InvalidHost));

		// -> Sub-Element
		assertEquals(ip, JsonUtils.getAsInet4Address(JSON_OBJECT, "Inet4Address"));
		assertOpenemsError(OpenemsError.JSON_NO_INET4ADDRESS_MEMBER, //
				() -> JsonUtils.getAsInet4Address(InvalidHostObject, "String") //
		);

		// -> Optional Sub-Element
		assertEquals(ip, JsonUtils.getAsOptionalInet4Address(JSON_OBJECT, "Inet4Address").get());
		assertEquals(Optional.empty(), JsonUtils.getAsOptionalInet4Address(JSON_OBJECT, "foo"));
	}

	@Test
	public void testGetAsUuid() throws OpenemsNamedException, UnknownHostException {
		final var uuid = UUID.fromString("c48e2e28-09be-41d5-8e58-260d162991cc");

		// -> Element
		assertEquals(uuid, JsonUtils.getAsUUID(JSON_UUID));
		assertOpenemsError(OpenemsError.JSON_NO_UUID, //
				() -> JsonUtils.getAsUUID(JSON_STRING) //
		);

		// -> Optional Element
		assertEquals(uuid, JsonUtils.getAsOptionalUUID(JSON_UUID).get());
		assertEquals(Optional.empty(), JsonUtils.getAsOptionalUUID(JSON_STRING));

		// -> Sub-Element
		assertEquals(uuid, JsonUtils.getAsUUID(JSON_OBJECT, "UUID"));
		assertOpenemsError(OpenemsError.JSON_NO_UUID_MEMBER, //
				() -> JsonUtils.getAsUUID(JSON_OBJECT, "String") //
		);

		// -> Optional Sub-Element
		assertEquals(uuid, JsonUtils.getAsOptionalUUID(JSON_OBJECT, "UUID").get());
		assertEquals(Optional.empty(), JsonUtils.getAsOptionalUUID(JSON_OBJECT, "foo"));
	}

	@Test
	public void testGetAsJsonElement() throws UnknownHostException {
		assertEquals(JsonNull.INSTANCE, JsonUtils.getAsJsonElement(null));
		assertEquals(JsonNull.INSTANCE, JsonUtils.getAsJsonElement(Optional.empty()));
		assertEquals(new JsonPrimitive(123), JsonUtils.getAsJsonElement(Optional.of(123)));
		assertEquals(new JsonPrimitive(123), JsonUtils.getAsJsonElement(123));
		assertEquals(new JsonPrimitive("foo"), JsonUtils.getAsJsonElement("foo"));
		assertEquals(new JsonPrimitive(true), JsonUtils.getAsJsonElement(true));
		assertEquals(new JsonPrimitive(JSON_INET4ADDRESS.getAsString()),
				JsonUtils.getAsJsonElement(Inet4Address.getByName("192.168.1.2")));
		assertEquals(new JsonPrimitive(true), JsonUtils.getAsJsonElement(new JsonPrimitive(true)));
		assertEquals(JsonUtils.buildJsonArray().add(true).add(false).build(),
				JsonUtils.getAsJsonElement(new boolean[] { true, false }));
		assertEquals(JsonUtils.buildJsonArray().add((short) 123).add((short) 456).build(),
				JsonUtils.getAsJsonElement(new short[] { (short) 123, (short) 456 }));
		assertEquals(JsonUtils.buildJsonArray().add(123).add(456).build(),
				JsonUtils.getAsJsonElement(new int[] { 123, 456 }));
		assertEquals(JsonUtils.buildJsonArray().add(123L).add(456L).build(),
				JsonUtils.getAsJsonElement(new long[] { 123, 456 }));
		assertEquals(JsonUtils.buildJsonArray().add(123F).add(456F).build(),
				JsonUtils.getAsJsonElement(new float[] { 123, 456 }));
		assertEquals(JsonUtils.buildJsonArray().add(123D).add(456D).build(),
				JsonUtils.getAsJsonElement(new double[] { 123, 456 }));
		assertEquals(JsonUtils.buildJsonArray().add("foo").add("bar").build(),
				JsonUtils.getAsJsonElement(new String[] { "foo", "bar" }));
		assertEquals(JsonUtils.buildJsonArray().add("foo").build(), JsonUtils.getAsJsonElement(new String[] { "foo" }));
		assertEquals(new JsonArray(), JsonUtils.getAsJsonElement(new String[] { "" }));
		assertEquals(JsonUtils.buildJsonArray().add("foo").build(), JsonUtils.getAsJsonElement(new Object[] { "foo" }));
		var uuid = UUID.randomUUID();
		assertEquals(new JsonPrimitive(uuid.toString()), JsonUtils.getAsJsonElement(uuid));
	}

	@Test
	public void testGetArrayAsInt() throws OpenemsNamedException {
		var arr = JsonUtils.buildJsonArray() //
				.add(10) //
				.add(20) //
				.add(30) //
				.build();
		assertEquals(10, JsonUtils.getAsInt(arr, 0));
		assertEquals(20, JsonUtils.getAsInt(arr, 1));
		assertEquals(30, JsonUtils.getAsInt(arr, 2));
		assertOpenemsError(OpenemsError.JSON_NO_INTEGER_MEMBER, //
				() -> JsonUtils.getAsInt(arr, -1), () -> JsonUtils.getAsInt(arr, 3));
	}

	@Test
	public void testGetAsBestType() throws OpenemsNamedException {
		assertEquals(Object[].class, JsonUtils.getAsBestType(new JsonArray()).getClass());
		assertEquals(true,
				((boolean[]) JsonUtils.getAsBestType(JsonUtils.buildJsonArray().add(true).add(false).build()))[0]);
		assertEquals(123, ((int[]) JsonUtils.getAsBestType(JsonUtils.buildJsonArray().add(123).add(456).build()))[0]);
		assertEquals("foo", ((Object[]) JsonUtils.getAsBestType(JsonUtils.buildJsonArray().add("foo").build()))[0]);
		assertEquals("{}",
				((Object[]) JsonUtils.getAsBestType(JsonUtils.buildJsonArray().add(new JsonObject()).build()))[0]);
		assertEquals("{}", JsonUtils.getAsBestType(new JsonObject()));
		assertEquals(true, JsonUtils.getAsBestType(JSON_BOOLEAN));
		assertEquals(123, JsonUtils.getAsBestType(JSON_NUMBER));
		assertEquals("192.168.1.2", JsonUtils.getAsBestType(JSON_INET4ADDRESS));
	}

	@Test
	public void testGetAsType1() throws OpenemsNamedException {
		assertEquals(Integer.class, JsonUtils.getAsType(Integer.class, new JsonPrimitive(123)).getClass());
		assertEquals(Long.class, JsonUtils.getAsType(Long.class, new JsonPrimitive(123)).getClass());
		assertEquals(Boolean.class, JsonUtils.getAsType(Boolean.class, new JsonPrimitive(true)).getClass());
		assertEquals(Double.class, JsonUtils.getAsType(Double.class, new JsonPrimitive(123)).getClass());
		assertEquals(String.class, JsonUtils.getAsType(String.class, new JsonPrimitive("foo")).getClass());
		assertEquals(JsonObject.class, JsonUtils.getAsType(JsonObject.class, new JsonObject()).getClass());
		assertEquals(JsonArray.class, JsonUtils.getAsType(JsonArray.class, new JsonArray()).getClass());
		assertEquals(Long[].class,
				JsonUtils.getAsType(Long[].class, JsonUtils.buildJsonArray().add(123).add(456).build()).getClass());
		assertAllThrow(NotImplementedException.class, //
				() -> JsonUtils.getAsType(Integer[].class, JsonUtils.buildJsonArray().add(123).add(456).build())
						.getClass(), //
				() -> JsonUtils.getAsType(Long[].class, new JsonObject()).getClass(), //
				() -> JsonUtils.getAsType(Inet4Address.class, new JsonObject()).getClass() //
		);
		assertThrows(IllegalStateException.class, //
				() -> JsonUtils.getAsType(Long[].class, JsonUtils.buildJsonArray().add(123).add("foo").build())
						.getClass() //
		);
	}

	@Test
	public void testGetAsType2() throws OpenemsNamedException {
		assertEquals((Boolean) null, JsonUtils.getAsType(OpenemsType.BOOLEAN, null));
		assertEquals((Boolean) null, JsonUtils.getAsType(OpenemsType.BOOLEAN, JsonNull.INSTANCE));
		assertEquals(true, JsonUtils.getAsType(OpenemsType.BOOLEAN, new JsonPrimitive(true)));
		assertEquals(Double.valueOf(123D), JsonUtils.getAsType(OpenemsType.DOUBLE, new JsonPrimitive(123)));
		assertEquals(Float.valueOf(123F), JsonUtils.getAsType(OpenemsType.FLOAT, new JsonPrimitive(123)));
		assertEquals(Integer.valueOf(123), JsonUtils.getAsType(OpenemsType.INTEGER, new JsonPrimitive(123)));
		assertEquals(Long.valueOf(123L), JsonUtils.getAsType(OpenemsType.LONG, new JsonPrimitive(123L)));
		assertEquals(Short.valueOf((short) 123), JsonUtils.getAsType(OpenemsType.SHORT, new JsonPrimitive(123L)));
		assertEquals("foo", JsonUtils.getAsType(OpenemsType.STRING, new JsonPrimitive("foo")));

		assertThrows(NotImplementedException.class, //
				() -> JsonUtils.getAsType(OpenemsType.BOOLEAN, new JsonObject()));
		assertEquals("{}", JsonUtils.getAsType(OpenemsType.STRING, new JsonObject()));
		assertEquals("[]", JsonUtils.getAsType(OpenemsType.STRING, new JsonArray()));
	}

	@Test
	public void testGetAsTypeOptional() throws OpenemsNamedException {
		assertAllThrow(NotImplementedException.class, //
				() -> JsonUtils.getAsType(Optional.empty(), null), //
				() -> JsonUtils.getAsType(Optional.empty(), new JsonPrimitive("foo")));
		assertEquals("foo", JsonUtils.getAsType(Optional.of(String.class), new JsonPrimitive("foo")));
	}

	@Test
	public void testGetAsZonedDateTime() throws OpenemsNamedException {
		var j = JsonUtils.buildJsonObject() //
				.addProperty("date", "2000-12-30") //
				.addProperty("foo", "bar") //
				.build();
		assertEquals(ZonedDateTime.of(2000, 12, 30, 0, 0, 0, 0, ZoneId.of("UTC")),
				JsonUtils.getAsZonedDateWithZeroTime(j, "date", ZoneId.of("UTC")));

		assertOpenemsError(OpenemsError.JSON_NO_DATE_MEMBER, //
				() -> JsonUtils.getAsZonedDateWithZeroTime(j, "foo", ZoneId.of("UTC")) //
		);
	}

	@Test
	public void testParse() throws OpenemsNamedException {
		assertEquals(JsonUtils.buildJsonObject().addProperty("foo", "bar").build(),
				JsonUtils.parse("{\"foo\": \"bar\"}"));

		assertOpenemsError(OpenemsError.JSON_PARSE_FAILED, //
				() -> JsonUtils.parse("{]") //
		);
	}

	@Test
	public void testParseToJsonObject() throws OpenemsNamedException {
		assertEquals(JsonUtils.buildJsonObject().addProperty("foo", "bar").build(),
				JsonUtils.parseToJsonObject("{\"foo\": \"bar\"}"));
	}

	@Test
	public void testParseToJsonArray() throws OpenemsNamedException {
		assertEquals(JsonUtils.buildJsonArray().add("foo").build(), JsonUtils.parseToJsonArray("[\"foo\"]"));
	}

	@Test
	public void testIsEmptyJsonObject() throws OpenemsNamedException {
		assertTrue(JsonUtils.isEmptyJsonObject(new JsonObject()));
		assertFalse(JsonUtils.isEmptyJsonObject(JSON_OBJECT));
		assertFalse(JsonUtils.isEmptyJsonObject(new JsonArray()));
		assertFalse(JsonUtils.isEmptyJsonObject(null));
	}

	@Test
	public void testIsEmptyJsonArray() throws OpenemsNamedException {
		assertTrue(JsonUtils.isEmptyJsonArray(new JsonArray()));
		assertFalse(JsonUtils.isEmptyJsonArray(JSON_ARRAY));
		assertFalse(JsonUtils.isEmptyJsonArray(new JsonObject()));
		assertFalse(JsonUtils.isEmptyJsonArray(null));
	}

	@Test
	public void testGenerateJsonArray() {
		var list = List.of("foo", "bar");
		var r = JsonUtils.generateJsonArray(list, v -> new JsonPrimitive(v));
		assertEquals("foo", r.get(0).getAsString());
		assertEquals("bar", r.get(1).getAsString());
	}
}
