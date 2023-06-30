package io.openems.edge.common.type;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Optional;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

import io.openems.common.types.OpenemsType;
import io.openems.common.types.OptionsEnum;
import io.openems.edge.common.channel.value.Value;

/**
 * Handles implicit conversions between {@link OpenemsType}s.
 */
public class TypeUtils {

	/**
	 * Converts and casts a Object to a given type.
	 *
	 * @param <T>   the Type for implicit casting of the result
	 * @param type  the type as {@link OpenemsType}
	 * @param value the value as {@link Object}
	 * @return the converted and casted value
	 * @throws IllegalArgumentException on error
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getAsType(OpenemsType type, Object value) throws IllegalArgumentException {
		// Extract Value containers
		if (value instanceof Value<?>) {
			value = ((Value<?>) value).get();
		}
		// Extract Optionals
		if (value instanceof Optional<?>) {
			value = ((Optional<?>) value).orElse(null);
		}
		// Extract OptionsEnum
		if (value instanceof OptionsEnum) {
			value = ((OptionsEnum) value).getValue();
		}
		// Extract Enum (lower priority than OptionsEnum)
		if (value instanceof Enum<?>) {
			value = ((Enum<?>) value).ordinal();
		}
		// Extract value from Array
		if (type != OpenemsType.STRING && value != null && value.getClass().isArray()) {
			if (Array.getLength(value) == 1) {
				return TypeUtils.getAsType(type, Array.get(value, 0));
			}
			return null;
		}

		switch (type) {
		case BOOLEAN:
			if (value == null) {
				return (T) (Boolean) value;

			}
			if (value instanceof Boolean) {
				return (T) (Boolean) value;

			} else if (value instanceof Short) {
				return (T) ((Short) value == 0 ? Boolean.FALSE : Boolean.TRUE);

			} else if (value instanceof Integer) {
				return (T) ((Integer) value == 0 ? Boolean.FALSE : Boolean.TRUE);

			} else if (value instanceof Long) {
				return (T) ((Long) value == 0 ? Boolean.FALSE : Boolean.TRUE);

			} else if (value instanceof Float) {
				return (T) ((Float) value == 0 ? Boolean.FALSE : Boolean.TRUE);

			} else if (value instanceof Double) {
				return (T) ((Double) value == 0 ? Boolean.FALSE : Boolean.TRUE);

			} else if (value instanceof String) {
				var stringValue = (String) value;
				if (stringValue.isEmpty()) {
					return null;
				} else if (stringValue.equalsIgnoreCase("false")) {
					return (T) Boolean.FALSE;
				} else if (stringValue.equalsIgnoreCase("true")) {
					return (T) Boolean.TRUE;
				} else {
					throw new IllegalArgumentException("Cannot convert String [" + value + "] to Boolean.");
				}
			}
			break;

		case SHORT:
			if (value == null) {
				return (T) (Short) value;

			}
			if (value instanceof Boolean) {
				boolean boolValue = (Boolean) value;
				return (T) Short.valueOf(boolValue ? (short) 1 : (short) 0);

			} else if (value instanceof Short) {
				return (T) (Short) value;

			} else if (value instanceof Integer) {
				int intValue = (Integer) value;
				if (intValue >= Short.MIN_VALUE && intValue <= Short.MAX_VALUE) {
					return (T) Short.valueOf((short) intValue);
				} else {
					throw new IllegalArgumentException(
							"Cannot convert. Integer [" + value + "] is not fitting in Short range.");
				}

			} else if (value instanceof Long) {
				long longValue = (Long) value;
				if (longValue >= Short.MIN_VALUE && longValue <= Short.MAX_VALUE) {
					return (T) Short.valueOf((short) longValue);
				} else {
					throw new IllegalArgumentException(
							"Cannot convert. Long [" + value + "] is not fitting in Short range.");
				}

			} else if (value instanceof Float) {
				float floatValue = (Float) value;
				var intValue = Math.round(floatValue);
				if (intValue >= Short.MIN_VALUE && intValue <= Short.MAX_VALUE) {
					return (T) Short.valueOf((short) intValue);
				} else {
					throw new IllegalArgumentException(
							"Cannot convert. Float [" + value + "] is not fitting in Short range.");
				}

			} else if (value instanceof Double) {
				double doubleValue = (Double) value;
				var longValue = Math.round(doubleValue);
				if (longValue >= Short.MIN_VALUE && longValue <= Short.MAX_VALUE) {
					return (T) Short.valueOf((short) longValue);
				} else {
					throw new IllegalArgumentException(
							"Cannot convert. Double [" + value + "] is not fitting in Short range.");
				}

			} else if (value instanceof String) {
				var stringValue = (String) value;
				if (stringValue.isEmpty()) {
					return null;
				}
				try {
					return (T) Short.valueOf(Short.parseShort(stringValue));
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException("Cannot convert String [" + stringValue + "] to Short.");
				}
			}
			break;

		case INTEGER:
			if (value == null) {
				return (T) (Integer) value;

			}
			if (value instanceof Boolean) {
				boolean boolValue = (Boolean) value;
				return (T) Integer.valueOf(boolValue ? 1 : 0);

			} else if (value instanceof Short) {
				return (T) Integer.valueOf((Short) value);

			} else if (value instanceof Integer) {
				return (T) (Integer) value;

			} else if (value instanceof Long) {
				long longValue = (Long) value;
				if (longValue >= Integer.MIN_VALUE && longValue <= Integer.MAX_VALUE) {
					return (T) Integer.valueOf((int) longValue);
				} else {
					throw new IllegalArgumentException(
							"Cannot convert. Long [" + value + "] is not fitting in Integer range.");
				}

			} else if (value instanceof Float) {
				float floatValue = (Float) value;
				if (floatValue >= Integer.MIN_VALUE && floatValue <= Integer.MAX_VALUE) {
					return (T) Integer.valueOf((int) floatValue);
				} else {
					throw new IllegalArgumentException(
							"Cannot convert. Float [" + value + "] is not fitting in Integer range.");
				}

			} else if (value instanceof Double) {
				double doubleValue = (Double) value;
				var longValue = Math.round(doubleValue);
				if (longValue >= Integer.MIN_VALUE && longValue <= Integer.MAX_VALUE) {
					return (T) Integer.valueOf((int) longValue);
				} else {
					throw new IllegalArgumentException(
							"Cannot convert. Double [" + value + "] is not fitting in Integer range.");
				}

			} else if (value instanceof String) {
				var stringValue = (String) value;
				if (stringValue.isEmpty()) {
					return null;
				}
				try {
					return (T) Integer.valueOf(Integer.parseInt(stringValue));
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException("Cannot convert String [" + stringValue + "] to Integer.");
				}
			}
			break;

		case LONG:
			if (value == null) {
				return (T) (Long) value;

			}
			if (value instanceof Boolean) {
				boolean boolValue = (Boolean) value;
				return (T) Long.valueOf(boolValue ? 1L : 0L);

			} else if (value instanceof Short) {
				return (T) (Long) ((Short) value).longValue();

			} else if (value instanceof Integer) {
				return (T) (Long) ((Integer) value).longValue();

			} else if (value instanceof Long) {
				return (T) (Long) value;

			} else if (value instanceof Float) {
				float floatValue = (Float) value;
				if (floatValue >= Long.MIN_VALUE && floatValue <= Long.MAX_VALUE) {
					return (T) Long.valueOf((long) floatValue);
				} else {
					throw new IllegalArgumentException(
							"Cannot convert. Float [" + value + "] is not fitting in Long range.");
				}

			} else if (value instanceof Double) {
				double doubleValue = (Double) value;
				var longValue = Math.round(doubleValue);
				if (longValue >= Long.MIN_VALUE && longValue <= Long.MAX_VALUE) {
					return (T) Long.valueOf(longValue);
				} else {
					throw new IllegalArgumentException(
							"Cannot convert. Double [" + value + "] is not fitting in Long range.");
				}

			} else if (value instanceof String) {
				var stringValue = (String) value;
				if (stringValue.isEmpty()) {
					return null;
				}
				try {
					return (T) Long.valueOf(Long.parseLong(stringValue));
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException("Cannot convert String [" + stringValue + "] to Long.");
				}
			}
			break;

		case FLOAT:
			if (value == null) {
				return (T) (Float) value;

			}
			if (value instanceof Boolean) {
				boolean boolValue = (Boolean) value;
				return (T) Float.valueOf(boolValue ? 1f : 0f);

			} else if (value instanceof Short) {
				return (T) (Float) ((Short) value).floatValue();

			} else if (value instanceof Integer) {
				return (T) (Float) ((Integer) value).floatValue();

			} else if (value instanceof Long) {
				return (T) (Float) ((Long) value).floatValue();

			} else if (value instanceof Float) {
				return (T) (Float) value;

			} else if (value instanceof Double) {
				// Returns the value of this Double as a float after a narrowing primitive
				// conversion.
				return (T) Float.valueOf(((Double) value).floatValue());

			} else if (value instanceof String) {
				var stringValue = (String) value;
				if (stringValue.isEmpty()) {
					return null;
				}
				try {
					return (T) Float.valueOf(Float.parseFloat(stringValue));
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException("Cannot convert String [" + stringValue + "] to Float.");
				}
			}
			break;

		case DOUBLE:
			if (value == null) {
				return (T) (Double) value;

			}
			if (value instanceof Boolean) {
				boolean boolValue = (Boolean) value;
				return (T) Double.valueOf(boolValue ? 1L : 0L);

			} else if (value instanceof Short) {
				return (T) Double.valueOf((Short) value);

			} else if (value instanceof Integer) {
				return (T) Double.valueOf((Integer) value);

			} else if (value instanceof Long) {
				return (T) Double.valueOf((Long) value);

			} else if (value instanceof Float) {
				return (T) Double.valueOf((Float) value);

			} else if (value instanceof Double) {
				return (T) (Double) value;

			} else if (value instanceof String) {
				var stringValue = (String) value;
				if (stringValue.isEmpty()) {
					return null;
				}
				try {
					return (T) Double.valueOf(Double.parseDouble(stringValue));
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException("Cannot convert String [" + stringValue + "] to Double.");
				}
			}
			break;

		case STRING:
			if (value == null) {
				return (T) (String) value;

			}
			if (value instanceof Object[]) {
				return (T) Arrays.deepToString((Object[]) value);

			} else if (value.getClass().isArray()) {
				if (value instanceof boolean[]) {
					return (T) Arrays.toString((boolean[]) value);
				} else if (value instanceof byte[]) {
					return (T) Arrays.toString((byte[]) value);
				} else if (value instanceof char[]) {
					return (T) Arrays.toString((char[]) value);
				} else if (value instanceof double[]) {
					return (T) Arrays.toString((double[]) value);
				} else if (value instanceof float[]) {
					return (T) Arrays.toString((float[]) value);
				} else if (value instanceof int[]) {
					return (T) Arrays.toString((int[]) value);
				} else if (value instanceof long[]) {
					return (T) Arrays.toString((long[]) value);
				} else if (value instanceof short[]) {
					return (T) Arrays.toString((short[]) value);
				} else {
					return (T) value.toString();
				}

			} else {
				return (T) value.toString();
			}

		}
		throw new IllegalArgumentException("Converter for value [" + value + "] of type [" + value.getClass()
				+ "] to type [" + type + "] is not implemented.");

	}

	/**
	 * Gets the value of the given type as {@link JsonElement}.
	 *
	 * @param type          the type as {@link OpenemsType}
	 * @param originalValue the value
	 * @return the converted value
	 */
	public static JsonElement getAsJson(OpenemsType type, Object originalValue) {
		if (originalValue == null) {
			return JsonNull.INSTANCE;
		}
		var value = TypeUtils.getAsType(type, originalValue);
		switch (type) {
		case BOOLEAN:
			return new JsonPrimitive((Boolean) value ? 1 : 0);
		case SHORT:
			return new JsonPrimitive((Short) value);
		case INTEGER:
			return new JsonPrimitive((Integer) value);
		case LONG:
			return new JsonPrimitive((Long) value);
		case FLOAT:
			return new JsonPrimitive((Float) value);
		case DOUBLE:
			return new JsonPrimitive((Double) value);
		case STRING:
			return new JsonPrimitive((String) value);
		}
		throw new IllegalArgumentException("Converter for value [" + value + "] to JSON is not implemented.");
	}

