package io.openems.edge.goodwe.charger;

import io.openems.edge.common.channel.ChannelId;

public interface PvChannelId extends ChannelId {

	public ChannelId getV();

	public ChannelId getI();

}