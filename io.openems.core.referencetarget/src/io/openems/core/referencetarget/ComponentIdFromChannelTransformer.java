package io.openems.core.referencetarget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;

public class ComponentIdFromChannelTransformer implements ValueTransformer {

	@Override
	public Object transform(Object value) {
		if (value instanceof String channelAddress) {
			return toComponentId(channelAddress);
		}

		if (value instanceof String[] channelAddresses) {
			return Arrays.stream(channelAddresses) //
					.filter(address -> !"".equals(address)) //
					.map(ComponentIdFromChannelTransformer::toComponentId) //
					.distinct() //
					.toArray(String[]::new);
		}

		if (value instanceof Collection<?> channelAddresses) {
			final var addresses = new ArrayList<String>();
			for (var address : channelAddresses) {
				if (!(address instanceof String channelAddress)) {
					throw new IllegalArgumentException("Expected each channel address to be a String");
				}
				addresses.add(channelAddress);
			}
			return this.transform(addresses.toArray(String[]::new));
		}

		throw new IllegalArgumentException("Expected a channel address, an array or a collection of channel addresses");
	}

	private static String toComponentId(String channelAddress) {
		if ("".equals(channelAddress)) {
			return "";
		}

		try {
			return ChannelAddress.fromString(channelAddress).getComponentId();
		} catch (OpenemsNamedException e) {
			throw new IllegalArgumentException("Invalid channel address: " + channelAddress, e);
		}
	}
}