	/**
	 * Safely add Integers. If one of them is null it is considered '0'. If all of
	 * them are null, 'null' is returned.
	 *
	 * @param values the {@link Integer} values
	 * @return the sum
	 */
	public static Integer sum(Integer... values) {
		Integer result = null;
		for (Integer value : values) {
			if (value == null) {
				continue;
			}
			if (result == null) {
				result = value;
			} else {
				result += value;
			}
		}
		return result;
	}

	/**
	 * Safely add Longs. If one of them is null it is considered '0'. If all of them
	 * are null, 'null' is returned.
	 *
	 * @param values the {@link Long} values
	 * @return the sum
	 */
	public static Long sum(Long... values) {
		Long result = null;
		for (Long value : values) {
			if (value == null) {
				continue;
			}
			if (result == null) {
				result = value;
			} else {
				result += value;
			}
		}
		return result;
	}

	/**
	 * Safely subtract Integers.
	 *
	 * <ul>
	 * <li>if minuend is null -&gt; result is null
	 * <li>if subtrahend is null -&gt; result is minuend
	 * <li>if both are null -&gt; result is null
	 * </ul>
	 *
	 * @param minuend    the minuend of the subtraction
	 * @param subtrahend the subtrahend of the subtraction
	 * @return the result, possibly null
	 */
	public static Integer subtract(Integer minuend, Integer subtrahend) {
		if (minuend == null) {
			return null;
		}
		if (subtrahend == null) {
			return minuend;
		}
		return minuend - subtrahend;
	}

