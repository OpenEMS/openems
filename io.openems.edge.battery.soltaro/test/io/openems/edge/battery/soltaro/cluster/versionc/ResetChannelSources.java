package io.openems.edge.battery.soltaro.cluster.versionc;

import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.protection.BatteryProtection;
import io.openems.edge.battery.soltaro.cluster.SoltaroCluster;
import io.openems.edge.common.channel.ChannelId;

/**
 * Reset Channel sources to avoid 'Unable to add Modbus mapping' errors on
 * running all tests at once.
 */
public class ResetChannelSources {

	public static void run() {
		resetChannelSources(Battery.ChannelId.values());
		resetChannelSources(BatteryProtection.ChannelId.values());
		resetChannelSources(SoltaroCluster.ChannelId.values());
	}

	private static void resetChannelSources(ChannelId[] channelIds) {
		for (ChannelId channelId : channelIds) {
			channelId.doc().source(null);
		}
	}

}
