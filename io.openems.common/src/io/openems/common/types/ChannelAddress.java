package io.openems.common.types;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.StringUtils;

public class ChannelAddress implements Comparable<ChannelAddress> {

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
		return componentId + "/" + channelId;
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
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		ChannelAddress other = (ChannelAddress) obj;
		return this.toString().equals(other.toString());
	}

	/**
	 * Match two ChannelAddresses, considering wildcards.
	 * 
	 * <ul>
	 * <li>if {@link #equals(Object)} is true -> return 0
	 * <li>if both {@link ChannelAddress}es match via wildcards -> return value > 1;
	 * bigger values represent a better match
	 * <li>if both {@link ChannelAddress}es do not match -> return -1
	 * </ul>
	 * 
	 * <p>
	 * See {@link StringUtils#matchWildcard(String, String)} for implementation
	 * details.
	 * 
	 * @param source  the source {@link ChannelAddress}
	 * @param pattern the pattern {@link ChannelAddress}
	 * @return an integer value representing the degree of matching
	 */
	public static int match(ChannelAddress source, ChannelAddress pattern) {
		int componentIdMatch = StringUtils.matchWildcard(source.componentId, pattern.componentId);
		int channelIdMatch = StringUtils.matchWildcard(source.channelId, pattern.channelId);
		if (componentIdMatch < 0 || channelIdMatch < 0) {
			return -1;
		} else if (componentIdMatch == 0 && channelIdMatch == 0) {
			return 0;
		}
		if (componentIdMatch == 0) {
			return Integer.MAX_VALUE / 2 + channelIdMatch;
		} else if (channelIdMatch == 0) {
			return Integer.MAX_VALUE / 2 + componentIdMatch;
		} else {
			return componentIdMatch + channelIdMatch;
		}
	}
}