	/**
	 * Safely subtract Longs.
	 *
	 * <ul>
	 * <li>if minuend is null -&gt; result is null
	 * <li>if subtrahend is null -&gt; result is minuend
	 * <li>if both are null -&gt; result is null
	 * </ul>
	 *
	 * @param minuend    the minuend of the subtraction
	 * @param subtrahend the subtrahend of the subtraction
	 * @return the result, possibly null
	 */
	public static Long subtract(Long minuend, Long subtrahend) {
		if (minuend == null) {
			return null;
		}
		if (subtrahend == null) {
			return minuend;
		}
		return minuend - subtrahend;
	}

	/**
	 * Safely multiply Integers.
	 *
	 * @param firstFactor    first factor of the multiplication
	 * @param furtherFactors further factors of the multiplication
	 * @return the result, possibly null if the first factor is null
	 */
	public static Integer multiply(Integer firstFactor, Integer... furtherFactors) {
		if (firstFactor == null) {
			return null;
		}
		int result = firstFactor;
		for (Integer factor : furtherFactors) {
			if (factor != null) {
				result *= factor;
			}
		}
		return result;
	}

	/**
	 * Safely multiply Floats.
	 *
	 * @param factors the factors of the multiplication
	 * @return the result, possibly null if all factors are null
	 */
	public static Float multiply(Float... factors) {
		Float result = null;
		for (var factor : factors) {
			if (result == null) {
				result = factor;
			} else if (factor != null) {
				result *= factor;
			}
		}
		return result;
	}

