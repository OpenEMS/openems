package io.openems.edge.common.type;

import static org.junit.Assert.assertEquals;

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
	public void testMin() {
		assertEquals(25, (int) TypeUtils.min(null, 25, null, 40, null));
		assertEquals(null, TypeUtils.min((Double) null, null, null));
		assertEquals(17, (int) TypeUtils.min(null, 17, 25, 40));
		assertEquals(34, (int) TypeUtils.min(null, 34, 40));
	}

	@Test
	public void testGetAsType() {
		/*
		 * Extract values
		 */
		{
			var expected = Short.valueOf((short) 123);
			assertEquals(expected, this.getAsShort(new Value<>(null, 123)));
			assertEquals(expected, this.getAsShort(Optional.of(123)));
			assertEquals(expected, this.getAsShort(MyOptionsEnum.E123));
			assertEquals(Short.valueOf((short) 0), this.getAsShort(MyEnum.E123));
			assertEquals(expected, this.getAsShort(new Integer[] { 123 }));
			assertEquals(null, this.getAsShort(new Integer[0]));
		}
		/*
		 * To BOOLEAN
		 */
		{
			assertEquals(null, this.getAsBoolean(null));
			assertEquals(true, this.getAsBoolean(Boolean.TRUE));
			assertEquals(false, this.getAsBoolean(Boolean.FALSE));
			assertEquals(true, this.getAsBoolean(Short.valueOf((short) 1)));
			assertEquals(false, this.getAsBoolean(Short.valueOf((short) 0)));
			assertEquals(true, this.getAsBoolean(Integer.valueOf(1)));
			assertEquals(false, this.getAsBoolean(Integer.valueOf(0)));
			assertEquals(true, this.getAsBoolean(Long.valueOf(1)));
			assertEquals(false, this.getAsBoolean(Long.valueOf(0)));
			assertEquals(true, this.getAsBoolean(Float.valueOf(1)));
			assertEquals(false, this.getAsBoolean(Float.valueOf(0)));
			assertEquals(true, this.getAsBoolean(Double.valueOf(1)));
			assertEquals(false, this.getAsBoolean(Double.valueOf(0)));
			assertEquals(null, this.getAsBoolean(""));
			assertEquals(false, this.getAsBoolean("fAlSe"));
			assertEquals(true, this.getAsBoolean("tRuE"));
			this.assertException(() -> this.getAsBoolean("foo"));
			this.assertException(() -> this.getAsBoolean(new Object()));
		}
		/*
		 * To SHORT
		 */
		{
			assertEquals(null, this.getAsShort(null));
			assertEquals(Short.valueOf((short) 1), this.getAsShort(Boolean.TRUE));
			assertEquals(Short.valueOf((short) 0), this.getAsShort(Boolean.FALSE));
			var expected = Short.valueOf((short) 123);
			assertEquals(expected, this.getAsShort(expected));
			this.assertException(() -> this.getAsShort(Integer.valueOf(Short.MAX_VALUE + 1)));
			this.assertException(() -> this.getAsShort(Integer.valueOf(Short.MIN_VALUE - 1)));
			assertEquals(expected, this.getAsShort(Integer.valueOf(123)));
			this.assertException(() -> this.getAsShort(Long.valueOf(Short.MAX_VALUE + 1)));
			this.assertException(() -> this.getAsShort(Long.valueOf(Short.MIN_VALUE - 1)));
			assertEquals(expected, this.getAsShort(Long.valueOf(123)));
			this.assertException(() -> this.getAsShort(Float.valueOf(Short.MAX_VALUE + 1)));
			this.assertException(() -> this.getAsShort(Float.valueOf(Short.MIN_VALUE - 1)));
			assertEquals(expected, this.getAsShort(Float.valueOf(123)));
			this.assertException(() -> this.getAsShort(Double.valueOf(Short.MAX_VALUE + 1)));
			this.assertException(() -> this.getAsShort(Double.valueOf(Short.MIN_VALUE - 1)));
			assertEquals(expected, this.getAsShort(Double.valueOf(123)));
			assertEquals(null, this.getAsShort(""));
			this.assertException(() -> this.getAsShort("foo"));
			assertEquals(expected, this.getAsShort("123"));
			this.assertException(() -> this.getAsShort(new Object()));
		}

		/*
		 * To INTEGER
		 */
		{
			assertEquals(null, this.getAsInteger(null));
			assertEquals(Integer.valueOf(0), this.getAsInteger(Boolean.FALSE));
			assertEquals(Integer.valueOf(1), this.getAsInteger(Boolean.TRUE));
			var expected = Integer.valueOf(123);
			assertEquals(expected, this.getAsInteger(Short.valueOf((short) 123)));
			assertEquals(expected, this.getAsInteger(123));
			this.assertException(() -> this.getAsInteger(Long.valueOf(Long.valueOf(Integer.MAX_VALUE) + 1)));
			this.assertException(() -> this.getAsInteger(Long.valueOf(Long.valueOf(Integer.MIN_VALUE) - 1)));
			assertEquals(expected, this.getAsInteger(Long.valueOf(123)));
			this.assertException(() -> this.getAsInteger(Float.valueOf(Float.valueOf(Integer.MAX_VALUE) + 1000)));
			this.assertException(() -> this.getAsInteger(Float.valueOf(Float.valueOf(Integer.MIN_VALUE) - 1000)));
			assertEquals(expected, this.getAsInteger(Float.valueOf(123)));
			this.assertException(() -> this.getAsInteger(Double.valueOf(Double.valueOf(Integer.MAX_VALUE) + 1000)));
			this.assertException(() -> this.getAsInteger(Double.valueOf(Double.valueOf(Integer.MIN_VALUE) - 1000)));
			assertEquals(expected, this.getAsInteger(Double.valueOf(123)));
			assertEquals(null, this.getAsInteger(""));
			this.assertException(() -> this.getAsInteger("foo"));
			assertEquals(expected, this.getAsInteger("123"));
			this.assertException(() -> this.getAsInteger(new Object()));
		}

		/*
		 * To LONG
		 */
		{
			assertEquals(null, this.getAsLong(null));
			assertEquals(Long.valueOf(0), this.getAsLong(Boolean.FALSE));
			assertEquals(Long.valueOf(1), this.getAsLong(Boolean.TRUE));
			var expected = Long.valueOf(123);
			assertEquals(expected, this.getAsLong(Short.valueOf((short) 123)));
			assertEquals(expected, this.getAsLong(123));
			assertEquals(expected, this.getAsLong(Long.valueOf(123)));
			assertEquals(expected, this.getAsLong(Float.valueOf(123)));
			assertEquals(expected, this.getAsLong(Double.valueOf(123)));
			assertEquals(null, this.getAsLong(""));
			this.assertException(() -> this.getAsLong("foo"));
			assertEquals(expected, this.getAsLong("123"));
			this.assertException(() -> this.getAsLong(new Object()));
		}

		/*
		 * To FLOAT
		 */
		{
			assertEquals(null, this.getAsFloat(null));
			assertEquals(Float.valueOf(0), this.getAsFloat(Boolean.FALSE));
			assertEquals(Float.valueOf(1), this.getAsFloat(Boolean.TRUE));
			var expected = Float.valueOf(123);
			assertEquals(expected, this.getAsFloat(Short.valueOf((short) 123)));
			assertEquals(expected, this.getAsFloat(123));
			assertEquals(expected, this.getAsFloat(Long.valueOf(123)));
			assertEquals(expected, this.getAsFloat(Float.valueOf(123)));
			assertEquals(expected, this.getAsFloat(Double.valueOf(123)));
			assertEquals(null, this.getAsFloat(""));
			this.assertException(() -> this.getAsFloat("foo"));
			assertEquals(expected, this.getAsFloat("123"));
			this.assertException(() -> this.getAsFloat(new Object()));
			assertEquals(Float.valueOf(0.0f), this.getAsFloat(Double.valueOf(0.0)));
		}

		/*
		 * To DOUBLE
		 */
		{
			assertEquals(null, this.getAsDouble(null));
			assertEquals(Double.valueOf(0), this.getAsDouble(Boolean.FALSE));
			assertEquals(Double.valueOf(1), this.getAsDouble(Boolean.TRUE));
			var expected = Double.valueOf(123);
			assertEquals(expected, this.getAsDouble(Short.valueOf((short) 123)));
			assertEquals(expected, this.getAsDouble(123));
			assertEquals(expected, this.getAsDouble(Long.valueOf(123)));
			assertEquals(expected, this.getAsDouble(Float.valueOf(123)));
			assertEquals(expected, this.getAsDouble(Double.valueOf(123)));
			assertEquals(null, this.getAsDouble(""));
			this.assertException(() -> this.getAsDouble("foo"));
			assertEquals(expected, this.getAsDouble("123"));
			this.assertException(() -> this.getAsDouble(new Object()));
		}

		/*
		 * To STRING
		 */
		{
			assertEquals(null, this.getAsString(null));
			assertEquals("", this.getAsString(""));
			assertEquals("[Hello, [World, !]]",
					this.getAsString(new Object[] { "Hello", new Object[] { "World", "!" } }));
			assertEquals("[true, false]", this.getAsString(new boolean[] { true, false }));
			assertEquals("[1, 2]", this.getAsString(new byte[] { 1, 2 }));
			assertEquals("[f, o, o]", this.getAsString(new char[] { 'f', 'o', 'o' }));
			assertEquals("[0.1, 0.2]", this.getAsString(new double[] { 0.1, 0.2 }));
			assertEquals("[0.1, 0.2]", this.getAsString(new float[] { 0.1f, 0.2f }));
			assertEquals("[1, 2]", this.getAsString(new int[] { 1, 2 }));
			assertEquals("[1, 2]", this.getAsString(new long[] { 1, 2 }));
			assertEquals("[1, 2]", this.getAsString(new short[] { 1, 2 }));
		}
	}

	private void assertException(ThrowingRunnable<Exception> runnable) {
		try {
			runnable.run();
			assertEquals("Expecting an Exception!", true, false);
		} catch (Exception e) {
			// ok
		}
	}

	private Boolean getAsBoolean(Object value) {
		return TypeUtils.getAsType(OpenemsType.BOOLEAN, value);
	}

	private Short getAsShort(Object value) {
		return TypeUtils.getAsType(OpenemsType.SHORT, value);
	}

	private Integer getAsInteger(Object value) {
		return TypeUtils.getAsType(OpenemsType.INTEGER, value);
	}

	private Long getAsLong(Object value) {
		return TypeUtils.getAsType(OpenemsType.LONG, value);
	}

	private Float getAsFloat(Object value) {
		return TypeUtils.getAsType(OpenemsType.FLOAT, value);
	}

	private Double getAsDouble(Object value) {
		return TypeUtils.getAsType(OpenemsType.DOUBLE, value);
	}

	private String getAsString(Object value) {
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
