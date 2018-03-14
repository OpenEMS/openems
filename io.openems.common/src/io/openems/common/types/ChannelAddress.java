package io.openems.common.types;

import io.openems.common.exceptions.OpenemsException;

public class ChannelAddress implements Comparable<ChannelAddress> {
	private final String thingId;
	private final String channelId;

	public ChannelAddress(String thingId, String channelId) {
		super();
		this.thingId = thingId;
		this.channelId = channelId;
	}

	public String getThingId() {
		return thingId;
	}

	public String getChannelId() {
		return channelId;
	}

	@Override
	public String toString() {
		return thingId + "/" + channelId;
	}

	public static ChannelAddress fromString(String address) throws OpenemsException {
		try {
			String[] addressArray = address.split("/");
			String thingId = addressArray[0];
			String channelId = addressArray[1];
			return new ChannelAddress(thingId, channelId);
		} catch (Exception e) {
			throw new OpenemsException("This [" + address + "] is not a valid channel address.");
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
