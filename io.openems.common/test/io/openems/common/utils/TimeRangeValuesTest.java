package io.openems.common.utils;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import java.time.Instant;
import java.util.Objects;

import org.junit.Test;

import com.google.gson.JsonPrimitive;

import io.openems.common.jsonrpc.serialization.JsonElementPath;
import io.openems.common.jsonrpc.serialization.JsonSerializerUtil;
import io.openems.common.timedata.DurationUnit;

public class TimeRangeValuesTest {
	@Test
	public void testTimeRangeValues() {
		var start = instant("00:00:00");
		var end = instant("02:00:00");

		var timeRangeValuesBuilder = TimeRangeValues.builder(start, end, DurationUnit.ofMinutes(15), Integer.class) //
				.setByTime(instant("00:00:00"), 11) //
				.setByTime(instant("00:15:00"), 12) //
				.setByTime(instant("00:17:00"), 13) //
				.setByTime(instant("00:30:00"), 14) //
				.setByPosition(4, 20) // 0 = 00:00:00, 1 = 00:15:00, 2 = 00:30:00, 3 = 00:45:00, 4 = 01:00:00
				.setByTime(instant("01:45:00"), 15);

		assertThrows(IndexOutOfBoundsException.class, () -> timeRangeValuesBuilder.setByTime(instant("02:00:00"), 15));

		var timeRangeValues = timeRangeValuesBuilder.build();
		assertEquals(11, timeRangeValues.getAt(instant("00:00:00")).intValue());
		assertEquals(11, timeRangeValues.getAt(instant("00:01:00")).intValue());
		assertEquals(13, timeRangeValues.getAt(instant("00:15:00")).intValue());
		assertEquals(13, timeRangeValues.getAt(instant("00:15:00")).intValue());
		assertEquals(20, timeRangeValues.getAt(instant("01:00:00")).intValue());
		assertNull(timeRangeValues.getAt(instant("01:30:00")));

		timeRangeValues = TimeRangeValues.builder(start, end, DurationUnit.ofMinutes(15), Integer.class) //
				.setByTime(instant("00:00:00"), 10) //
				.setByTime(instant("01:00:00"), 20) //
				.setByTime(instant("01:45:00"), 30) //
				.fillMissingDataWithPreviousData() //
				.build();

		assertEquals(10, timeRangeValues.getAt(instant("00:30:00")).intValue());
		assertEquals(20, timeRangeValues.getAt(instant("01:00:00")).intValue());
		assertEquals(20, timeRangeValues.getAt(instant("01:20:00")).intValue());
		assertEquals(30, timeRangeValues.getAt(instant("01:59:59")).intValue());
	}

	@Test
	public void testClone() {
		var start = instant("00:00:00");
		var end = instant("12:00:00");

		var valuesWith60min = TimeRangeValues.builder(start, end, DurationUnit.ofMinutes(60), Integer.class) //
				.setByTime(instant("01:00:00"), 11) //
				.setByTime(instant("02:00:00"), 12) //
				.setByTime(instant("03:00:00"), 13) //
				.setByTime(instant("05:00:00"), 14) //
				.setByTime(instant("06:20:00"), 15) //
				.build();

		var valuesWith15min = valuesWith60min.cloneWithDifferentUnit(DurationUnit.ofMinutes(15),
				x -> x.stream().filter(Objects::nonNull).findFirst().orElse(null));
		assertEqualTrv(valuesWith60min, valuesWith15min, instant("00:00:00"), instant("00:00:00"));
		assertEqualTrv(valuesWith60min, valuesWith15min, instant("00:05:00"), instant("00:05:00"));
		assertEqualTrv(valuesWith60min, valuesWith15min, instant("01:00:00"), instant("01:00:00"));
		assertEqualTrv(valuesWith60min, valuesWith15min, instant("01:00:00"), instant("01:15:00"));
		assertEqualTrv(valuesWith60min, valuesWith15min, instant("01:30:00"), instant("01:30:00"));
		assertEqualTrv(valuesWith60min, valuesWith15min, instant("02:00:00"), instant("02:30:00"));
		assertEqualTrv(valuesWith60min, valuesWith15min, instant("06:20:00"), instant("06:20:00"));
		assertEqualTrv(valuesWith60min, valuesWith15min, instant("08:00:00"), instant("08:00:00"));
	}

	@Test
	public void testSerialization() {
		var intSerializer = JsonSerializerUtil.jsonSerializer(Integer.class, JsonElementPath::getAsInt,
				JsonPrimitive::new);
		var serializer = TimeRangeValues.serializer(Integer[]::new, intSerializer);

		var start = instant("08:00:00");
		var end = instant("12:00:00");

		var originalValues = TimeRangeValues.builder(start, end, DurationUnit.ofMinutes(15), Integer.class) //
				.setByTime(instant("09:00:00"), 11) //
				.setByTime(instant("10:15:00"), 12) //
				.build();

		assertArrayEquals(new Integer[] { 11, 12 }, originalValues.asList(false).toArray(Integer[]::new));
		assertArrayEquals(new Integer[] { 11, null, null, null, null, 12 }, originalValues.asList(true).toArray(Integer[]::new));

		var serializedValues = serializer.serialize(originalValues);
		var deserializedValues = serializer.deserialize(serializedValues);

		assertEquals(originalValues, deserializedValues);

		var emptyOriginalValues = TimeRangeValues.builder(start, end, DurationUnit.ofMinutes(15), Integer.class) //
				.build();

		serializedValues = serializer.serialize(emptyOriginalValues);
		deserializedValues = serializer.deserialize(serializedValues);

		assertEquals(emptyOriginalValues, deserializedValues);
	}

	protected static Instant instant(String time) {
		return Instant.parse("2026-02-03T" + time + "Z");
	}

	private static <T> void assertEqualTrv(io.openems.common.utils.TimeRangeValues<T> source,
			io.openems.common.utils.TimeRangeValues<T> dest, Instant sourceTime, Instant destTime) {
		assertEquals(source.getAt(sourceTime), dest.getAt(destTime));
	}
}
