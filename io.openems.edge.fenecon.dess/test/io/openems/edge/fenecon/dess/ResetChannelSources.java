package io.openems.edge.fenecon.dess;

import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.SymmetricMeter;

/**
 * Reset Channel sources to avoid 'Unable to add Modbus mapping' errors on
 * running all tests at once.
 */
public class ResetChannelSources {

	public static void run() {
		resetChannelSources(SymmetricMeter.ChannelId.values());
		resetChannelSources(AsymmetricMeter.ChannelId.values());
	}

	private static void resetChannelSources(ChannelId[] channelIds) {
		for (ChannelId channelId : channelIds) {
			channelId.doc().source(null);
		}
	}

}