	/**
	 * Safely multiply Doubles.
	 *
	 * @param factors the factors of the multiplication
	 * @return the result, possibly null if all factors are null
	 */
	public static Double multiply(Double... factors) {
		Double result = null;
		for (Double factor : factors) {
			if (result == null) {
				result = factor;
			} else if (factor != null) {
				result *= factor;
			}
		}
		return result;
	}

	/**
	 * Safely divides Integers.
	 *
	 * <ul>
	 * <li>if dividend is null -&gt; result is null
	 * </ul>
	 *
	 * @param dividend the dividend of the division
	 * @param divisor  the divisor of the division
	 * @return the result, possibly null
	 */
	public static Integer divide(Integer dividend, int divisor) {
		if (dividend == null) {
			return null;
		}
		return dividend / divisor;
	}

	/**
	 * Safely divides Longs.
	 *
	 * <ul>
	 * <li>if dividend is null -&gt; result is null
	 * </ul>
	 *
	 * @param dividend the dividend of the division
	 * @param divisor  the divisor of the division
	 * @return the result, possibly null
	 */
	public static Long divide(Long dividend, long divisor) {
		if (dividend == null) {
			return null;
		}
		return dividend / divisor;
	}

	/**
	 * Safely finds the max value of all values.
	 *
	 * @param values the {@link Integer} values
	 * @return the max value; or null if all values are null
	 */
	public static Integer max(Integer... values) {
		Integer result = null;
		for (Integer value : values) {
			if (value != null) {
				if (result == null) {
					result = value;
				} else {
					result = Math.max(result, value);
				}
			}
		}
		return result;
	}

	/**
	 * Safely finds the max value of all values.
	 *
	 * @param values the {@link Float} values
	 * @return the max value; or null if all values are null
	 */
	public static Float max(Float... values) {
		Float result = null;
		for (var value : values) {
			if (value != null) {
				if (result == null) {
					result = value;
				} else {
					result = Math.max(result, value);
				}
			}
		}
		return result;
	}

