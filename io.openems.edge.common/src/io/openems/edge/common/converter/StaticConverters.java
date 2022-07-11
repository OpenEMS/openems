package io.openems.edge.common.converter;

import java.util.function.Function;

import io.openems.common.types.OpenemsType;

public class StaticConverters {

	/**
	 * Converts only positive values from Element to Channel.
	 */
	public static final Function<Object, Object> KEEP_POSITIVE = value -> {
		if (value == null) {
			return null;
		}
		for (OpenemsType openemsType : OpenemsType.values()) {
			// this 'for' + 'switch' is only utilized to get an alert by Eclipse IDE if a
			// new OpenemsType was added. ("The enum constant XX needs a corresponding case
			// label in this enum switch on OpenemsType")
			switch (openemsType) {
			case BOOLEAN:
			case SHORT:
			case INTEGER:
			case LONG:
			case FLOAT:
			case DOUBLE:
			case STRING:
				if (value instanceof Boolean || value instanceof String) {
					return value; // impossible
				}
				if (value instanceof Short) {
					short shortValue = (Short) value;
					if (shortValue > 0) {
						return shortValue;
					} else {
						return 0;
					}
				} else if (value instanceof Integer) {
					int intValue = (Integer) value;
					if (intValue > 0) {
						return intValue;
					} else {
						return 0;
					}
				} else if (value instanceof Long) {
					long longValue = (Long) value;
					if (longValue > 0) {
						return longValue;
					} else {
						return 0;
					}
				} else if (value instanceof Float) {
					float floatValue = (Float) value;
					if (floatValue > 0) {
						return floatValue;
					} else {
						return 0;
					}
				} else if (value instanceof Double) {
					double doubleValue = (Double) value;
					if (doubleValue > 0) {
						return doubleValue;
					} else {
						return 0;
					}
				}
			}
			break;
		}
		throw new IllegalArgumentException("Converter KEEP_POSITIVE does not accept the type of [" + value + "]");
	};

	/**
	 * Invert a value.
	 */
	public static final Function<Object, Object> INVERT = value -> {
		if (value == null) {
			return null;
		}
		for (OpenemsType openemsType : OpenemsType.values()) {
			// this 'for' + 'switch' is only utilized to get an alert by Eclipse IDE if a
			// new OpenemsType was added. ("The enum constant XX needs a corresponding case
			// label in this enum switch on OpenemsType")
			switch (openemsType) {
			case BOOLEAN:
			case SHORT:
			case INTEGER:
			case LONG:
			case FLOAT:
			case DOUBLE:
			case STRING:
				if (value instanceof String) {
					return value; // impossible
				}
				if (value instanceof Boolean) {
					return Boolean.valueOf(!(boolean) value);
				} else if (value instanceof Short) {
					return Short.valueOf((short) ((short) value * -1));
				} else if (value instanceof Integer) {
					return Integer.valueOf((int) value * -1);
				} else if (value instanceof Long) {
					return Long.valueOf((long) value * -1);
				} else if (value instanceof Float) {
					return Float.valueOf((float) value * -1);
				} else if (value instanceof Double) {
					return Double.valueOf((double) value * -1);
				}
			}
			break;
		}
		throw new IllegalArgumentException("Converter INVERT does not accept the type of [" + value + "]");
	};
}
