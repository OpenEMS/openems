package io.openems.edge.common.type;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Optional;

import org.junit.Test;

import io.openems.common.function.ThrowingRunnable;
import io.openems.common.types.OpenemsType;
import io.openems.common.types.OptionsEnum;
import io.openems.edge.common.channel.value.Value;

public class TypeUtilsTest {

	@Test
	public void testAverageInt() {
		// int values input, avg - rounding previous value
		assertEquals(Integer.valueOf(0), TypeUtils.averageInt(0, 0, 1));

		// int values
		assertEquals(Integer.valueOf(10), TypeUtils.averageInt(5, 10, 15));

		// int values input, avg - rounding to next value
		assertEquals(Integer.valueOf(11), TypeUtils.averageInt(10, 11));

		// null values
		assertEquals(null, TypeUtils.averageInt(null, null));
	}

	@Test
	public void testAverage() {
		// null values
		assertEquals(null, TypeUtils.average(null, null, null));

		// int value
		assertEquals(Integer.valueOf(2), TypeUtils.averageRounded(1, 2, 3));

		// float values
		assertEquals(2.5f, TypeUtils.average(2F, 3F), 0.001);

		// mixed values
		assertEquals(2.5f, TypeUtils.average(2, null, 3), 0.001);
	}

	@Test
	public void testAverageRounded() {
		// no values
		assertEquals(null, TypeUtils.averageRounded());

		// null values
		assertEquals(null, TypeUtils.averageRounded(null, null, null));

		// int value
		assertEquals(Integer.valueOf(2), TypeUtils.averageRounded(1, 2, 3));

		// float values
		assertEquals(Integer.valueOf(3), TypeUtils.averageRounded(2, 3));

		// mixed values
		assertEquals(Integer.valueOf(3), TypeUtils.averageRounded(2, null, 3));
	}

	@Test
	public void testFitWithin() {
		assertEquals(10, TypeUtils.fitWithin(5, 15, 10));
		assertEquals(5.0, TypeUtils.fitWithin(2.5, 7.5, 5.0), 0.0);
		assertEquals(0F, TypeUtils.fitWithin(0F, 100F, -99.005F), 0F);
		assertEquals(100F, TypeUtils.fitWithin(0F, 100F, 100.005F), 0F);
		assertEquals(50F, TypeUtils.fitWithin(0F, 100F, 50F), 0F);
	}

	@Test
	public void testMin() {
		assertEquals(25, (int) TypeUtils.min(null, 25, null, 40, null));
		assertEquals(null, TypeUtils.min((Double) null, null, null));
		assertEquals(17, (int) TypeUtils.min(null, 17, 25, 40));
		assertEquals(34, (int) TypeUtils.min(null, 34, 40));
	}

	@Test
	public void testSumDouble() {
		assertNull(TypeUtils.sum((Double) null, null));
		assertEquals(4.0, TypeUtils.sum(1.5, 2.5), 0.1);
	}

	@Test
	public void testSubtractDouble() {
		assertNull(TypeUtils.subtract((Double) null, null));
		assertEquals(2.0, TypeUtils.subtract(4.5, 2.5), 0.1);
		assertEquals(4.5, TypeUtils.subtract(4.5, null), 0.1);
	}