	/**
	 * Safely finds the min value of all values.
	 *
	 * @param values the {@link Integer} values
	 * @return the min value; or null if all values are null
	 */
	public static Integer min(Integer... values) {
		Integer result = null;
		for (Integer value : values) {
			if (result != null && value != null) {
				result = Math.min(result, value);
			} else if (value != null) {
				result = value;
			}
		}
		return result;
	}

	/**
	 * Safely finds the min value of all values.
	 *
	 * @param values the {@link Double} values
	 * @return the min value; or null if all values are null
	 */
	public static Double min(Double... values) {
		Double result = null;
		for (Double value : values) {
			if (value != null) {
				if (result == null) {
					result = value;
				} else {
					result = Math.min(result, value);
				}
			}
		}
		return result;
	}

	/**
	 * Safely finds the average value of all values.
	 *
	 * @param values the {@link Integer} values
	 * @return the average value; or null if all values are null
	 */
	public static Float average(Integer... values) {
		var count = 0;
		var sum = 0.f;
		for (Integer value : values) {
			if (value != null) {
				count++;
				sum += value;
			}
		}
		if (count == 0) {
			return null;
		}
		return sum / count;
	}

	/**
	 * Safely finds the average value of all values.
	 *
	 * @param values the double values
	 * @return the average value; or Double.NaN if all values are invalid.
	 */
	public static double average(double... values) {
		var count = 0;
		var sum = 0.;
		for (double value : values) {
			if (Double.isNaN(value)) {
				continue;
			}
			count++;
			sum += value;
		}
		if (count == 0) {
			return Double.NaN;
		}
		return sum / count;
	}

	/**
	 * Safely finds the average value of all values.
	 *
	 * @param values the {@link Integer} values
	 * @return the average value; or null if all values are null
	 */
	public static Integer averageInt(Integer... values) {
		var count = 0;
		float sum = 0;
		for (Integer value : values) {
			if (value != null) {
				count++;
				sum += value;
			}
		}
		if (count == 0) {
			return null;
		}
		return Math.round(sum / count);
	}

	/**
	 * Safely finds the average value of all values and rounds the result to an
	 * Integer using {@link Math#round(float)}.
	 *
	 * @param values the {@link Integer} values
	 * @return the rounded average value; or null if all values are null
	 */
	public static Integer averageRounded(Integer... values) {
		var result = average(values);
		if (result == null) {
			return null;
		}
		return Math.round(result);
	}

	/**
	 * Throws an descriptive exception if the object is null.
	 *
	 * @param description text that is added to the exception
	 * @param objects     the objects
	 * @throws IllegalArgumentException if any object is null
	 */
	public static void assertNull(String description, Object... objects) throws IllegalArgumentException {
		for (Object object : objects) {
			if (object == null) {
				throw new IllegalArgumentException(description + ": value is null!");
			}
		}
	}

	/**
	 * Safely convert from {@link Integer} to {@link Double}.
	 *
	 * @param value the Integer value, possibly null
	 * @return the Double value, possibly null
	 */
	public static Double toDouble(Integer value) {
		if (value == null) {
			return null;
		}
		return Double.valueOf(value);
	}

	/**
	 * Safely convert from {@link Float} to {@link Double}.
	 *
	 * @param value the Float value, possibly null
	 * @return the Double value, possibly null
	 */
	public static Double toDouble(Float value) {
		if (value == null) {
			return null;
		}
		return Double.valueOf(value);
	}

	/**
	 * Returns the 'alternativeValue' if the 'nullableValue' is null.
	 *
	 * @param <T>              the Type for implicit casting
	 * @param nullableValue    the value, can be null
	 * @param alternativeValue the alternative value
	 * @return either the value (not null), alternatively the 'orElse' value
	 */
	public static <T> T orElse(T nullableValue, T alternativeValue) {
		if (nullableValue != null) {
			return nullableValue;
		}
		return alternativeValue;
	}

	/**
	 * Fits a value within a lower and upper boundary.
	 *
	 * @param lowLimit  the lower boundary
	 * @param highLimit the upper boundary
	 * @param value     the actual value
	 * @return the adjusted value
	 */
	public static int fitWithin(int lowLimit, int highLimit, int value) {
		return Math.max(lowLimit, //
				Math.min(highLimit, value));
	}

	/**
	 * Safely returns the absolute value of an Integer value.
	 *
	 * @param value the Integer value, possibly null
	 * @return the absolute value, possibly null
	 */
	public static Integer abs(Integer value) {
		if (value == null) {
			return null;
		}
		return Math.abs(value);
	}
}
