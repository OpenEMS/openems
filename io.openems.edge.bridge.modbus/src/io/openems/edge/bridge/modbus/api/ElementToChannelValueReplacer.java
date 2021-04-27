package io.openems.edge.bridge.modbus.api;

import io.openems.common.types.OpenemsType;

/**
 * Converts between Element and Channel, but if the element has a specific value, a replacement value is written in
 * the channel instead.
 * Only supports Short, Integer and Long.
 * 
 * <p>
 * if (element has value x) {
 *     channel = y
 * }
 * 
 * <p>
 * Example: A Modbus device sends the value "0x8000H" if the requested value is not available. This converter is then
 * used to scan for this value, and if detected writes "null" in the associated channel instead of "0x8000H".
 */
public class ElementToChannelValueReplacer extends ElementToChannelConverter {

	public ElementToChannelValueReplacer(int valueToReplace, int replacementValue) {
		super(//
				// element -> channel
				value -> {
					return replace(value, valueToReplace, replacementValue);
				}, //

				// channel -> element
				value -> value);
	}

	public ElementToChannelValueReplacer(int valueToReplace) {
		super(//
				// element -> channel
				value -> {
					return replaceWithNull(value, valueToReplace);
				}, //

				// channel -> element
				value -> value);
	}

	private static Object replace(Object value, int valueToReplace, int replacementValue) {
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
					if (Integer.toHexString((Short) value).equals(Integer.toHexString(valueToReplace))) {
						return (Integer) replacementValue;
					} else {
						return (Short) value;
					}
				}
				if (value instanceof Integer) {
					if (Integer.toHexString((Integer) value).equals(Integer.toHexString(valueToReplace))) {
						return (Integer) replacementValue;
					} else {
						return (Integer) value;
					}
				}
				if (value instanceof Long) {
					if (Long.toHexString((Long) value).equals(Long.toHexString(valueToReplace))) {
						return (Integer) replacementValue;
					} else {
						return (Long) value;
					}
				}
				if (value instanceof Float) {
					return (Float) value;
				}
				if (value instanceof Double) {
					return (Double) value;
				}
				if (value instanceof String) {
					return (String) value;
				}
			}
			break;
		}
		throw new IllegalArgumentException(
				"Type [" + value.getClass().getName() + "] not supported by REPLACE_VALUE converter");
	}

	private static Object replaceWithNull(Object value, int valueToReplace) {
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
						if (Integer.toHexString((Short) value).equals(Integer.toHexString(valueToReplace))) {
							return null;
						} else {
							return (Short) value;
						}
					}
					if (value instanceof Integer) {
						if (Integer.toHexString((Integer) value).equals(Integer.toHexString(valueToReplace))) {
							return null;
						} else {
							return (Integer) value;
						}
					}
					if (value instanceof Long) {
						if (Long.toHexString((Long) value).equals(Long.toHexString(valueToReplace))) {
							return null;
						} else {
							return (Long) value;
						}
					}
					if (value instanceof Float) {
						return (Float) value;
					}
					if (value instanceof Double) {
						return (Double) value;
					}
					if (value instanceof String) {
						return (String) value;
					}
			}
			break;
		}
		throw new IllegalArgumentException(
				"Type [" + value.getClass().getName() + "] not supported by REPLACE_VALUE converter");
	}
}
