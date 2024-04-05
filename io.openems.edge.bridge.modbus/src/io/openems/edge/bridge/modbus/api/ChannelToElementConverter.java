package io.openems.edge.bridge.modbus.api;

import java.util.function.Function;

/**
 * Provides Functions to convert from Element to Channel and back. Also has some
 * static convenience functions to facilitate conversion.
 */
public class ChannelToElementConverter implements Function<Object, Object> {

	/**
	 * Converts directly 1-to-1 between Channel and Element.
	 */
	public static final ChannelToElementConverter DIRECT_1_TO_1 = new ChannelToElementConverter(value -> value);

	private final Function<Object, Object> function;

	public ChannelToElementConverter(Function<Object, Object> function) {
		this.function = function;
	}

	@Override
	public Object apply(Object t) {
		return this.function.apply(t);
	}
}
