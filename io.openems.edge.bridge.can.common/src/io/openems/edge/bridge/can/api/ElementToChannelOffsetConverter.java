package io.openems.edge.bridge.can.api;

import io.openems.common.types.OpenemsType;

/**
 * Converts between Element and Channel by applying an offset value.
 *
 * <p>
 * (channel = element + offset value)
 *
 * <p>
 * Example: if the Register is in unit [15 V] and this converter has a offset
 * value of '5' , it converts to unit [20 V]
 */
public class ElementToChannelOffsetConverter extends ElementToChannelConverter {

	public ElementToChannelOffsetConverter(int offset) {
		super(//
				// element -> channel
				value -> apply(value, offset), //

				// channel -> element
				value -> apply(value, -1 * offset));
	}

	private static Object apply(Object value, int offset) {
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
					double result = (Short) value + offset;
					if (result >= Short.MIN_VALUE && result <= Short.MAX_VALUE) {
						return Short.valueOf((short) result);
					}
					if (result > Integer.MIN_VALUE && result < Integer.MAX_VALUE) {
						return Integer.valueOf((int) result);
					}
					return Double.valueOf(Math.round(result));
				}
				if (value instanceof Integer) {
					double result = (Integer) value + offset;
					if (result >= Integer.MIN_VALUE && result <= Integer.MAX_VALUE) {
						return Integer.valueOf((int) result);
					}
					return Double.valueOf(Math.round(result));
				}
				if (value instanceof Long) {
					double result = (Long) value + offset;
					return Math.round(result);
				}
				if (value instanceof Float) {
					double result = (Float) value + offset;
					if (result >= Float.MIN_VALUE && result <= Float.MAX_VALUE) {
						return Float.valueOf((float) result);
					}
					return Double.valueOf(result);
				}
				if (value instanceof Double) {
					return Double.valueOf((Double) value + offset);
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
