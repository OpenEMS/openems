package io.openems.edge.io.gpio.api;

import static io.openems.edge.common.type.TypeUtils.assertNull;

import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.Doc;

public abstract class AbstractGpioChannel implements ChannelId {

	/**
	 * The GPIO number of the hardware entity.
	 */
	public final int gpio;

	/**
	 * The human readable name of the GPIO pin.
	 * 
	 * <p>
	 * NOTE: Channel names should written in UPPER_CASE format because OpenEMS
	 * converts the names to UpperCamelCase format. Be aware that the channel
	 * 'DIGITAL_INPUT_1' will be converted to 'DigitalInput1'
	 * 
	 * @param gpio number of the GPIO pin on the device.
	 * @param name human readable name of the GPIO pin.
	 */
	private final String name;

	/**
	 * The Channel-Doc.
	 */
	private final Doc doc;

	protected AbstractGpioChannel(int gpio, String name, Doc doc) throws IllegalArgumentException {
		assertNull("Channel name for GPIO [" + gpio + "]", name);
		this.gpio = gpio;
		this.name = name;
		this.doc = doc;
	}

	@Override
	public final String name() {
		return this.name;
	}

	@Override
	public final Doc doc() {
		return this.doc;
	}
}