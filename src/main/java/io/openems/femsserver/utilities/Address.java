package io.openems.femsserver.utilities;

public class Address {
	private final String thingId;
	private final String channelId;

	public Address(String thingId, String channelId) {
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

	public static Address fromString(String address) {
		String[] addressArray = address.split("/");
		String thingId = addressArray[0];
		String channelId = addressArray[1];
		return new Address(thingId, channelId);
	}
}