	@Test
	public void testGetAsType() {
		/*
		 * Extract values
		 */
		{
			var expected = Short.valueOf((short) 123);
			assertEquals(expected, getAsShort(new Value<>(null, 123)));
			assertEquals(expected, getAsShort(Optional.of(123)));
			assertNull(getAsShort(Optional.empty()));
			assertEquals(expected, getAsShort(MyOptionsEnum.E123));
			assertEquals(Short.valueOf((short) 0), getAsShort(MyEnum.E123));
			assertEquals(expected, getAsShort(new Integer[] { 123 }));
			assertNull(getAsShort(new Integer[0]));
		}
		/*
		 * Special floating point numbers
		 */
		{
			assertNull(getAsFloat(Float.NaN));
			assertNull(getAsFloat(Float.NEGATIVE_INFINITY));
			assertNull(getAsFloat(Float.POSITIVE_INFINITY));
			assertNull(getAsDouble(Double.NaN));
			assertNull(getAsDouble(Double.NEGATIVE_INFINITY));
			assertNull(getAsDouble(Double.POSITIVE_INFINITY));
		}
		/*
		 * To BOOLEAN
		 */
		{
			assertNull(getAsBoolean(null));
			assertEquals(true, getAsBoolean(Boolean.TRUE));
			assertEquals(false, getAsBoolean(Boolean.FALSE));
			assertEquals(true, getAsBoolean(Short.valueOf((short) 1)));
			assertEquals(false, getAsBoolean(Short.valueOf((short) 0)));
			assertEquals(true, getAsBoolean(Integer.valueOf(1)));
			assertEquals(false, getAsBoolean(Integer.valueOf(0)));
			assertEquals(true, getAsBoolean(Long.valueOf(1)));
			assertEquals(false, getAsBoolean(Long.valueOf(0)));
			assertEquals(true, getAsBoolean(Float.valueOf(1)));
			assertEquals(false, getAsBoolean(Float.valueOf(0)));
			assertEquals(true, getAsBoolean(Double.valueOf(1)));
			assertEquals(false, getAsBoolean(Double.valueOf(0)));
			assertNull(getAsBoolean(""));
			assertEquals(false, getAsBoolean("fAlSe"));
			assertEquals(true, getAsBoolean("tRuE"));
			assertException(() -> getAsBoolean("foo"));
			assertException(() -> getAsBoolean(new Object()));
		}
		/*
		 * To SHORT
		 */
		{
			assertNull(getAsShort(null));
			assertEquals(Short.valueOf((short) 1), getAsShort(Boolean.TRUE));
			assertEquals(Short.valueOf((short) 0), getAsShort(Boolean.FALSE));
			var expected = Short.valueOf((short) 123);
			assertEquals(expected, getAsShort(expected));
			assertException(() -> getAsShort(Integer.valueOf(Short.MAX_VALUE + 1)));
			assertException(() -> getAsShort(Integer.valueOf(Short.MIN_VALUE - 1)));
			assertEquals(expected, getAsShort(Integer.valueOf(123)));
			assertException(() -> getAsShort(Long.valueOf(Short.MAX_VALUE + 1)));
			assertException(() -> getAsShort(Long.valueOf(Short.MIN_VALUE - 1)));
			assertEquals(expected, getAsShort(Long.valueOf(123)));
			assertException(() -> getAsShort(Float.valueOf(Short.MAX_VALUE + 1)));
			assertException(() -> getAsShort(Float.valueOf(Short.MIN_VALUE - 1)));
			assertEquals(expected, getAsShort(Float.valueOf(123)));
			assertException(() -> getAsShort(Double.valueOf(Short.MAX_VALUE + 1)));
			assertException(() -> getAsShort(Double.valueOf(Short.MIN_VALUE - 1)));
			assertEquals(expected, getAsShort(Double.valueOf(123)));
			assertNull(getAsShort(""));
			assertException(() -> getAsShort("foo"));
			assertEquals(expected, getAsShort("123"));
			assertException(() -> getAsShort(new Object()));
		}

		/*
		 * To INTEGER
		 */
		{
			assertNull(getAsInteger(null));
			assertEquals(Integer.valueOf(0), getAsInteger(Boolean.FALSE));
			assertEquals(Integer.valueOf(1), getAsInteger(Boolean.TRUE));
			var expected = Integer.valueOf(123);
			assertEquals(expected, getAsInteger(Short.valueOf((short) 123)));
			assertEquals(expected, getAsInteger(123));
			assertException(() -> getAsInteger(Long.valueOf(Long.valueOf(Integer.MAX_VALUE) + 1)));
			assertException(() -> getAsInteger(Long.valueOf(Long.valueOf(Integer.MIN_VALUE) - 1)));
			assertEquals(expected, getAsInteger(Long.valueOf(123)));
			assertException(() -> getAsInteger(Float.valueOf(Float.valueOf(Integer.MAX_VALUE) + 1000)));
			assertException(() -> getAsInteger(Float.valueOf(Float.valueOf(Integer.MIN_VALUE) - 1000)));
			assertEquals(expected, getAsInteger(Float.valueOf(123)));
			assertException(() -> getAsInteger(Double.valueOf(Double.valueOf(Integer.MAX_VALUE) + 1000)));
			assertException(() -> getAsInteger(Double.valueOf(Double.valueOf(Integer.MIN_VALUE) - 1000)));
			assertEquals(expected, getAsInteger(Double.valueOf(123)));
			assertNull(getAsInteger(""));
			assertException(() -> getAsInteger("foo"));
			assertEquals(expected, getAsInteger("123"));
			assertException(() -> getAsInteger(new Object()));
		}

		/*
		 * To LONG
		 */
		{
			assertNull(getAsLong(null));
			assertEquals(Long.valueOf(0), getAsLong(Boolean.FALSE));
			assertEquals(Long.valueOf(1), getAsLong(Boolean.TRUE));
			var expected = Long.valueOf(123);
			assertEquals(expected, getAsLong(Short.valueOf((short) 123)));
			assertEquals(expected, getAsLong(123));
			assertEquals(expected, getAsLong(Long.valueOf(123)));
			assertEquals(expected, getAsLong(Float.valueOf(123)));
			assertEquals(expected, getAsLong(Double.valueOf(123)));
			assertNull(getAsLong(""));
			assertException(() -> getAsLong("foo"));
			assertEquals(expected, getAsLong("123"));
			assertException(() -> getAsLong(new Object()));
		}

		/*
		 * To FLOAT
		 */
		{
			assertNull(getAsFloat(null));
			assertEquals(Float.valueOf(0), getAsFloat(Boolean.FALSE));
			assertEquals(Float.valueOf(1), getAsFloat(Boolean.TRUE));
			var expected = Float.valueOf(123);
			assertEquals(expected, getAsFloat(Short.valueOf((short) 123)));
			assertEquals(expected, getAsFloat(123));
			assertEquals(expected, getAsFloat(Long.valueOf(123)));
			assertEquals(expected, getAsFloat(Float.valueOf(123)));
			assertEquals(expected, getAsFloat(Double.valueOf(123)));
			assertNull(getAsFloat(""));
			assertException(() -> getAsFloat("foo"));
			assertEquals(expected, getAsFloat("123"));
			assertException(() -> getAsFloat(new Object()));
			assertEquals(Float.valueOf(0.0f), getAsFloat(Double.valueOf(0.0)));
		}

		/*
		 * To DOUBLE
		 */
		{
			assertNull(getAsDouble(null));
			assertEquals(Double.valueOf(0), getAsDouble(Boolean.FALSE));
			assertEquals(Double.valueOf(1), getAsDouble(Boolean.TRUE));
			var expected = Double.valueOf(123);
			assertEquals(expected, getAsDouble(Short.valueOf((short) 123)));
			assertEquals(expected, getAsDouble(123));
			assertEquals(expected, getAsDouble(Long.valueOf(123)));
			assertEquals(expected, getAsDouble(Float.valueOf(123)));
			assertEquals(expected, getAsDouble(Double.valueOf(123)));
			assertNull(getAsDouble(""));
			assertException(() -> getAsDouble("foo"));
			assertEquals(expected, getAsDouble("123"));
			assertException(() -> getAsDouble(new Object()));
		}

		/*
		 * To STRING
		 */
		{
			assertNull(getAsString(null));
			assertEquals("", getAsString(""));
			assertEquals("[Hello, [World, !]]", getAsString(new Object[] { "Hello", new Object[] { "World", "!" } }));
			assertEquals("[true, false]", getAsString(new boolean[] { true, false }));
			assertEquals("[1, 2]", getAsString(new byte[] { 1, 2 }));
			assertEquals("[f, o, o]", getAsString(new char[] { 'f', 'o', 'o' }));
			assertEquals("[0.1, 0.2]", getAsString(new double[] { 0.1, 0.2 }));
			assertEquals("[0.1, 0.2]", getAsString(new float[] { 0.1f, 0.2f }));
			assertEquals("[1, 2]", getAsString(new int[] { 1, 2 }));
			assertEquals("[1, 2]", getAsString(new long[] { 1, 2 }));
			assertEquals("[1, 2]", getAsString(new short[] { 1, 2 }));
		}
	}

