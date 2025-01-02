package io.openems.edge.io.gpio.hardware;

import java.util.List;
import java.util.Optional;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.io.gpio.api.AbstractGpioChannel;
import io.openems.edge.io.gpio.api.ReadChannelId;
import io.openems.edge.io.gpio.api.WriteChannelId;

public interface HardwarePlatform {

	/**
	 * Gets all channels of the hardware.
	 * 
	 * @return List of hardware pins.
	 */
	public List<ChannelId> getAllChannelIds();

	/**
	 * Gets the write channel IDs of the platform.
	 * 
	 * @return List of WriteChannelIds.
	 */
	public default List<WriteChannelId> getWriteChannelIds() {
		return this.getAllChannelIds().stream()//
				.filter(WriteChannelId.class::isInstance)//
				.map(WriteChannelId.class::cast)//
				.toList();
	}

	/**
	 * Gets all read channels of the hardware device. These include digital and
	 * analog inputs.
	 * 
	 * @return List of hardware pins.
	 */
	public default List<ReadChannelId> getReadChannelIds() {
		return this.getAllChannelIds().stream()//
				.filter(ReadChannelId.class::isInstance)//
				.map(ReadChannelId.class::cast)//
				.toList();
	}

	/**
	 * Creates pin objects based on hardware enum description.
	 * 
	 * @param channels List of hardware description values. Each of the will be
	 *                 exported as a channel.
	 */
	public void createPinObjects(List<ChannelId> channels);

	/**
	 * Gets the value of a GPIO pin based on the given channel.
	 * 
	 * @param channelId hardware channel to be queried.
	 * @return the value of the digital IO. <code>true</code> if high, otherwise
	 *         <code>false</code>.
	 */
	public Optional<Boolean> getGpioValueByChannelId(AbstractGpioChannel channelId);

	/**
	 * Sets the value of a GPIO based.
	 * 
	 * @param channelId hardware channel to set
	 * @param value     the new requested value. In case of digital IOs, the value
	 *                  should be <code>boolean</code>.
	 * @throws OpenemsException thrown in the case if there is an OS/Hardware
	 *                          failure.
	 */
	public void setGpio(WriteChannelId channelId, boolean value) throws OpenemsException;
}
