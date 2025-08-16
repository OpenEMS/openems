package io.openems.edge.bridge.can.api;

import io.openems.common.types.OpenemsType;

/**
 * Converts between Element and Channel by multiplying a value.
 *
 * <p>
 * (channel = element * value)
 *
 * <p>
 * Example: if the Register is in unit [20 V] and this converter has a value of
 * '.5' , it converts to unit [10 V]
 */
public class ElementToChannelFactorConverter extends ElementToChannelConverter {

	public ElementToChannelFactorConverter(double factor) {
		super(//
				// element -> channel
				value -> apply(value, factor), //

				// channel -> element
				value -> apply(value, 1 / factor));
	}

	private static Object apply(Object value, double factor) {
		if (value == null) {
			return null;
		}
		for (OpenemsType openemsType : OpenemsType.values()) {
			// this 'for' + 'switch' is only utilized to get an alert by Eclipse IDE if a
			// new OpenemsType was added. ("The enum constant [...] needs a corresponding
			// case label in this enum switch on OpenemsType")
			switch (openemsType) {
			case BOOLEAN:
			case SHORT:
			case INTEGER:
			case LONG:
			case FLOAT:
			case DOUBLE:
			case STRING:
				if (value instanceof Boolean) {
					return (boolean) value;
				}
				if (value instanceof Short) {
					var result = (Short) value * factor;
					if (result >= Short.MIN_VALUE && result <= Short.MAX_VALUE) {
						return Short.valueOf((short) result);
					}
					if (result > Integer.MIN_VALUE && result < Integer.MAX_VALUE) {
						return Integer.valueOf((int) result);
					}
					return Double.valueOf(Math.round(result));
				}
				if (value instanceof Integer) {
					var result = (Integer) value * factor;
					if (result >= Integer.MIN_VALUE && result <= Integer.MAX_VALUE) {
						return Integer.valueOf((int) result);
					}
					return Double.valueOf(Math.round(result));
				}
				if (value instanceof Long) {
					var result = (Long) value * factor;
					return Math.round(result);
				}
				if (value instanceof Float) {
					var result = (Float) value * factor;
					if (result >= Float.MIN_VALUE && result <= Float.MAX_VALUE) {
						return Float.valueOf((float) result);
					}
					return Double.valueOf(result);
				}
				if (value instanceof Double) {
					return Double.valueOf((Double) value * factor);
				}
				if (value instanceof String) {
					return value;
				}
			}
			break;
		}
		throw new IllegalArgumentException(
				"Type [" + value.getClass().getName() + "] not supported by offset factor converter");
	}

}
