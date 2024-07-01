package io.openems.edge.ess.mr.gridcon;

import io.openems.edge.battery.api.Battery;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.channel.internal.AbstractReadChannel;

public class Helper {

	/**
	 * Checks if all API values of a battery are set.
	 * 
	 * @param battery the {@link Battery}
	 * @return true if all API values are filled
	 */
	public static boolean isUndefined(Battery battery) {
		for (Channel<?> c : battery.channels()) {
			if (isBatteryApiChannel(c)) {
				if (c instanceof AbstractReadChannel<?, ?> && !(c instanceof WriteChannel<?>)) {
					if (!c.value().isDefined()) {
						System.out.println("Channel " + c + " is not defined!");
						return true;
					}
				}
			}
		}
		return false;
	}

	private static boolean isBatteryApiChannel(Channel<?> c) {
		for (io.openems.edge.common.channel.ChannelId id : Battery.ChannelId.values()) {
			if (id.equals(c.channelId())) {
				return true;
			}
		}
		return false;
	}
}
