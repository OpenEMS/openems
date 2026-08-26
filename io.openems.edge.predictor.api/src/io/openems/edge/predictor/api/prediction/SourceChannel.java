package io.openems.edge.predictor.api.prediction;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.sum.Sum;

public enum SourceChannel {
	PRODUCTION_ACTIVE_POWER(new ChannelAddress("_sum", //
			Sum.ChannelId.PRODUCTION_ACTIVE_POWER.id())), //
	UNMANAGED_PRODUCTION_ACTIVE_POWER(new ChannelAddress("_sum", //
			Sum.ChannelId.UNMANAGED_PRODUCTION_ACTIVE_POWER.id()));

	public final ChannelAddress channelAddress;

	private SourceChannel(ChannelAddress channelAddress) {
		this.channelAddress = channelAddress;
	}
}
