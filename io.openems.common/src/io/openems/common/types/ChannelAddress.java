package io.openems.common.types;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;

import java.util.Objects;

public class ChannelAddress implements Comparable<ChannelAddress> {

	public static final String DELIMITER = "/";

	private final String componentId;
	private final String channelId;

	public ChannelAddress(String componentId, String channelId) {
		super();
		this.componentId = componentId;
		this.channelId = channelId;
	}

	/**
	 * Gets the Component-ID.
	 *
	 * @return the Component-ID
	 */
	public String getComponentId() {
		return componentId;
	}

	/**
	 * Gets the Channel-Id.
	 *
	 * @return the Channel-Id
	 */
	public String getChannelId() {
		return channelId;
	}

	@Override
	public String toString() {
		return componentId + DELIMITER + channelId;
	}

	/**
	 * Parses a string "Component-ID/Channel-ID" to a ChannelAddress.
	 *
	 * @param address the address as a String
	 * @return the ChannelAddress
	 * @throws OpenemsNamedException on parse error
	 */
	public static ChannelAddress fromString(String address) throws OpenemsNamedException {
		try {
			String[] addressArray = address.split(DELIMITER);
			String componentId = addressArray[0];
			String channelId = addressArray[1];
			return new ChannelAddress(componentId, channelId);
		} catch (Exception e) {
			throw OpenemsError.COMMON_NO_VALID_CHANNEL_ADDRESS.exception(address);
		}
	}

	public boolean matches(ChannelAddress other) {
		return this.channelId.matches(other.channelId) && this.componentId.matches(other.componentId);
	}


	@Override
	public int compareTo(ChannelAddress other) {
		return this.toString().compareTo(other.toString());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ChannelAddress that = (ChannelAddress) o;
		return Objects.equals(componentId, that.componentId)
			&& Objects.equals(channelId, that.channelId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(componentId, channelId);
	}
}
