package io.openems.common.types;

public class ChannelAddress {
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

	public static ChannelAddress fromString(String address) {
		String[] addressArray = address.split("/");
		String thingId = addressArray[0];
		String channelId = addressArray[1];
		return new ChannelAddress(thingId, channelId);
	}
}
