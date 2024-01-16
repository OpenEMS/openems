package io.openems.edge.battery.fenecon.f2b;

import java.util.List;
import java.util.function.BiConsumer;

import io.openems.edge.battery.fenecon.f2b.cluster.common.BatteryFeneconF2bCluster;
import io.openems.edge.common.channel.ChannelId;

public interface DeviceSpecificOnSetNextValueHandler<B extends BatteryFeneconF2b> {

	public static record OnSetNextValueCallback(//
			BiConsumer<List<BatteryFeneconF2b>, BatteryFeneconF2bCluster> callback,
			List<? extends ChannelId> channelIds) {
	}

	/**
	 * Gets the battery type.
	 * 
	 * @return a class which extends the {@link BatteryFeneconF2b}
	 */
	public Class<B> getBatteryType();

	/**
	 * Gets the on change call back methods.
	 * 
	 * @return {@link List} of {@link OnSetNextValueCallback}
	 */
	public List<OnSetNextValueCallback> getOnSetNextValueCallbacks();

}
