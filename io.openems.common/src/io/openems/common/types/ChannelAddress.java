package io.openems.common.types;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;

public class ChannelAddress implements Comparable<ChannelAddress> {

	private final String componentId;
	private final String channelId;

	public ChannelAddress(String componentId, String channelId) {
		super();
		this.componentId = componentId;
		this.channelId = channelId;
	}

	public String getComponentId() {
		return componentId;
	}

	public String getChannelId() {
		return channelId;
	}

	@Override
	public String toString() {
		return componentId + "/" + channelId;
	}

	public static ChannelAddress fromString(String address) throws OpenemsNamedException {
		try {
			String[] addressArray = address.split("/");
			String componentId = addressArray[0];
			String channelId = addressArray[1];
			return new ChannelAddress(componentId, channelId);
		} catch (Exception e) {
			throw OpenemsError.COMMON_NO_VALID_CHANNEL_ADDRESS.exception(address);
		}
	}

	@Override
	public int compareTo(ChannelAddress other) {
		return this.toString().compareTo(other.toString());
	}

	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ChannelAddress other = (ChannelAddress) obj;
		return this.toString().equals(other.toString());
	}
}
