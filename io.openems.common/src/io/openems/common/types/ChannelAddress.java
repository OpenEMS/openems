package io.openems.common.types;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.StringUtils;

public class ChannelAddress implements Comparable<ChannelAddress> {

	private final String componentId;
	private final String channelId;
	private final String toString;

	public ChannelAddress(String componentId, String channelId) {
		this(componentId, channelId, new StringBuilder(componentId).append("/").append(channelId).toString());
	}

	private ChannelAddress(String componentId, String channelId, String toString) {
		this.componentId = componentId;
		this.channelId = channelId;
		this.toString = toString;
	}

	/**
	 * Gets the Component-ID.
	 *
	 * @return the Component-ID
	 */
	public String getComponentId() {
		return this.componentId;
	}

	/**
	 * Gets the Channel-Id.
	 *
	 * @return the Channel-Id
	 */
	public String getChannelId() {
		return this.channelId;
	}

	@Override
	public String toString() {
		return this.toString;
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
			var addressArray = address.split("/");
			var componentId = addressArray[0];
			var channelId = addressArray[1];
			return new ChannelAddress(componentId, channelId, address);
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
		if ((obj == null) || (this.getClass() != obj.getClass())) {
			return false;
		}
		var other = (ChannelAddress) obj;
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
		var componentIdMatch = StringUtils.matchWildcard(source.componentId, pattern.componentId);
		var channelIdMatch = StringUtils.matchWildcard(source.channelId, pattern.channelId);
		if (componentIdMatch < 0 || channelIdMatch < 0) {
			return -1;
		}
		if (componentIdMatch == 0 && channelIdMatch == 0) {
			return 0;
		}
		if (componentIdMatch == 0) {
			return Integer.MAX_VALUE / 2 + channelIdMatch;
		}
		if (channelIdMatch == 0) {
			return Integer.MAX_VALUE / 2 + componentIdMatch;
		} else {
			return componentIdMatch + channelIdMatch;
		}
	}
}
