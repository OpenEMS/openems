package io.openems.edge.system.fenecon.home.enums;

import io.openems.common.types.ChannelAddress;

public enum LedOrder {
	DEFAULT_RED_BLUE_GREEN("DigitalOutput1", "DigitalOutput2", "DigitalOutput3"), //
	RED_GREEN_BLUE("DigitalOutput1", "DigitalOutput3", "DigitalOutput2"), //
	BLUE_RED_GREEN("DigitalOutput3", "DigitalOutput1", "DigitalOutput2"), //
	BLUE_GREEN_RED("DigitalOutput3", "DigitalOutput2", "DigitalOutput1"), //
	GREEN_RED_BLUE("DigitalOutput2", "DigitalOutput1", "DigitalOutput3"), //
	GREEN_BLUE_RED("DigitalOutput2", "DigitalOutput3", "DigitalOutput1");

	public final String ledRed;
	public final String ledBlue;
	public final String ledGreen;

	private LedOrder(String ledRed, String ledBlue, String ledGreen) {
		this.ledRed = ledRed;
		this.ledBlue = ledBlue;
		this.ledGreen = ledGreen;
	}

	/**
	 * Gets the {@link LedOrder.Actual}.
	 * 
	 * @param componentId the IO Component-ID
	 * @return the {@link LedOrder.Actual}
	 */
	public LedOrder.Actual toActual(String componentId) {
		return new LedOrder.Actual(//
				new ChannelAddress(componentId, this.ledRed), //
				new ChannelAddress(componentId, this.ledBlue), //
				new ChannelAddress(componentId, this.ledGreen));
	}

	public record Actual(ChannelAddress ledRed, ChannelAddress ledBlue, ChannelAddress ledGreen) {

		/**
		 * Returns all output {@link ChannelAddress}es.
		 *
		 * @return the {@link ChannelAddress}es
		 */
		public ChannelAddress[] getAllChannelAddresses() {
			return new ChannelAddress[] { this.ledRed, this.ledBlue, this.ledGreen };
		}

		/**
		 * Returns the output {@link ChannelAddress} for given color.
		 *
		 * @param color the {@link Color}
		 * @return the {@link ChannelAddress}
		 */
		public ChannelAddress getChannelAddressForColor(Color color) {
			return switch (color) {
			case RED -> this.ledRed;
			case GREEN -> this.ledGreen;
			case BLUE -> this.ledBlue;
			};
		}

		/**
		 * Returns the output {@link ChannelAddress} that DO NOT match the given color.
		 *
		 * @param color the {@link Color}
		 * @return the {@link ChannelAddress}es
		 */
		public ChannelAddress[] getRemainingChannelAddressesForColor(Color color) {
			return switch (color) {
			case RED -> new ChannelAddress[] { this.ledGreen, this.ledBlue };
			case GREEN -> new ChannelAddress[] { this.ledRed, this.ledBlue };
			case BLUE -> new ChannelAddress[] { this.ledRed, this.ledGreen };
			};
		}
	}
}