	private static void assertException(ThrowingRunnable<Exception> runnable) {
		try {
			runnable.run();
			assertEquals("Expecting an Exception!", true, false);
		} catch (Exception e) {
			// ok
		}
	}

	private static Boolean getAsBoolean(Object value) {
		return TypeUtils.getAsType(OpenemsType.BOOLEAN, value);
	}

	private static Short getAsShort(Object value) {
		return TypeUtils.getAsType(OpenemsType.SHORT, value);
	}

	private static Integer getAsInteger(Object value) {
		return TypeUtils.getAsType(OpenemsType.INTEGER, value);
	}

	private static Long getAsLong(Object value) {
		return TypeUtils.getAsType(OpenemsType.LONG, value);
	}

	private static Float getAsFloat(Object value) {
		return TypeUtils.getAsType(OpenemsType.FLOAT, value);
	}

	private static Double getAsDouble(Object value) {
		return TypeUtils.getAsType(OpenemsType.DOUBLE, value);
	}

	private static String getAsString(Object value) {
		return TypeUtils.getAsType(OpenemsType.STRING, value);
	}

	private static enum MyOptionsEnum implements OptionsEnum {
		UNDEFINED(-1), //
		E123(123);

		private final int value;

		private MyOptionsEnum(int value) {
			this.value = value;
		}

		@Override
		public int getValue() {
			return this.value;
		}

		@Override
		public OptionsEnum getUndefined() {
			return UNDEFINED;
		}

		@Override
		public String getName() {
			return this.name();
		}
	}

	private static enum MyEnum {
		E123;
	}
}
