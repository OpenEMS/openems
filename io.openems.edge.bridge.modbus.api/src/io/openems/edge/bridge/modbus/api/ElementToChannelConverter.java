package io.openems.edge.bridge.modbus.api;

import java.util.Optional;
import java.util.function.Function;

/**
 * Provides Functions to convert from Element to Channel and back.
 */
public class ElementToChannelConverter {

	/**
	 * Converts directly 1-to-1 between Element and Channel
	 */
	public final static ElementToChannelConverter CONVERT_1_TO_1 = new ElementToChannelConverter( //
			// element -> channel
			value -> Optional.ofNullable(value), //
			// channel -> element
			value -> value);

	/**
	 * Converts only positive values from Element to Channel
	 */
	public final static ElementToChannelConverter CONVERT_POSITIVE = new ElementToChannelConverter( //
			// element -> channel
			value -> {
				if (!(value instanceof Integer)) {
					throw new IllegalArgumentException("CONVERT_POSITIVE accepts only Integer type");
				}
				int intValue = (int) value;
				if (intValue >= 0) {
					return Optional.of(intValue);
				} else {
					return Optional.of(0);
				}
			}, //
				// channel -> element
			value -> value);

	/**
	 * Converts only negative values from Element to Channel and inverts them (makes
	 * the value positive)
	 */
	public final static ElementToChannelConverter CONVERT_NEGATIVE_AND_INVERT = new ElementToChannelConverter( //
			// element -> channel
			value -> {
				if (!(value instanceof Integer)) {
					throw new IllegalArgumentException("CONVERT_NEGATIVE_AND_INVERT accepts only Integer type");
				}
				int intValue = (int) value;
				if (intValue >= 0) {
					return Optional.of(0);
				} else {
					return Optional.of(intValue * -1);
				}
			}, //
				// channel -> element
			value -> value);

	private final Function<Object, Optional<Object>> elementToChannel;
	private final Function<Object, Object> channelToElement;

	public ElementToChannelConverter(Function<Object, Optional<Object>> elementToChannel,
			Function<Object, Object> channelToElement) {
		this.elementToChannel = elementToChannel;
		this.channelToElement = channelToElement;
	}

	/**
	 * Convert an Element value to an optional Channel value. If the value can or
	 * should not be converted, this method returns Optional.empty.
	 * 
	 * @param value
	 * @return
	 */
	public Optional<Object> elementToChannel(Object value) {
		return this.elementToChannel.apply(value);
	}

	public Object channelToElement(Object value) {
		return this.channelToElement.apply(value);
	}
}
